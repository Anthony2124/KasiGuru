/**
 * Writes an `app_releases/vX.Y.Z` Firestore doc — the one step of a release that is not
 * a build artifact. Reads the service-account key from the GCP_SA_KEY environment
 * variable (the same secret release.yml's Firestore rules deploy step already uses),
 * not a file path, since CI has no reason to ever write it to disk.
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
  const ref = admin.firestore().collection('app_releases').doc(`v${versionName}`);

  // The admin dashboard's publish form writes the same six fields to this same doc id,
  // so whichever runs second must not undo the other's work. {merge: true} alone does
  // not achieve that: merge only protects fields absent from the payload, and an
  // unconditional `releaseNotes: ''` is present — so re-running this job after an admin
  // typed release notes would blank them, and re-stamp releasedAt to the re-run's date.
  //
  // The build facts (versionCode, versionName, apkUrl) are always written: CI is their
  // source of truth and a re-run should correct them. The editorial fields are seeded
  // only when the document does not already carry them.
  const snap = await ref.get();
  const existing = snap.exists ? snap.data() : {};

  const doc = {
    versionCode,
    versionName,
    apkUrl,
  };
  if (typeof existing.releaseNotes !== 'string') doc.releaseNotes = '';
  if (typeof existing.forceUpdate !== 'boolean') doc.forceUpdate = false;
  if (typeof existing.releasedAt !== 'number') doc.releasedAt = Date.now();

  await ref.set(doc, { merge: true });
  console.log(
    `Published app_releases/v${versionName}${snap.exists ? ' (merged into existing doc)' : ''}:`,
    JSON.stringify(doc)
  );
})().catch((err) => {
  console.error('Publish failed:', err.message);
  process.exit(1);
});
