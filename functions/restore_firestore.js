/**
 * Restore Firestore from a backup folder created by backup_firestore.js - written for the day the
 * database has been wiped or vandalised and has to come back quickly without breaking anything.
 *
 * Usage:
 *   node restore_firestore.js <service-account.json> <backup-dir> [--only=a,b] [--confirm=<projectId>]
 *   node restore_firestore.js emulator:demo-<name> <backup-dir> ...    (drill on the Firestore emulator)
 *
 * Dry run until --confirm matches the project id in the key. The dry run reads what is in
 * Firestore now and prints, per collection, exactly what the armed run will do.
 *
 * Per document, addressed by its full path so users/{uid}/progress/* lands where it came from:
 *   restore    missing from Firestore - written back.
 *   overwrite  present but different - replaced by the backup copy. This undoes vandalism, and also
 *              any legitimate edit made since the backup: use --only to restore just the
 *              collections that were hit.
 *   same       already identical - not written.
 *   kept       a learner's progress/main or leaderboard row already at or past the backup's XP, or a
 *              device token already present. Overwriting those with the older copy would lock the
 *              learner's phone out of syncing; see planRestore in firestore_backup_util.js.
 * Documents created since the backup are never touched: this is a roll-forward, not a rollback.
 * Use reset_firestore.js --mode=factory when the database must end at exactly a backup's state.
 *
 * Restored vocabulary and stories get a fresh updatedAt, as the dashboard's restore does, so phones
 * pull them on their next incremental sync instead of at the daily full reconcile.
 *
 * Backups written before format 2 carry no paths; those restore by id at the root.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const {
  initAdmin,
  serialize,
  deserialize,
  readAllDocsDeep,
  writeAllDocsByPath,
  planRestore
} = require('./firestore_backup_util');

// What the app syncs incrementally by updatedAt (FirestoreSyncManager).
const APP_SYNCED_CONTENT = new Set(['vocabulary', 'stories']);

function parseArgs(argv) {
  const out = { _: [] };
  for (const arg of argv) {
    const m = /^--([^=]+)(?:=(.*))?$/.exec(arg);
    if (m) out[m[1]] = m[2] === undefined ? true : m[2];
    else out._.push(arg);
  }
  return out;
}

function usage(message) {
  if (message) console.error('Error:', message + '\n');
  console.error('Usage:');
  console.error('  node restore_firestore.js <service-account.json> <backup-dir> [--only=a,b] [--confirm=<projectId>]');
  console.error('  node restore_firestore.js emulator:demo-<name> <backup-dir> [--only=a,b] [--confirm=demo-<name>]');
  console.error('\nRuns as a dry run until --confirm matches the project id.');
  process.exit(1);
}

const args = parseArgs(process.argv.slice(2));
const [keyArg, backupDir] = args._;

if (!keyArg || !backupDir) usage();
if (!fs.existsSync(backupDir)) usage(`backup directory not found: ${backupDir}`);
// backup_firestore.js writes the manifest last, so a folder without one is a run that died part-way.
if (!fs.existsSync(path.join(backupDir, 'manifest.json'))) {
  usage(`${backupDir} has no manifest.json - that backup never finished. Pick an older folder.`);
}

let projectId;
let target;
try {
  ({ projectId, target } = initAdmin(keyArg));
} catch (err) {
  usage(err.message);
}
const armed = args.confirm === projectId;
const only = typeof args.only === 'string'
  ? new Set(args.only.split(',').map((s) => s.trim()).filter(Boolean))
  : null;

const pad = (value, width) => String(value).padStart(width);

(async () => {
  const db = admin.firestore();
  const manifest = JSON.parse(fs.readFileSync(path.join(backupDir, 'manifest.json'), 'utf8'));

  const files = fs
    .readdirSync(backupDir)
    .filter((f) => f.endsWith('.json') && f !== 'manifest.json')
    .filter((f) => !only || only.has(path.basename(f, '.json')));
  if (files.length === 0) usage(only ? `none of --only=${args.only} is in ${backupDir}` : `no collection files in ${backupDir}`);
  for (const name of only || []) {
    if (!files.includes(name + '.json')) console.log(`NOTE: --only names ${name}, which this backup does not contain.`);
  }

  console.log(`Target:  ${target} (${projectId})`);
  console.log(`Backup:  ${path.basename(path.resolve(backupDir))}, taken ${manifest.createdAt || 'at an unknown time'} from ${manifest.projectId || 'an unknown project'}`);
  if (manifest.projectId && manifest.projectId !== projectId) {
    console.log(`         NOTE: this backup is from a different project than the target.`);
  }
  if (!manifest.format || manifest.format < 2) {
    console.log('         NOTE: format below 2 - this backup holds no learner progress.');
  }
  console.log(`Mode:    ${armed ? 'ARMED - writing' : 'DRY RUN - reading only'}\n`);

  console.log(`  ${'collection'.padEnd(24)}${pad('restore', 9)}${pad('overwrite', 11)}${pad('same', 7)}${pad('kept', 7)}`);
  const plans = [];
  for (const file of files) {
    const parsed = JSON.parse(fs.readFileSync(path.join(backupDir, file), 'utf8'));
    const live = await readAllDocsDeep(db.collection(parsed.collection));
    const current = new Map(live.filter((d) => !d.missing).map((d) => [d.path, serialize(d.data)]));
    const plan = planRestore(parsed.collection, parsed.documents, current,
      APP_SYNCED_CONTENT.has(parsed.collection) ? ['updatedAt'] : []);
    plans.push({ collection: parsed.collection, plan });
    console.log(`  ${parsed.collection.padEnd(24)}${pad(plan.restore, 9)}${pad(plan.overwrite, 11)}${pad(plan.same, 7)}${pad(plan.kept, 7)}`);
  }

  const sum = (key) => plans.reduce((n, p) => n + p.plan[key], 0);
  const writes = sum('restore') + sum('overwrite');
  console.log(`  ${'TOTAL'.padEnd(24)}${pad(sum('restore'), 9)}${pad(sum('overwrite'), 11)}${pad(sum('same'), 7)}${pad(sum('kept'), 7)}`);
  console.log(`\n${writes} document writes. The free plan allows 20,000 a day.`);
  if (sum('kept') > 0) {
    console.log(`${sum('kept')} kept: learners whose phones already synced past this backup, and current device tokens.`);
  }

  if (!armed) {
    console.log('\nDRY RUN - nothing was changed.');
    console.log(`To execute, re-run with:  --confirm=${projectId}`);
    return;
  }
  if (writes === 0) {
    console.log('\nNothing to write - Firestore already holds everything in this backup.');
    return;
  }

  console.log('\nRestoring...');
  const now = Date.now();
  for (const { collection, plan } of plans) {
    if (plan.writes.length === 0) continue;
    const entries = plan.writes.map((d) => {
      const data = deserialize(db, d.data);
      if (APP_SYNCED_CONTENT.has(collection)) data.updatedAt = now;
      return { id: d.id, path: d.path, data };
    });
    const n = await writeAllDocsByPath(db, collection, entries);
    console.log(`  restored ${collection}: ${n}`);
  }

  // A restore is an administrative action, so it belongs in the log - including a log it restored.
  await db.collection('admin_audit_log').add({
    action: 'database_restore',
    actor: 'restore_firestore.js (service account)',
    projectId,
    restoredFrom: path.basename(path.resolve(backupDir)),
    only: only ? [...only] : null,
    written: writes,
    kept: sum('kept'),
    at: admin.firestore.FieldValue.serverTimestamp()
  });

  console.log('\nRestore complete. Check it with a fresh backup and compare the two manifest.json files:');
  console.log('  node backup_firestore.js <service-account.json>');
})().catch((err) => {
  console.error('Restore failed:', err.message);
  console.error('It is safe to run the same command again: documents already restored are skipped as "same".');
  process.exit(1);
});
