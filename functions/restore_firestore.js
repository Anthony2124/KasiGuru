/**
 * Restore Firestore from a local backup folder created by backup_firestore.js.
 *
 * Usage:
 *   node restore_firestore.js <service-account.json> <backup-dir>
 *
 * Writes every document back, addressed by its full path so nested documents
 * (users/{uid}/progress/{doc}) land where they came from. Documents with the same
 * path are overwritten; documents created since the backup are left alone, so this
 * is a roll-forward, not a point-in-time rollback. Use reset_firestore.js when the
 * database must be returned to exactly the state of a backup.
 *
 * Backups written before format 2 carry no paths; those restore by id at the root,
 * exactly as they did before.
 *
 * Test restore on a scratch project before relying on it in production.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { deserialize, writeAllDocsByPath } = require('./firestore_backup_util');

const keyFile = process.argv[2];
const backupDir = process.argv[3];

if (!keyFile || !backupDir || !fs.existsSync(backupDir)) {
  console.error('Usage: node restore_firestore.js <service-account.json> <backup-dir>');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyFile))
});

(async () => {
  const db = admin.firestore();
  const files = fs
    .readdirSync(backupDir)
    .filter((f) => f.endsWith('.json') && f !== 'manifest.json');

  if (files.length === 0) {
    console.error('No collection files found in', backupDir);
    process.exit(1);
  }

  for (const file of files) {
    const parsed = JSON.parse(fs.readFileSync(path.join(backupDir, file), 'utf8'));
    const entries = parsed.documents.map((d) => ({
      id: d.id,
      path: d.path,
      missing: d.missing,
      data: d.data === null ? null : deserialize(db, d.data)
    }));
    const n = await writeAllDocsByPath(db, parsed.collection, entries);
    const skipped = entries.length - n;
    console.log(
      `Restored ${parsed.collection}: ${n} documents` +
        (skipped ? ` (${skipped} nested parents skipped - they hold subcollections, not fields)` : '')
    );
  }

  console.log('Restore complete.');
})().catch((err) => {
  console.error('Restore failed:', err.message);
  process.exit(1);
});
