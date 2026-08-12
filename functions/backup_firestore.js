/**
 * Local JSON backup of Firestore — works on the FREE (Spark) plan.
 *
 * Uses the Admin SDK (service-account key) to read every collection and write
 * a timestamped folder of JSON files. No billing, no Cloud Functions, no GCS.
 * (Firestore's built-in GCS export requires billing; this does not.)
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
 * keep them out of the repo and consider syncing the backup folder to
 * Google Drive / OneDrive / another machine for off-site redundancy.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { serialize, readAllDocs } = require('./firestore_backup_util');

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
    collections: {}
  };

  for (const col of collections) {
    const docs = await readAllDocs(col);
    const payload = {
      collection: col.id,
      count: docs.length,
      documents: docs.map((d) => ({ id: d.id, data: serialize(d.data) }))
    };
    fs.writeFileSync(
      path.join(runDir, col.id + '.json'),
      JSON.stringify(payload, null, 2)
    );
    manifest.collections[col.id] = docs.length;
  }

  fs.writeFileSync(
    path.join(runDir, 'manifest.json'),
    JSON.stringify(manifest, null, 2)
  );

  console.log('Backup complete ->', runDir);
  console.log('Collections:', JSON.stringify(manifest.collections));
  console.log('WARNING: keep this folder private (contains user-submitted data).');
})().catch((err) => {
  console.error('Backup failed:', err.message);
  process.exit(1);
});
