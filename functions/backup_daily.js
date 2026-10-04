/**
 * The scheduled daily backup: a full backup, a rotation that cannot eat the last good backups, and
 * a copy off this machine.
 *
 * Usage:
 *   node backup_daily.js <service-account.json> [--out=<dir>] [--mirror=<dir>] [--keep=14]
 *
 *   --out     Where backups are written. Defaults to backup_firestore.js's own default:
 *             KasiGuruBackups beside the repository folder.
 *   --mirror  A second folder that receives a copy of every finished backup - OneDrive, or a USB
 *             drive - so a backup outlives this machine. Copied, never mirrored: a backup deleted
 *             here is not deleted there, so whoever empties one folder has not emptied both.
 *   --keep    How many of the newest backups each folder keeps (default 14). The oldest backup of
 *             each month is kept too, and a backup much larger than the newest is never removed;
 *             see planRotation in firestore_backup_util.js.
 *
 * Registered with Task Scheduler by scripts/register_backup_task.ps1. Safe to run several times a
 * day: backup_firestore.js skips when today's backup already exists. Appends to backup_daily.log in
 * the --out folder.
 *
 * Exit code: 0 fine, 1 the backup or the copy failed, 2 the backup worked but looks like a wipe.
 */

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const { listBackups, planRotation, shrunkCollections } = require('./firestore_backup_util');

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
const keep = Number(args.keep || 14);
if (!keyFile || !Number.isInteger(keep) || keep < 1) {
  console.error('Usage: node backup_daily.js <service-account.json> [--out=<dir>] [--mirror=<dir>] [--keep=14]');
  process.exit(1);
}
const outDir = path.resolve(typeof args.out === 'string' ? args.out : path.join(__dirname, '..', '..', 'KasiGuruBackups'));
const mirrorDir = typeof args.mirror === 'string' ? path.resolve(args.mirror) : null;

fs.mkdirSync(outDir, { recursive: true });
const logFile = path.join(outDir, 'backup_daily.log');

function log(message) {
  for (const line of String(message).split(/\r?\n/).filter((l) => l.trim())) {
    const stamped = `${new Date().toISOString()} ${line}`;
    console.log(stamped);
    fs.appendFileSync(logFile, stamped + '\n');
  }
}

function rotate(dir, label) {
  const { remove, held } = planRotation(listBackups(dir), keep);
  for (const name of remove) fs.rmSync(path.join(dir, name), { recursive: true, force: true });
  if (remove.length) log(`${label}: removed ${remove.length} old backup(s): ${remove.join(', ')}`);
  if (held.length) log(`${label}: holding ${held.length} backup(s) over twice the size of the newest: ${held.join(', ')}`);
}

let exitCode = 0;
const fail = (code) => { if (exitCode !== 1) exitCode = code; };

// 1. The backup itself.
const before = new Set(listBackups(outDir).map((b) => b.name));
const run = spawnSync(process.execPath, [path.join(__dirname, 'backup_firestore.js'), keyFile, outDir], {
  env: { ...process.env, KASIGURU_BACKUP_DAILY: '1' },
  encoding: 'utf8',
  maxBuffer: 16 * 1024 * 1024
});
log(run.stdout || '');
log(run.stderr || '');
if (run.status !== 0) {
  log(`BACKUP FAILED: ${run.error ? run.error.message : `exit code ${run.status}`}`);
  fail(1);
}

// 2. Does the database look wiped? Per collection on the day it happens, and for as long as an
// older backup more than twice the size of the newest is still here - so the warning, and the
// task's last result, do not go quiet at the next run while nothing has been restored.
const backups = listBackups(outDir);
if (backups.length >= 2) {
  const newest = backups[backups.length - 1];
  const previous = backups[backups.length - 2];
  const shrunk = before.has(newest.name) ? [] : shrunkCollections(previous.manifest, newest.manifest);
  const larger = backups.filter((b) => b.total > newest.total * 2);
  const restoreFrom = larger.length ? larger[larger.length - 1] : previous;
  if (shrunk.length) log(`WARNING: since ${previous.name}: ${shrunk.join(', ')}.`);
  if (larger.length) {
    log(`WARNING: the newest backup holds ${newest.total} documents, under half the ${restoreFrom.total} in ${restoreFrom.name}.`);
  }
  if (shrunk.length || larger.length) {
    log('If nobody reset the database on purpose, it may have been wiped. Follow "If the database is attacked"');
    log(`in docs/BACKUP_AND_RESET.md and restore from ${restoreFrom.name} or an older backup.`);
    fail(2);
  }
}

for (const entry of fs.readdirSync(outDir, { withFileTypes: true })) {
  if (entry.isDirectory() && /^\d{4}-\d{2}-\d{2}T/.test(entry.name) && !backups.some((b) => b.name === entry.name)) {
    log(`ignored: ${entry.name} has no manifest.json - a run that died part-way, not a usable backup`);
  }
}

// 3. Rotation here.
rotate(outDir, 'local');

// 4. The copy off this machine.
if (mirrorDir) {
  try {
    fs.mkdirSync(mirrorDir, { recursive: true });
    for (const name of fs.readdirSync(mirrorDir)) {
      if (name.endsWith('.partial')) fs.rmSync(path.join(mirrorDir, name), { recursive: true, force: true });
    }
    // Only backups newer than the newest copy there. Copying every missing one would keep bringing
    // back the folders the mirror's own rotation removed.
    const mirrored = listBackups(mirrorDir);
    const newestThere = mirrored.length ? mirrored[mirrored.length - 1].name : '';
    let copied = 0;
    for (const b of listBackups(outDir).filter((b) => b.name > newestThere)) {
      // Under a temporary name until complete, so a copy cut short never passes for a backup.
      const partial = path.join(mirrorDir, b.name + '.partial');
      fs.cpSync(path.join(outDir, b.name), partial, { recursive: true });
      fs.renameSync(partial, path.join(mirrorDir, b.name));
      copied++;
    }
    log(`mirror: copied ${copied} backup(s) to ${mirrorDir}`);
    rotate(mirrorDir, 'mirror');
  } catch (err) {
    log(`MIRROR FAILED: ${err.message}`);
    fail(1);
  }
}

log(`finished with exit code ${exitCode}`);
process.exit(exitCode);
