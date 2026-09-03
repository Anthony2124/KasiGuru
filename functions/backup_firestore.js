/**
 * Local JSON backup of Firestore — works on the FREE (Spark) plan.
 *
 * Uses the Admin SDK (service-account key) to read every collection and write
 * a timestamped folder of JSON files. No billing, no Cloud Functions, no GCS
 * export needed. (Firestore's built-in GCS export requires billing; this does not —
 * uploading the resulting JSON files to Storage below is a separate, optional step
 * that a Spark-plan default bucket already supports.)
 *
 * Usage:
 *   node backup_firestore.js <service-account.json> [output-dir]
 *
 *   output-dir defaults to C:\KasiGuru\KasiGuruBackups (outside the repo).
 *   Each run creates <output-dir>/<timestamp>/<collection>.json + manifest.json.
 *
 * Restore with: node restore_firestore.js <service-account.json> <backup-dir>
 *
 * The service-account JSON and the backup folders contain sensitive data —
 * keep them out of the repo.
 *
 * Off-site copy: this previously only wrote to the local disk of whichever
 * machine ran it — if that machine is off, wiped, or stops being logged into,
 * backups silently stop with nothing to notice. Set KASIGURU_BACKUP_BUCKET to
 * a Firebase Storage bucket name (e.g. kasiguru-86042.appspot.com, the default
 * bucket every Firebase project already has) to also upload each run there.
 * Left unset, behavior is unchanged — local-only, same as before.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { serialize, readAllDocsDeep } = require('./firestore_backup_util');

const keyFile = process.argv[2];
const outDir =
  process.argv[3] || path.join(__dirname, '..', '..', 'KasiGuruBackups');

if (!keyFile || !fs.existsSync(keyFile)) {
  console.error('Usage: node backup_firestore.js <path-to-service-account.json> [output-dir]');
  process.exit(1);
}

// With KASIGURU_BACKUP_DAILY=1, skip if a backup already exists for today
// (used by the logon-triggered startup task so it is effectively daily).
if (process.env.KASIGURU_BACKUP_DAILY === '1') {
  const todayPrefix = new Date().toISOString().slice(0, 10);
  if (fs.existsSync(outDir)) {
    const existing = fs
      .readdirSync(outDir)
      .filter((n) => n.startsWith(todayPrefix));
    if (existing.length > 0) {
      console.log(`Backup already exists for ${todayPrefix} — skipping.`);
      process.exit(0);
    }
  }
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyFile))
});

const projectId = require(keyFile).project_id;

(async () => {
  const db = admin.firestore();
  const collections = await db.listCollections();
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const runDir = path.join(outDir, stamp);
  fs.mkdirSync(runDir, { recursive: true });

  const manifest = {
    projectId,
    createdAt: new Date().toISOString(),
    format: 2,
    collections: {}
  };

  for (const col of collections) {
    // Deep, not flat. listCollections() returns root collections only, and a document that owns a
    // subcollection but has no fields of its own does not come back from a query at all - which is
    // why `users` reported 0 while holding every learner's synced progress. See readAllDocsDeep.
    const docs = await readAllDocsDeep(col);
    const payload = {
      collection: col.id,
      count: docs.length,
      documents: docs.map((d) => ({
        id: d.id,
        path: d.path,
        missing: d.missing || undefined,
        data: d.data === null ? null : serialize(d.data)
      }))
    };
    fs.writeFileSync(
      path.join(runDir, col.id + '.json'),
      JSON.stringify(payload, null, 2)
    );
    // Report real documents separately from missing parents, so a manifest can never again read as
    // healthy while the thing it was meant to protect is absent.
    const real = docs.filter((d) => !d.missing).length;
    manifest.collections[col.id] = real;
    if (real !== docs.length) {
      manifest.nestedParents = manifest.nestedParents || {};
      manifest.nestedParents[col.id] = docs.length - real;
    }
  }

  fs.writeFileSync(
    path.join(runDir, 'manifest.json'),
    JSON.stringify(manifest, null, 2)
  );

  console.log('Backup complete ->', runDir);
  console.log('Collections:', JSON.stringify(manifest.collections));
  console.log('WARNING: keep this folder private (contains user-submitted data).');

  const bucketName = process.env.KASIGURU_BACKUP_BUCKET;
  if (bucketName) {
    const bucket = admin.storage().bucket(bucketName);
    const destPrefix = `firestore-backups/${stamp}/`;
    const files = fs.readdirSync(runDir);
    for (const file of files) {
      await bucket.upload(path.join(runDir, file), {
        destination: destPrefix + file
      });
    }
    console.log(`Off-site copy uploaded -> gs://${bucketName}/${destPrefix}`);
  } else {
    console.log('KASIGURU_BACKUP_BUCKET not set — backup stayed local-only.');
  }
})().catch((err) => {
  console.error('Backup failed:', err.message);
  process.exit(1);
});
