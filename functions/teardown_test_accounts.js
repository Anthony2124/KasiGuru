/**
 * Deletes disposable test accounts created during debugging sessions.
 *
 * There is no separate dev/staging Firebase project for this app - kasiguru-86042
 * is the one project, and it is production (the same project the shipped app, the
 * admin dashboard, and the download site all point at). So this script cannot
 * detect "a safe non-prod environment" the way a typical teardown script would;
 * instead it matches strictly by naming convention (an email ending in
 * @kasiguru-test.local) and refuses to run against any other project id as a
 * last-resort guard against pointing it at the wrong service-account key.
 *
 * Deletes exactly what AuthRepository.deleteAccount() defines as "a whole
 * account" in the Android app, via the Admin SDK instead of the client SDK so it
 * can act on other users' accounts:
 *   - users/{uid}/progress/{main,achievements,gameLevels,wordStates}
 *   - leaderboard_public/{uid}
 *   - device_tokens/{uid}
 *   - the Firebase Auth user itself
 *
 * Usage (from the functions/ directory):
 *   node teardown_test_accounts.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only lists what it would delete.
 */

const admin = require('firebase-admin');
const path = require('path');

const EXPECTED_PROJECT_ID = 'kasiguru-86042';
const TEST_EMAIL_SUFFIX = '@kasiguru-test.local';
const PROGRESS_DOC_IDS = ['main', 'achievements', 'gameLevels', 'wordStates'];

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node teardown_test_accounts.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

const resolvedKeyPath = path.resolve(keyPath);
const serviceAccount = require(resolvedKeyPath);

if (serviceAccount.project_id !== EXPECTED_PROJECT_ID) {
  console.error(
    `Refusing to run: service account is for project "${serviceAccount.project_id}", ` +
      `expected "${EXPECTED_PROJECT_ID}". This script only ever targets the one known project.`
  );
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const auth = admin.auth();

/** Every Auth user whose email matches the test convention, across all pages. */
async function listTestUsers() {
  const matches = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    for (const user of page.users) {
      if ((user.email || '').toLowerCase().endsWith(TEST_EMAIL_SUFFIX)) {
        matches.push(user);
      }
    }
    pageToken = page.pageToken;
  } while (pageToken);
  return matches;
}

async function deleteAccountData(uid) {
  const progress = db.collection('users').doc(uid).collection('progress');
  const batch = db.batch();
  for (const docId of PROGRESS_DOC_IDS) {
    batch.delete(progress.doc(docId));
  }
  batch.delete(db.collection('leaderboard_public').doc(uid));
  batch.delete(db.collection('device_tokens').doc(uid));
  await batch.commit();
  await auth.deleteUser(uid);
}

async function main() {
  const users = await listTestUsers();

  console.log(`Found ${users.length} account(s) matching *${TEST_EMAIL_SUFFIX}.`);
  if (!apply) console.log('DRY RUN — pass --apply to delete.\n');

  for (const user of users) {
    console.log(`  ${user.uid}  ${user.email}`);
    if (apply) {
      await deleteAccountData(user.uid);
    }
  }

  console.log(`\n${apply ? 'Deleted' : 'Would delete'} ${users.length} test account(s).`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Teardown failed:', err.message);
    process.exit(1);
  });
