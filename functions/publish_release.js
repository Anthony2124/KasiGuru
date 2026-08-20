/**
 * Writes an `app_releases/vX.Y.Z` Firestore doc — the one manual step left after
 * `release.yml` builds and deploys everything else. Reads the service-account key
 * from the GCP_SA_KEY environment variable (the same secret release.yml's Firestore
 * rules deploy step already uses), not a file path, since CI has no reason to ever
 * write it to disk.
 *
 * Usage: GCP_SA_KEY='<service-account-json>' node publish_release.js <versionCode> <versionName> <apkUrl>
 */

const admin = require('firebase-admin');

const [, , versionCodeArg, versionName, apkUrl] = process.argv;
const versionCode = Number(versionCodeArg);

if (!versionCodeArg || !Number.isInteger(versionCode) || !versionName || !apkUrl) {
  console.error('Usage: node publish_release.js <versionCode> <versionName> <apkUrl>');
  process.exit(1);
}

const keyJson = process.env.GCP_SA_KEY;
if (!keyJson) {
  console.error('GCP_SA_KEY environment variable is not set.');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(keyJson))
});

(async () => {
  const doc = { versionCode, versionName, apkUrl };
  await admin.firestore().collection('app_releases').doc(`v${versionName}`).set(doc);
  console.log(`Published app_releases/v${versionName}:`, JSON.stringify(doc));
})().catch((err) => {
  console.error('Publish failed:', err.message);
  process.exit(1);
});
