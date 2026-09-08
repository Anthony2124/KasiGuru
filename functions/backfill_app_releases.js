/**
 * One-off backfill for `app_releases`: repairs docs written before CI and the admin
 * dashboard's publish form agreed on one schema, and migrates every apkUrl off the
 * download site onto its GitHub Release asset.
 *
 * Two things this fixes:
 *  1. CI-only docs (written by the old 3-field publish_release.js) are missing
 *     releaseNotes/forceUpdate/releasedAt, which made the app and download page
 *     render "Invalid Date" / "No release notes provided" for them.
 *  2. Every apkUrl still points at https://kasiguru-download.vercel.app. That host is
 *     redeployed from a checkout where *.apk is gitignored, so a deployment carries
 *     only the APK CI just built and every older versioned URL 404s the moment the
 *     next release ships. GitHub keeps release assets permanently, so each doc is
 *     repointed at its own version's asset —
 *     .../releases/download/v<name>/kasiguru-v<name>.apk — restoring exact-version
 *     downloads rather than collapsing them all onto a moving "latest" alias.
 *
 * An apkUrl is only rewritten once the target asset is confirmed reachable. A version
 * whose APK was never uploaded to its GitHub Release is reported and left pointing
 * where it already pointed: a URL that 404s today is no worse than a URL that 404s
 * after this script runs, and a loud list is what tells you which binaries still need
 * uploading (scripts/archive-apks-to-releases.sh does that).
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
const REPO = process.env.KASIGURU_REPO || 'Anthony2124/KasiGuru';

function assetUrl(versionName) {
  return `https://github.com/${REPO}/releases/download/v${versionName}/kasiguru-v${versionName}.apk`;
}

// GitHub redirects release asset downloads to objects.githubusercontent.com, so follow
// redirects. A HEAD is enough — this must not pull ~8 MB per release just to check.
async function isReachable(url) {
  try {
    const res = await fetch(url, { method: 'HEAD', redirect: 'follow' });
    return res.status === 200;
  } catch (err) {
    console.warn(`  ! could not reach ${url}: ${err.message}`);
    return false;
  }
}

async function main() {
  const snapshot = await db.collection('app_releases').get();
  console.log(`Found ${snapshot.docs.length} app_releases doc(s). Repo: ${REPO}`);
  if (!apply) console.log('DRY RUN — pass --apply to write changes.\n');

  let changed = 0;
  let unchanged = 0;
  const missingAssets = [];

  for (const docSnap of snapshot.docs) {
    const data = docSnap.data();
    const patch = {};

    if (typeof data.releaseNotes !== 'string') patch.releaseNotes = '';
    if (typeof data.forceUpdate !== 'boolean') patch.forceUpdate = false;
    // Not the real historical release date (that was never recorded for these
    // docs) — just a stable value so "Invalid Date" stops appearing. Backfill
    // time, not release time.
    if (typeof data.releasedAt !== 'number') patch.releasedAt = Date.now();

    // The doc id is the authority for which version this is: it is `v<versionName>` by
    // construction in both writers, whereas a versionName *field* was missing entirely
    // from the docs the old CI wrote.
    const versionName = typeof data.versionName === 'string' && data.versionName
      ? data.versionName
      : docSnap.id.replace(/^v/, '');
    if (typeof data.versionName !== 'string' || !data.versionName) patch.versionName = versionName;

    const target = assetUrl(versionName);
    if (data.apkUrl !== target) {
      if (await isReachable(target)) {
        patch.apkUrl = target;
      } else {
        missingAssets.push({ id: docSnap.id, versionName, currentUrl: data.apkUrl || '(none)' });
      }
    }

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

  if (missingAssets.length) {
    console.log(`\n${missingAssets.length} release(s) have no APK on their GitHub Release yet —`);
    console.log('apkUrl left untouched for these. Upload the binaries, then re-run:');
    for (const m of missingAssets) {
      console.log(`  ${m.id.padEnd(10)} still points at ${m.currentUrl}`);
    }
  }
}

main().catch((err) => {
  console.error('Backfill failed:', err.message);
  process.exit(1);
});
