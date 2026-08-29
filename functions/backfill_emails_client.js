/**
 * One-off backfill: patches email addresses from the Firebase Auth export
 * (auth_users.json) into leaderboard_public/{uid} documents in Firestore.
 *
 * This script uses the Firebase Client SDK (not Admin SDK) so it does NOT
 * require a service account key. It signs in as the admin user and writes
 * to Firestore using the client rules (which grant admin write access).
 *
 * Usage:
 *   node backfill_emails_client.js [--apply]
 *
 * Without --apply it runs as a dry run and only reports what it would patch.
 *
 * Prerequisites:
 *   - auth_users.json in the project root (from `firebase auth:export`)
 *   - npm install firebase (in this directory)
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');

const apply = process.argv.includes('--apply');

// Firebase client SDK (v9+ modular)
const { initializeApp } = require('firebase/app');
const { getAuth, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, getDoc, setDoc } = require('firebase/firestore');

// Firebase config for KasiGuru
const firebaseConfig = {
  apiKey: "AIzaSyC9YT2VfAXxPJ6HcWKz4pJqKCqKKvHtZk4",
  authDomain: "kasiguru-86042.firebaseapp.com",
  projectId: "kasiguru-86042",
  storageBucket: "kasiguru-86042.appspot.com",
  messagingSenderId: "776289754762",
  appId: "1:776289754762:web:placeholder"
};

async function askQuestion(question) {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stderr
  });
  return new Promise((resolve) => {
    rl.question(question, (answer) => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

async function main() {
  // 1) Read the auth export
  const authFile = path.resolve(__dirname, '..', 'auth_users.json');
  if (!fs.existsSync(authFile)) {
    console.error('ERROR: auth_users.json not found in project root.');
    console.error('Run:  firebase auth:export auth_users.json --format=json --project kasiguru-86042');
    process.exit(1);
  }

  const authData = JSON.parse(fs.readFileSync(authFile, 'utf8'));
  const authUsers = authData.users || [];
  console.log(`Loaded ${authUsers.length} user(s) from auth export.`);

  // 2) Build uid -> email mapping
  const emailMap = new Map();
  for (const u of authUsers) {
    let email = u.email || null;
    let displayName = u.displayName || null;

    // Check providerUserInfo for Google email
    if (!email && u.providerUserInfo) {
      for (const p of u.providerUserInfo) {
        if (p.email) { email = p.email; break; }
      }
    }
    if (!displayName && u.providerUserInfo) {
      for (const p of u.providerUserInfo) {
        if (p.displayName) { displayName = p.displayName; break; }
      }
    }

    const createdAt = u.createdAt ? parseInt(u.createdAt, 10) : null;
    const isAnonymous = !email && (!u.providerUserInfo || u.providerUserInfo.length === 0);

    if (email) {
      emailMap.set(u.localId, { email, displayName, createdAt, isAnonymous });
    }
  }

  console.log(`${emailMap.size} user(s) have an email address.`);
  if (!apply) console.log('DRY RUN — pass --apply to write changes.\n');

  if (!apply) {
    // Just show the mapping
    for (const [uid, data] of emailMap) {
      console.log(`  ${uid}: ${data.email} (${data.displayName || 'no name'})`);
    }
    console.log(`\nWould patch up to ${emailMap.size} leaderboard entries.`);
    console.log('Run with --apply to write changes.');
    process.exit(0);
  }

  // 3) Sign in as admin
  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  const db = getFirestore(app);

  const adminEmail = await askQuestion('Admin email: ');
  const adminPassword = await askQuestion('Admin password: ');

  try {
    await signInWithEmailAndPassword(auth, adminEmail, adminPassword);
    console.log(`Signed in as ${adminEmail}`);
  } catch (e) {
    console.error(`Failed to sign in: ${e.message}`);
    process.exit(1);
  }

  // 4) Patch leaderboard_public documents
  let patched = 0;
  let skipped = 0;
  for (const [uid, data] of emailMap) {
    try {
      const lbRef = doc(db, 'leaderboard_public', uid);
      const lbSnap = await getDoc(lbRef);

      if (!lbSnap.exists()) {
        skipped++;
        continue;
      }

      const lbData = lbSnap.data() || {};
      const needsEmail = !lbData.email || !lbData.email.includes('@');
      const needsDate = !lbData.registeredAt && !lbData.createdAt;

      if (!needsEmail && !needsDate) {
        skipped++;
        continue;
      }

      const patch = {};
      if (needsEmail) patch.email = data.email;
      if (needsDate && data.createdAt) {
        patch.registeredAt = data.createdAt;
        patch.createdAt = data.createdAt;
      }
      patch.isAnonymous = false;

      // Fix displayName if it's generic
      const dn = (lbData.displayName || '').trim().toLowerCase();
      if (dn === 'learner' || dn === 'registered user' || dn === 'google account' || !dn) {
        if (data.displayName) patch.displayName = data.displayName.slice(0, 40);
      }

      console.log(`  PATCH ${uid}: ${JSON.stringify(patch)}`);
      await setDoc(lbRef, patch, { merge: true });
      patched++;

      // Also patch progress doc
      const progRef = doc(db, 'users', uid, 'progress', 'main');
      const progSnap = await getDoc(progRef);
      if (progSnap.exists()) {
        const pData = progSnap.data() || {};
        const progPatch = {};
        if (!pData.email || !pData.email.includes('@')) progPatch.email = data.email;
        if (!pData.fullName && data.displayName) progPatch.fullName = data.displayName;
        if (Object.keys(progPatch).length > 0) {
          console.log(`  PATCH progress ${uid}: ${JSON.stringify(progPatch)}`);
          await setDoc(progRef, progPatch, { merge: true });
        }
      }
    } catch (e) {
      console.error(`  ERROR ${uid}: ${e.message}`);
    }
  }

  console.log(`\nPatched ${patched} leaderboard entries, skipped ${skipped}.`);
  process.exit(0);
}

main().catch((err) => {
  console.error('Backfill failed:', err.message);
  process.exit(1);
});
