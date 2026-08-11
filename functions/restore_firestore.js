/**
 * Restore Firestore from a local backup folder created by backup_firestore.js.
 *
 * Usage:
 *   node restore_firestore.js <service-account.json> <backup-dir>
 *
 * Writes every document back. Documents with the same ID are overwritten.
 * Test restore on a scratch project before relying on it in production.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { deserialize, writeAllDocs } = require('./firestore_backup_util');

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
      data: deserialize(db, d.data)
    }));
    const n = await writeAllDocs(db, parsed.collection, entries);
    console.log(`Restored ${parsed.collection}: ${n} documents`);
  }

  console.log('Restore complete.');
})().catch((err) => {
  console.error('Restore failed:', err.message);
  process.exit(1);
});
