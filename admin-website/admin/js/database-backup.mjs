// database-backup.mjs — the full backup, restore and factory reset behind the portal's Backup &
// Restore page.
//
// A backup is what functions/backup_firestore.js writes: one <collection>.json per root collection,
// every document with its full path (users/{uid}/progress/* included) and manifest.json, format 2.
// This module reads and writes exactly that, so a zip downloaded from the page unzips to a folder
// restore_firestore.js accepts, and a daily backup folder dropped on the page restores from it.
//
// `client` is the service-account Firestore client from firestore-rest.mjs, already speaking the
// backup's value format: { projectId, listCollectionIds(path), listDocuments(path, { showMissing }),
// getDocuments(paths), writeDocuments(entries, onWritten) }. Security rules do not apply to it,
// which is the point: learner data is owner-writable only, so an admin login could neither read
// all of it nor put it back.
//
// readAllDocsDeep and planRestore are ports of functions/firestore_backup_util.js, whose Admin SDK
// cannot load in a browser, and the factory reset is a port of functions/reset_firestore.js.
// functions/verify_backup_util.js runs both copies on the same fixtures and checks the reset lists;
// change them together.

export const FORMAT = 2;

// Collections whose documents own subcollections. Walking one costs a call per document, so only
// `users` (users/{uid}/progress/*, rewardReceipts/*) is walked, as in the CLI.
export const DEEP_COLLECTIONS = ['users'];

// What the app syncs incrementally by updatedAt (FirestoreSyncManager): restored copies get a fresh
// one, so phones pull them on their next sync instead of at the daily full reconcile.
export const APP_SYNCED_CONTENT = new Set(['vocabulary', 'stories']);

// What a factory reset deletes, the same lists as functions/firestore_backup_util.js
// RESET_LEARNER_COLLECTIONS and RESET_CONTENT_COLLECTIONS; verify_backup_util.js checks they agree.
// The content is written back from the backup; the learner data is not.
export const RESET_LEARNER_COLLECTIONS = [
  'users', 'leaderboard_public', 'public_profiles', 'device_tokens', 'security_questions',
  'word_submissions', 'literature_submissions', 'issue_reports'
];
export const RESET_CONTENT_COLLECTIONS = [
  'vocabulary', 'word_audio', 'vocabulary_trash', 'dictionary_updates', 'stories', 'story_page_images',
  'announcements', 'app_releases', 'sentence_submissions'
];

// Learner data: what "Learner data only" puts back after a reset, and what the runbook restores
// without reading when the day's reads are used up.
export const LEARNER_COLLECTIONS = RESET_LEARNER_COLLECTIONS;

// A daily backup folder's name: the time it was taken with `:` and `.` made filename-safe.
export function backupStamp(date) {
  return date.toISOString().replace(/[:.]/g, '-');
}

async function mapInOrder(items, limit, fn) {
  const results = new Array(items.length);
  let next = 0;
  const worker = async () => {
    while (next < items.length) {
      const i = next++;
      results[i] = await fn(items[i]);
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, worker));
  return results;
}

/**
 * Every document under a collection, nested subcollections included, as { id, path, data } with
 * data in the backup's format. A *missing* document (no fields, but it owns a subcollection, as most
 * users/{uid} are) does not come back from a query; it is listed with showMissing and recorded as
 * { missing: true, data: null } so its subcollections are walked and nothing is invented for it.
 */
export async function readAllDocsDeep(client, collectionPath, onRead = () => {}) {
  const id = collectionPath.split('/').pop();
  const deep = DEEP_COLLECTIONS.includes(id);
  const listed = await client.listDocuments(collectionPath, { showMissing: deep });
  onRead(listed.length);
  const docId = (path) => path.split('/').pop();

  const out = listed
    .filter((d) => d.exists)
    .map((d) => ({ id: docId(d.path), path: d.path, data: d.data }));
  if (!deep) return out;

  const nested = await mapInOrder(listed, 6, async (d) => {
    const items = d.exists ? [] : [{ id: docId(d.path), path: d.path, data: null, missing: true }];
    for (const sub of await client.listCollectionIds(d.path)) {
      items.push(...await readAllDocsDeep(client, `${d.path}/${sub}`, onRead));
    }
    return items;
  });
  for (const items of nested) out.push(...items);
  return out;
}

/**
 * Reads the whole database into backup files: [{ name: '<collection>.json' | 'manifest.json', text }].
 * onProgress({ collection, index, total, documents }) reports as it goes.
 */
export async function takeBackup(client, { now = new Date(), onProgress = () => {} } = {}) {
  const manifest = { projectId: client.projectId, createdAt: now.toISOString(), format: FORMAT, collections: {} };
  const files = [];
  let documents = 0;
  const ids = await client.listCollectionIds('');

  for (const [index, collection] of ids.entries()) {
    onProgress({ collection, index, total: ids.length, documents });
    const docs = await readAllDocsDeep(client, collection, (n) => {
      documents += n;
      onProgress({ collection, index, total: ids.length, documents });
    });
    const payload = {
      collection,
      count: docs.length,
      documents: docs.map((d) => ({ id: d.id, path: d.path, missing: d.missing || undefined, data: d.data }))
    };
    files.push({ name: `${collection}.json`, text: JSON.stringify(payload, null, 2) });
    // Real documents apart from missing parents, so a manifest can never read as healthy while the
    // learner progress it was meant to hold is absent.
    const real = docs.filter((d) => !d.missing).length;
    manifest.collections[collection] = real;
    if (real !== docs.length) {
      manifest.nestedParents = manifest.nestedParents || {};
      manifest.nestedParents[collection] = docs.length - real;
    }
  }

  // Last, as the CLI writes it: a backup without a manifest is one that never finished.
  files.push({ name: 'manifest.json', text: JSON.stringify(manifest, null, 2) });
  return { stamp: backupStamp(now), manifest, files };
}

const normalise = (path) => path.replace(/\\/g, '/').replace(/^\/+/, '');
const baseName = (path) => path.slice(path.lastIndexOf('/') + 1);
const dirName = (path) => path.slice(0, Math.max(path.lastIndexOf('/'), 0));

/**
 * Which of the dropped paths make up the backup: manifest.json and the .json files beside it. Run on
 * names alone, before anything is read, so dropping the whole KasiGuruBackups folder fails at once
 * instead of reading every backup in it first.
 */
export function selectBackupFiles(paths) {
  const norm = paths.map(normalise);
  const manifests = norm.filter((p) => baseName(p) === 'manifest.json');
  if (manifests.length === 0) {
    throw new Error('No manifest.json in what you dropped. Drop a backup folder, or the .zip this page downloads. ' +
      'A folder without one is a backup that never finished; pick an older one.');
  }
  if (manifests.length > 1) {
    throw new Error('That holds more than one backup. Drop a single backup folder or zip.');
  }
  const dir = dirName(manifests[0]);
  return paths.filter((p, i) => dirName(norm[i]) === dir && /\.json$/i.test(norm[i]));
}

/**
 * The backup in dropped files, [{ path, text }]: manifest.json and its <collection>.json files.
 * Throws with a message for the page when it is not one.
 */
export function readBackupFiles(entries, fallbackName = 'backup') {
  const keep = new Set(selectBackupFiles(entries.map((e) => e.path)));
  const files = entries.filter((e) => keep.has(e.path)).map((e) => ({ ...e, path: normalise(e.path) }));
  const manifestFile = files.find((f) => baseName(f.path) === 'manifest.json');
  const dir = dirName(manifestFile.path);
  let manifest;
  try { manifest = JSON.parse(manifestFile.text); } catch { manifest = null; }
  if (!manifest || typeof manifest !== 'object' || !manifest.collections || typeof manifest.collections !== 'object') {
    throw new Error('manifest.json is damaged, so this backup cannot be trusted. Pick another one.');
  }

  const collections = [];
  for (const f of files) {
    if (f === manifestFile) continue;
    let parsed;
    try { parsed = JSON.parse(f.text); } catch { parsed = null; }
    if (!parsed || typeof parsed.collection !== 'string' || !Array.isArray(parsed.documents)) {
      throw new Error(`${baseName(f.path)} is damaged or is not part of a backup.`);
    }
    const real = parsed.documents.filter((d) => !d.missing && d.data !== null).length;
    collections.push({ collection: parsed.collection, documents: parsed.documents, real, parents: parsed.documents.length - real });
  }
  if (collections.length === 0) throw new Error('This backup has a manifest but no collection files.');
  collections.sort((a, b) => a.collection.localeCompare(b.collection));
  return { name: dir ? baseName(dir) : fallbackName, manifest, collections };
}

// ── Restore decisions (port of firestore_backup_util.planRestore) ──────────────────────────────

// Documents whose rules cap how far one client write may move them (totalXp +2,000, streak +1,
// words +50, level +5 over the cloud copy). After a wipe phones re-upload into the empty database;
// writing the older backup copy over that leaves the phone further ahead than the caps allow, and
// every sync it makes afterwards is rejected for good. The cloud copy is kept when it is at least as
// far along as the backup.
const DELTA_CAPPED = [/^users\/[^/]+\/progress\/main$/, /^leaderboard_public\/[^/]+$/];

// The app saves its current FCM token at every start, so a token already in the cloud is newer.
const KEEP_IF_PRESENT = [/^device_tokens\/[^/]+$/];

/** A document whose restore decision depends on what a phone may have written since the preview. */
export function isGuardedPath(path) {
  return DELTA_CAPPED.some((re) => re.test(path)) || KEEP_IF_PRESENT.some((re) => re.test(path));
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  }
  return value;
}

/**
 * What a restore writes, document by document. `entries` are a backup file's documents; `current`
 * maps each path now in Firestore to its data, both in the backup's format. Actions: restore
 * (missing), overwrite (differs), same, kept (see above), parent (a missing parent: never written).
 * `ignoreFields` are left out of the comparison: the restore stamps a fresh updatedAt.
 */
export function planRestore(collection, entries, current, ignoreFields = []) {
  const comparable = (data) => {
    const copy = { ...data };
    for (const field of ignoreFields) delete copy[field];
    return JSON.stringify(canonical(copy));
  };
  const decisions = [];
  for (const entry of entries) {
    if (entry.missing || entry.data === null) {
      decisions.push({ entry, action: 'parent' });
      continue;
    }
    const docPath = entry.path || (entry.id ? `${collection}/${entry.id}` : null);
    const live = docPath ? current.get(docPath) : undefined;
    let action;
    if (live === undefined) action = 'restore';
    else if (comparable(live) === comparable(entry.data)) action = 'same';
    else if (KEEP_IF_PRESENT.some((re) => re.test(docPath))) action = 'kept';
    else if (DELTA_CAPPED.some((re) => re.test(docPath)) && Number(live.totalXp) >= Number(entry.data.totalXp)) action = 'kept';
    else action = 'overwrite';
    decisions.push({ entry, path: docPath, action });
  }
  const count = (action) => decisions.filter((d) => d.action === action).length;
  return {
    writes: decisions.filter((d) => d.action === 'restore' || d.action === 'overwrite').map((d) => d.entry),
    restore: count('restore'),
    overwrite: count('overwrite'),
    same: count('same'),
    kept: count('kept'),
    parents: count('parent')
  };
}

const entryPath = (collection, entry) => entry.path || `${collection}/${entry.id}`;

/**
 * The dry run: reads what Firestore holds now for each chosen collection and decides every write.
 * With writeOnly nothing is read and every document is written; that is for the day reads are
 * refused, and safe only while those collections are empty or older than the backup.
 */
export async function previewRestore(client, backup, { only = null, writeOnly = false, onProgress = () => {} } = {}) {
  const chosen = backup.collections.filter((c) => !only || only.has(c.collection));
  const plans = [];
  let documents = 0;
  for (const [index, c] of chosen.entries()) {
    onProgress({ collection: c.collection, index, total: chosen.length, documents });
    const live = writeOnly ? [] : await readAllDocsDeep(client, c.collection, (n) => {
      documents += n;
      onProgress({ collection: c.collection, index, total: chosen.length, documents });
    });
    const current = new Map(live.filter((d) => !d.missing).map((d) => [d.path, d.data]));
    const ignore = APP_SYNCED_CONTENT.has(c.collection) ? ['updatedAt'] : [];
    plans.push({ collection: c.collection, entries: c.documents, current, ignore, plan: planRestore(c.collection, c.documents, current, ignore) });
  }
  return plans;
}

/**
 * Re-reads, just before writing, the planned writes whose decision depends on a phone: progress,
 * leaderboard rows and device tokens. Between the preview and the click on Restore a phone may have
 * synced, and writing the backup over it then is exactly what `kept` exists to prevent. Costs one
 * read per such document. Returns how many writes were dropped.
 */
export async function recheckGuarded(client, plans) {
  const guarded = plans.flatMap((p) => p.plan.writes.map((e) => entryPath(p.collection, e)).filter(isGuardedPath));
  if (guarded.length === 0) return 0;
  const fresh = await client.getDocuments(guarded);
  let dropped = 0;
  for (const p of plans) {
    const before = p.plan.writes.length;
    for (const entry of p.plan.writes) {
      const path = entryPath(p.collection, entry);
      if (!isGuardedPath(path)) continue;
      if (fresh.has(path)) p.current.set(path, fresh.get(path));
      else p.current.delete(path);
    }
    p.plan = planRestore(p.collection, p.entries, p.current, p.ignore);
    dropped += before - p.plan.writes.length;
  }
  return dropped;
}

/** Writes every planned document; onProgress({ collection, done, total }). Returns how many. */
export async function runRestore(client, plans, { now = Date.now(), onProgress = () => {} } = {}) {
  let written = 0;
  for (const { collection, plan } of plans) {
    const entries = plan.writes
      .filter((e) => e.path || e.id)
      .map((e) => ({
        path: entryPath(collection, e),
        data: APP_SYNCED_CONTENT.has(collection) ? { ...e.data, updatedAt: now } : e.data
      }));
    if (entries.length === 0) continue;
    onProgress({ collection, done: 0, total: entries.length });
    written += await client.writeDocuments(entries, (done) => onProgress({ collection, done, total: entries.length }));
  }
  return written;
}

export function sumPlans(plans) {
  const sum = (key) => plans.reduce((n, p) => n + p.plan[key], 0);
  return { restore: sum('restore'), overwrite: sum('overwrite'), same: sum('same'), kept: sum('kept'), writes: sum('restore') + sum('overwrite') };
}

// ── Factory reset (port of functions/reset_firestore.js --mode=factory) ────────────────────────

/**
 * The content a factory reset writes back: collection -> its real documents, for each content
 * collection the backup has a file for. Throws when it has none, because a factory reset with
 * nothing to restore from would only delete.
 */
export function resetContent(backup) {
  const content = new Map();
  for (const name of RESET_CONTENT_COLLECTIONS) {
    const c = backup.collections.find((x) => x.collection === name);
    if (c) content.set(name, c.documents.filter((d) => !d.missing && d.data !== null && (d.path || d.id)));
  }
  if (content.size === 0) {
    throw new Error('This backup holds none of the content collections, so a factory reset from it would only delete. Pick a full backup.');
  }
  return content;
}

/**
 * The dry run, as reset_firestore.js prints it: for every collection the reset empties, how many
 * documents it holds now and, for the content, how many the backup writes back (null when the
 * backup has no file for it, so it stays empty). Reads every document once.
 */
export async function previewFactoryReset(client, backup, { onProgress = () => {} } = {}) {
  const content = resetContent(backup);
  const targets = [...RESET_LEARNER_COLLECTIONS, ...RESET_CONTENT_COLLECTIONS];
  const rows = [];
  let documents = 0;
  for (const [index, collection] of targets.entries()) {
    onProgress({ collection, index, total: targets.length, documents });
    const live = await readAllDocsDeep(client, collection, (n) => {
      documents += n;
      onProgress({ collection, index, total: targets.length, documents });
    });
    const isContent = RESET_CONTENT_COLLECTIONS.includes(collection);
    rows.push({
      collection,
      content: isContent,
      delete: live.filter((d) => !d.missing).length,
      restore: isContent && content.has(collection) ? content.get(collection).length : null
    });
  }
  return rows;
}

/**
 * Deletes every document of the learner and content collections, then writes the content back
 * from the backup as it was stored, with the backup's own updatedAt, as reset_firestore.js does.
 * Each collection is read again first, so whatever was written since the preview goes too.
 * onProgress({ phase: 'read' | 'delete' | 'write', collection, done, total }).
 *
 * Returns { deleted, restored }, collection -> count. If it stops part-way the error carries
 * err.deleted and err.restored as far as they got, so the page can say what is left to put back.
 */
export async function runFactoryReset(client, backup, { onProgress = () => {} } = {}) {
  const content = resetContent(backup);
  const deleted = {};
  const restored = {};
  try {
    for (const collection of [...RESET_LEARNER_COLLECTIONS, ...RESET_CONTENT_COLLECTIONS]) {
      onProgress({ phase: 'read', collection, done: 0, total: 0 });
      const live = await readAllDocsDeep(client, collection);
      // Children before their parent, as the script deletes. A missing parent is no document: it
      // goes when the last document under it does.
      const paths = live.filter((d) => !d.missing).map((d) => d.path)
        .sort((a, b) => b.split('/').length - a.split('/').length);
      deleted[collection] = 0;
      onProgress({ phase: 'delete', collection, done: 0, total: paths.length });
      await client.deleteDocuments(paths, (done) => {
        deleted[collection] = done;
        onProgress({ phase: 'delete', collection, done, total: paths.length });
      });
    }
    for (const [collection, docs] of content) {
      const entries = docs.map((d) => ({ path: entryPath(collection, d), data: d.data }));
      restored[collection] = 0;
      if (entries.length === 0) continue;
      onProgress({ phase: 'write', collection, done: 0, total: entries.length });
      await client.writeDocuments(entries, (done) => {
        restored[collection] = done;
        onProgress({ phase: 'write', collection, done, total: entries.length });
      });
    }
  } catch (err) {
    err.deleted = deleted;
    err.restored = restored;
    throw err;
  }
  return { deleted, restored };
}
