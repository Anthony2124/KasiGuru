/**
 * One-off backfill: patches the email address and displayName from Firebase Auth
 * into both leaderboard_public/{uid} and users/{uid}/progress/main for every user
 * who has an email in Firebase Auth but is missing it in Firestore.
 *
 * This fixes the admin dashboard showing "—" instead of the real email address,
 * because the leaderboard documents were originally created without the email field.
 *
 * Usage (from the functions/ directory):
 *   node backfill_emails.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only reports what it would patch.
 */

const admin = require('firebase-admin');
const path = require('path');

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node backfill_emails.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyPath))
});

const db = admin.firestore();
const auth = admin.auth();

async function main() {
  // List all Firebase Auth users (supports pagination for >1000 users)
  const authUsers = [];
  let nextPageToken;
  do {
    const listResult = await auth.listUsers(1000, nextPageToken);
    authUsers.push(...listResult.users);
    nextPageToken = listResult.pageToken;
  } while (nextPageToken);

  console.log(`Found ${authUsers.length} Firebase Auth user(s).`);

  // Build a map: uid -> { email, displayName, createdAt }
  const authMap = new Map();
  for (const u of authUsers) {
    const email = u.email || (u.providerData && u.providerData[0] && u.providerData[0].email) || null;
    const displayName = u.displayName || (u.providerData && u.providerData[0] && u.providerData[0].displayName) || null;
    const createdAt = u.metadata && u.metadata.creationTime
      ? new Date(u.metadata.creationTime).getTime()
      : null;
    const isAnonymous = !u.email && (!u.providerData || u.providerData.length === 0);

    if (email) {
      authMap.set(u.uid, { email, displayName, createdAt, isAnonymous });
    }
  }

  console.log(`${authMap.size} user(s) have an email address in Firebase Auth.`);
  if (!apply) console.log('DRY RUN — pass --apply to write changes.\n');

  let patchedLeaderboard = 0;
  let patchedProgress = 0;
  let batch = db.batch();
  let pending = 0;

  for (const [uid, authData] of authMap) {
    // Check leaderboard_public/{uid}
    const lbRef = db.collection('leaderboard_public').doc(uid);
    const lbDoc = await lbRef.get();

    if (lbDoc.exists) {
      const lbData = lbDoc.data() || {};
      const needsEmail = !lbData.email || !lbData.email.includes('@');
      const needsDate = !lbData.registeredAt && !lbData.createdAt;
      const needsAnon = lbData.isAnonymous === undefined;

      if (needsEmail || needsDate || needsAnon) {
        const patch = {};
        if (needsEmail) patch.email = authData.email;
        if (needsDate && authData.createdAt) {
          patch.registeredAt = authData.createdAt;
          patch.createdAt = authData.createdAt;
        }
        if (needsAnon) patch.isAnonymous = authData.isAnonymous;
        // Also update displayName if it's generic
        const dn = (lbData.displayName || '').trim().toLowerCase();
        if (dn === 'learner' || dn === 'registered user' || dn === 'google account' || !dn) {
          if (authData.displayName) patch.displayName = authData.displayName.slice(0, 40);
        }

        console.log(`  LEADERBOARD ${uid}: ${JSON.stringify(patch)}`);
        if (apply) {
          batch.set(lbRef, patch, { merge: true });
          pending++;
        }
        patchedLeaderboard++;
      }
    }

    // Check users/{uid}/progress/main
    const progRef = db.collection('users').doc(uid).collection('progress').doc('main');
    const progDoc = await progRef.get();

    if (progDoc.exists) {
      const pData = progDoc.data() || {};
      const needsEmail = !pData.email || !pData.email.includes('@');
      const needsName = !pData.fullName || pData.fullName.trim() === '';

      if (needsEmail || needsName) {
        const patch = {};
        if (needsEmail) patch.email = authData.email;
        if (needsName && authData.displayName) patch.fullName = authData.displayName;

        console.log(`  PROGRESS   ${uid}: ${JSON.stringify(patch)}`);
        if (apply) {
          batch.set(progRef, patch, { merge: true });
          pending++;
        }
        patchedProgress++;
      }
    }

    // Flush batch if approaching the 500 operation limit
    if (pending >= 450) {
      await batch.commit();
      batch = db.batch();
      pending = 0;
    }
  }

  if (apply && pending > 0) await batch.commit();

  console.log(
    `\n${apply ? 'Patched' : 'Would patch'} ${patchedLeaderboard} leaderboard row(s) and ${patchedProgress} progress doc(s).`
  );
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Backfill failed:', err.message);
    process.exit(1);
  });
