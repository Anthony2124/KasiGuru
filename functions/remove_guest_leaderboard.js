/**
 * One-off cleanup: removes leaderboard rows that belong to guests — players who
 * never made an account.
 *
 * Only real accounts are ranked now: the app no longer publishes a row for an
 * anonymous session (ProgressSyncManager.publishLeaderboardEntry), and
 * firestore.rules refuses one. This deletes the rows published before that.
 *
 * Firebase Auth is the source of truth, not the row's own isAnonymous flag (rows
 * from older app versions may not carry it). A row is removed when its uid:
 *   - is an anonymous Auth user (no sign-in provider linked), or
 *   - no longer exists in Auth at all (an orphaned row nobody can sign in to).
 *
 * Only leaderboard_public/{uid} is deleted. The guest's progress under
 * users/{uid} is left alone, so a guest who later links an account keeps
 * everything and reappears on the board after their next sync.
 *
 * Usage (from the functions/ directory):
 *   node remove_guest_leaderboard.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only lists what it would delete.
 */

const admin = require('firebase-admin');
const path = require('path');

const EXPECTED_PROJECT_ID = 'kasiguru-86042';

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node remove_guest_leaderboard.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

const serviceAccount = require(path.resolve(keyPath));

if (serviceAccount.project_id !== EXPECTED_PROJECT_ID) {
  console.error(
    `Refusing to run: service account is for project "${serviceAccount.project_id}", ` +
      `expected "${EXPECTED_PROJECT_ID}".`
  );
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const auth = admin.auth();

/** uid -> Auth user record, for every uid Auth still knows. getUsers() takes 100 at a time. */
async function lookUpUsers(uids) {
  const found = new Map();
  for (let i = 0; i < uids.length; i += 100) {
    const result = await auth.getUsers(uids.slice(i, i + 100).map((uid) => ({ uid })));
    for (const user of result.users) found.set(user.uid, user);
  }
  return found;
}

async function main() {
  const snapshot = await db.collection('leaderboard_public').get();
  const users = await lookUpUsers(snapshot.docs.map((doc) => doc.id));

  console.log(`Found ${snapshot.size} leaderboard row(s).`);
  if (!apply) console.log('DRY RUN — pass --apply to delete.\n');

  const guests = [];
  let kept = 0;
  for (const doc of snapshot.docs) {
    const user = users.get(doc.id);
    // An anonymous Auth user has no linked provider (Google, email/password, ...).
    const reason = !user ? 'no Auth user' : user.providerData.length === 0 ? 'guest' : null;
    if (!reason) {
      kept += 1;
      continue;
    }
    const data = doc.data() || {};
    console.log(`  ${doc.id}  ${data.displayName || '(no name)'}  ${Number(data.totalXp) || 0} XP  [${reason}]`);
    guests.push(doc.ref);
  }

  if (apply) {
    for (let i = 0; i < guests.length; i += 450) {
      const batch = db.batch();
      guests.slice(i, i + 450).forEach((ref) => batch.delete(ref));
      await batch.commit();
    }
  }

  console.log(
    `\n${apply ? 'Deleted' : 'Would delete'} ${guests.length} guest row(s); kept ${kept} with an account.`
  );
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Cleanup failed:', err.message);
    process.exit(1);
  });
