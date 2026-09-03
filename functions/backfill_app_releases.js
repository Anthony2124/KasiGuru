/**
 * One-off backfill for `app_releases`: repairs docs written before CI and the admin
 * dashboard's publish form agreed on one schema.
 *
 * Two things this fixes:
 *  1. CI-only docs (written by the old 3-field publish_release.js) are missing
 *     releaseNotes/forceUpdate/releasedAt, which made the app and download page
 *     render "Invalid Date" / "No release notes provided" for them.
 *  2. Every doc's apkUrl still points at that release's own versioned filename
 *     (kasiguru-vX.Y.Z.apk). Every deploy of the download site carries only the
 *     newest APK, so all but the most recent of those URLs are already 404ing.
 *     Per the "only latest needs to stay downloadable" decision, this rewrites
 *     every apkUrl to the stable kasiguru-latest.apk alias instead of trying to
 *     resurrect old binaries — old exact-version downloads are not preserved.
 *
 * This is a one-time repair for existing data, not something CI needs going
 * forward: publish_release.js and the admin dashboard's publish form already
 * write the full six-field shape (and the alias URL) on every new release.
 *
 * Usage (from the functions/ directory):
 *   node backfill_app_releases.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only reports what it would change.
 */

const admin = require('firebase-admin');
const path = require('path');

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node backfill_app_releases.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyPath))
});

const db = admin.firestore();
const LATEST_APK_URL = 'https://kasiguru-download.vercel.app/kasiguru-latest.apk';

async function main() {
  const snapshot = await db.collection('app_releases').get();
  console.log(`Found ${snapshot.docs.length} app_releases doc(s).`);
  if (!apply) console.log('DRY RUN — pass --apply to write changes.\n');

  let changed = 0;
  let unchanged = 0;

  for (const docSnap of snapshot.docs) {
    const data = docSnap.data();
    const patch = {};

    if (typeof data.releaseNotes !== 'string') patch.releaseNotes = '';
    if (typeof data.forceUpdate !== 'boolean') patch.forceUpdate = false;
    // Not the real historical release date (that was never recorded for these
    // docs) — just a stable value so "Invalid Date" stops appearing. Backfill
    // time, not release time.
    if (typeof data.releasedAt !== 'number') patch.releasedAt = Date.now();
    if (data.apkUrl !== LATEST_APK_URL) patch.apkUrl = LATEST_APK_URL;

    if (Object.keys(patch).length === 0) {
      unchanged++;
      continue;
    }

    changed++;
    console.log(`${apply ? 'Updating' : 'Would update'} ${docSnap.id}:`, JSON.stringify(patch));
    if (apply) {
      await docSnap.ref.set(patch, { merge: true });
    }
  }

  console.log(`\n${changed} doc(s) ${apply ? 'updated' : 'would be updated'}, ${unchanged} already correct.`);
}

main().catch((err) => {
  console.error('Backfill failed:', err.message);
  process.exit(1);
});
