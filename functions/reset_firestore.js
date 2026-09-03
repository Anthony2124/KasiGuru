/**
 * Resets the KasiGuru Firestore database to a known state. Works on the FREE (Spark) plan.
 *
 * This runs through the Admin SDK with a service-account key, and it has to. `firestore.rules`
 * deliberately makes learner data owner-writable only: an admin may read `users/{uid}` for the
 * dashboard but may not write it, `security_questions` and `device_tokens` are invisible to
 * admins entirely, and `admin_audit_log` is append-only with `allow delete: if false`. A reset
 * button in the browser therefore cannot clear learner data, and weakening the rules so it could
 * would hand every admin session the ability to rewrite any learner's progress. Holding the
 * service-account key is the stronger credential, and it stays off the web.
 *
 * Usage:
 *   node reset_firestore.js <service-account.json> --mode=learner
 *   node reset_firestore.js <service-account.json> --mode=factory --from=<backup-dir>
 *
 * Modes:
 *   learner   Clears learner-generated data: synced progress, submissions, issue reports,
 *             leaderboard rows, device tokens, security questions. The curated corpus - the
 *             dictionary, stories and releases - is untouched.
 *   factory   Everything `learner` clears, plus the content collections are deleted and rewritten
 *             from the backup given by --from, so the database ends at exactly that snapshot.
 *
 * Safety:
 *   Dry run by default. Nothing is deleted until you pass --confirm=<projectId>, and the project
 *   id has to match the one in the service-account key, so a key for the wrong project fails loudly
 *   instead of wiping it. `factory` additionally refuses to run without a readable backup, because
 *   a factory reset with nothing to restore from is just a delete.
 *
 * Always take a backup first:
 *   node backup_firestore.js <service-account.json>
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const {
  deserialize,
  writeAllDocsByPath,
  deleteCollectionDeep,
  countCollectionDeep
} = require('./firestore_backup_util');

/**
 * Learner-generated data. Everything here is produced by using the app and can be recreated by
 * using it again; none of it is authored content.
 */
const LEARNER_COLLECTIONS = [
  'users',                    // walked deep: users/{uid}/progress/{doc}
  'leaderboard_public',
  'device_tokens',
  'security_questions',
  'word_submissions',
  'literature_submissions',
  'issue_reports'
];

/**
 * Curated content. Authored by the team and moderators, expensive to recreate, and the reason a
 * factory reset must restore rather than merely delete.
 */
const CONTENT_COLLECTIONS = [
  'vocabulary',
  'stories',
  'story_page_images',
  'announcements',
  'app_releases'
];

/**
 * Deliberately in neither list. The audit log is append-only by rule (`allow delete: if false`)
 * and is the record of who did what - including who ran this script. Clearing it as part of a
 * reset would erase the evidence of the reset.
 */
const PRESERVED_COLLECTIONS = ['admin_audit_log'];

function parseArgs(argv) {
  const out = { _: [] };
  for (const arg of argv) {
    const m = /^--([^=]+)(?:=(.*))?$/.exec(arg);
    if (m) out[m[1]] = m[2] === undefined ? true : m[2];
    else out._.push(arg);
  }
  return out;
}

const args = parseArgs(process.argv.slice(2));
const keyFile = args._[0];
const mode = args.mode;

function usage(message) {
  if (message) console.error('Error:', message + '\n');
  console.error('Usage:');
  console.error('  node reset_firestore.js <service-account.json> --mode=learner [--confirm=<projectId>]');
  console.error('  node reset_firestore.js <service-account.json> --mode=factory --from=<backup-dir> [--confirm=<projectId>]');
  console.error('\nRuns as a dry run until --confirm matches the project id in the key.');
  process.exit(1);
}

if (!keyFile || !fs.existsSync(keyFile)) usage('service-account key not found');
if (mode !== 'learner' && mode !== 'factory') usage('--mode must be "learner" or "factory"');
if (mode === 'factory' && !args.from) usage('--mode=factory requires --from=<backup-dir>');
if (mode === 'factory' && !fs.existsSync(args.from)) usage(`backup directory not found: ${args.from}`);

const projectId = require(path.resolve(keyFile)).project_id;
const armed = args.confirm === projectId;

admin.initializeApp({ credential: admin.credential.cert(path.resolve(keyFile)) });

/** Reads the content half of a backup folder, so factory mode knows what it is restoring. */
function readContentFromBackup(db, backupDir) {
  const restorable = [];
  for (const name of CONTENT_COLLECTIONS) {
    const file = path.join(backupDir, name + '.json');
    if (!fs.existsSync(file)) continue;
    const parsed = JSON.parse(fs.readFileSync(file, 'utf8'));
    const entries = (parsed.documents || []).map((d) => ({
      id: d.id,
      path: d.path,
      missing: d.missing,
      data: d.data === null ? null : deserialize(db, d.data)
    }));
    restorable.push({ collection: name, entries });
  }
  return restorable;
}

(async () => {
  const db = admin.firestore();

  const targets = mode === 'factory'
    ? [...LEARNER_COLLECTIONS, ...CONTENT_COLLECTIONS]
    : [...LEARNER_COLLECTIONS];

  console.log(`Project:   ${projectId}`);
  console.log(`Mode:      ${mode}`);
  console.log(`Preserved: ${PRESERVED_COLLECTIONS.join(', ')}`);
  if (mode === 'factory') console.log(`Restoring content from: ${args.from}`);
  console.log('');

  let restorable = [];
  if (mode === 'factory') {
    restorable = readContentFromBackup(db, args.from);
    if (restorable.length === 0) {
      console.error('Refusing to continue: no content collections found in', args.from);
      console.error('A factory reset with nothing to restore from would only delete.');
      process.exit(1);
    }
  }

  // Count first, always - the dry run and the armed run report the same numbers, so what you see
  // in the rehearsal is what the real run does.
  console.log('Documents to delete:');
  let total = 0;
  for (const name of targets) {
    const n = await countCollectionDeep(db, db.collection(name));
    total += n;
    console.log(`  ${name.padEnd(24)} ${String(n).padStart(6)}`);
  }
  console.log(`  ${'TOTAL'.padEnd(24)} ${String(total).padStart(6)}`);

  if (mode === 'factory') {
    console.log('\nDocuments to restore afterwards:');
    let restoreTotal = 0;
    for (const item of restorable) {
      const n = item.entries.filter((e) => !e.missing && e.data !== null).length;
      restoreTotal += n;
      console.log(`  ${item.collection.padEnd(24)} ${String(n).padStart(6)}`);
    }
    console.log(`  ${'TOTAL'.padEnd(24)} ${String(restoreTotal).padStart(6)}`);
  }

  if (!armed) {
    console.log('\nDRY RUN - nothing was changed.');
    console.log(`To execute, re-run with:  --confirm=${projectId}`);
    console.log('Take a backup first:      node backup_firestore.js <service-account.json>');
    return;
  }

  console.log('\nARMED. Deleting...');
  for (const name of targets) {
    const n = await deleteCollectionDeep(db, db.collection(name), (p, c) =>
      console.log(`  ...${p}: ${c}`)
    );
    console.log(`  deleted ${name}: ${n}`);
  }

  if (mode === 'factory') {
    console.log('\nRestoring content...');
    for (const item of restorable) {
      const n = await writeAllDocsByPath(db, item.collection, item.entries);
      console.log(`  restored ${item.collection}: ${n}`);
    }
  }

  // The reset is itself an administrative action, so it belongs in the log that survives it.
  await db.collection('admin_audit_log').add({
    action: mode === 'factory' ? 'database_factory_reset' : 'database_learner_reset',
    actor: 'reset_firestore.js (service account)',
    projectId,
    deletedDocuments: total,
    restoredFrom: mode === 'factory' ? path.basename(args.from) : null,
    at: admin.firestore.FieldValue.serverTimestamp()
  });

  console.log('\nReset complete. Verify with:  node backup_firestore.js <service-account.json>');
  console.log('and compare the new manifest against the expected counts.');
})().catch((err) => {
  console.error('Reset failed:', err.message);
  process.exit(1);
});
