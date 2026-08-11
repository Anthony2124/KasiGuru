/**
 * One-off script to grant the `admin` custom claim to a Firebase Auth user.
 * Works on any Firebase plan (no Blaze upgrade needed) using a service-account
 * key from the Firebase console.
 *
 * Usage (from the functions/ directory):
 *   node set_admin_claim.js <email> <path-to-service-account.json>
 *
 * The service-account JSON must NOT be committed — keep it outside the repo.
 *
 * Production alternative: deploy the bootstrapAdmin Cloud Function (see
 * index.js) which does the same thing behind a secret on the Blaze plan.
 */

const admin = require('firebase-admin');
const path = require('path');

const [email, keyPath] = process.argv.slice(2);

if (!email || !keyPath) {
  console.error('Usage: node set_admin_claim.js <email> <path-to-service-account.json>');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyPath))
});

admin
  .auth()
  .getUserByEmail(email.trim().toLowerCase())
  .then((user) => admin.auth().setCustomUserClaims(user.uid, { admin: true }))
  .then(() => {
    console.log(`Admin claim set for ${email.trim().toLowerCase()}.`);
    console.log('Sign out and back into the admin portal so the token refreshes.');
    process.exit(0);
  })
  .catch((err) => {
    console.error('Failed:', err.message);
    process.exit(1);
  });
