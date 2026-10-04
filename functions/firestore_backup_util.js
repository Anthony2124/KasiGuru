/**
 * Shared helpers for backup_firestore.js / restore_firestore.js / reset_firestore.js /
 * backup_daily.js: Admin SDK start-up, type-safe JSON serialization (Timestamps, refs, GeoPoints,
 * bytes), paginated reads / batched writes, and the restore and rotation decisions.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const PAGE_SIZE = 300;

/**
 * Starts the Admin SDK and reports where it points, so every script can say "LIVE project" before
 * it does anything.
 *
 * `emulator:<project>` in place of a key file runs against the local Firestore emulator with no
 * credential at all. The id must start with `demo-`, which Firebase treats as emulator-only: a drill
 * run with FIRESTORE_EMULATOR_HOST forgotten then fails instead of reaching production.
 */
function initAdmin(keyArg) {
  const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST;
  if (typeof keyArg === 'string' && keyArg.startsWith('emulator:')) {
    const projectId = keyArg.slice('emulator:'.length);
    if (!emulatorHost) {
      throw new Error('emulator:<project> needs FIRESTORE_EMULATOR_HOST set, e.g. 127.0.0.1:8080');
    }
    if (!projectId.startsWith('demo-')) {
      throw new Error('an emulator project id must start with "demo-" so it can never reach a real project');
    }
    admin.initializeApp({ projectId });
    return { projectId, target: `emulator at ${emulatorHost}` };
  }
  if (!keyArg || !fs.existsSync(keyArg)) throw new Error(`service-account key not found: ${keyArg}`);
  // Resolved against the working directory, as fs.existsSync above was. A bare require(keyArg)
  // resolves against this file's folder, or as a package name.
  const keyPath = path.resolve(keyArg);
  admin.initializeApp({ credential: admin.credential.cert(keyPath) });
  return {
    projectId: require(keyPath).project_id,
    target: emulatorHost ? `emulator at ${emulatorHost}` : 'LIVE project'
  };
}

function serialize(value) {
  if (value === null || value === undefined) return null;
  if (value instanceof admin.firestore.Timestamp) {
    // `v` alone is milliseconds; seconds + nanoseconds keep the full value. `v` stays so a restore
    // script from before this change can still read the file.
    return { __t: 'timestamp', v: value.toDate().toISOString(), s: value.seconds, n: value.nanoseconds };
  }
  if (value instanceof admin.firestore.DocumentReference) {
    return { __t: 'ref', v: value.path };
  }
  if (value instanceof admin.firestore.GeoPoint) {
    return { __t: 'geopoint', v: [value.latitude, value.longitude] };
  }
  if (Buffer.isBuffer(value)) {
    return { __t: 'bytes', v: value.toString('base64') };
  }
  if (Array.isArray(value)) return value.map(serialize);
  if (typeof value === 'object') {
    const out = {};
    for (const key of Object.keys(value)) {
      // A null field is data - Firestore keeps it apart from an absent one, and the dashboard writes
      // explicit nulls into vocabulary (partOfSpeech, meaningEnglish...). Only undefined is dropped.
      if (value[key] !== undefined) out[key] = serialize(value[key]);
    }
    return out;
  }
  return value;
}

function deserialize(db, value) {
  if (value === null) return null;
  if (Array.isArray(value)) return value.map((v) => deserialize(db, v));
  if (typeof value === 'object') {
    if (value.__t === 'timestamp') {
      if (typeof value.s === 'number') return new admin.firestore.Timestamp(value.s, value.n);
      return admin.firestore.Timestamp.fromDate(new Date(value.v));
    }
    if (value.__t === 'ref') return db.doc(value.v);
    if (value.__t === 'geopoint') {
      return new admin.firestore.GeoPoint(value.v[0], value.v[1]);
    }
    if (value.__t === 'bytes') return Buffer.from(value.v, 'base64');
    const out = {};
    for (const key of Object.keys(value)) out[key] = deserialize(db, value[key]);
    return out;
  }
  return value;
}

async function readAllDocs(colRef) {
  const docs = [];
  let last = null;
  while (true) {
    let query = colRef.orderBy('__name__').limit(PAGE_SIZE);
    if (last) query = query.startAfter(last);
    const snapshot = await query.get();
    if (snapshot.empty) break;
    for (const doc of snapshot.docs) {
      docs.push({ id: doc.id, data: doc.data() });
    }
    if (snapshot.size < PAGE_SIZE) break;
    last = snapshot.docs[snapshot.docs.length - 1];
  }
  return docs;
}

async function writeAllDocs(db, collectionName, entries) {
  const col = db.collection(collectionName);
  let batch = db.batch();
  let count = 0;
  for (const entry of entries) {
    const ref = entry.id ? col.doc(entry.id) : col.doc();
    batch.set(ref, entry.data);
    count++;
    if (count % 400 === 0) {
      await batch.commit();
      batch = db.batch();
    }
  }
  await batch.commit();
  return count;
}

/**
 * Collections whose documents own subcollections and must therefore be walked, not just listed.
 *
 * This list exists because recursion is not free: discovering a document's subcollections costs one
 * metadata call per document, so walking `vocabulary` would cost ~1,250 calls to find nothing. Only
 * `users` has subcollections in this schema (users/{uid}/progress/{doc}). Override with
 * KASIGURU_DEEP_COLLECTIONS="users,something_else" if that ever changes.
 */
const DEEP_COLLECTIONS = (process.env.KASIGURU_DEEP_COLLECTIONS || 'users')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

/**
 * Every document under a collection, including documents nested in subcollections.
 *
 * Uses `listDocuments()` rather than a query for the parent level, which is the whole point: a
 * Firestore document that has never been written but owns a subcollection is a *missing* document.
 * It does not come back from a query, so `users` read as zero documents while holding every
 * learner's synced progress underneath it — which is exactly how a year of backups can contain no
 * user data and still look successful in the manifest.
 *
 * Returned entries carry a full `path` so a restore can address them at any depth.
 */
async function readAllDocsDeep(colRef) {
  const out = [];

  // Fast path for the data itself: one paginated query beats N individual gets.
  for (const doc of await readAllDocs(colRef)) {
    out.push({ path: `${colRef.path}/${doc.id}`, id: doc.id, data: doc.data });
  }

  if (!DEEP_COLLECTIONS.includes(colRef.id)) return out;

  const seen = new Set(out.map((d) => d.path));
  for (const ref of await colRef.listDocuments()) {
    // A missing parent has no fields to back up, but its subcollections must still be walked.
    if (!seen.has(ref.path)) {
      out.push({ path: ref.path, id: ref.id, data: null, missing: true });
    }
    for (const sub of await ref.listCollections()) {
      for (const nested of await readAllDocsDeep(sub)) out.push(nested);
    }
  }

  return out;
}

// A batched write is one request, and Firestore caps a request at 10 MiB. Pronunciation clips are
// stored as bytes of up to 400 KB and average ~29 KB, so 400 of them come to ~12 MB: a batch closed
// on document count alone would sink the word_audio restore at its first commit. A batch closes at
// whichever limit it reaches first.
const BATCH_MAX_DOCS = 400;
const BATCH_MAX_BYTES = 8 * 1024 * 1024;

/** A document's size, near enough to keep a batch under the request limit. */
function approxBytes(value) {
  if (value === null || value === undefined) return 1;
  if (Buffer.isBuffer(value)) return value.length;
  if (typeof value === 'string') return Buffer.byteLength(value) + 1;
  if (typeof value !== 'object') return 8;
  if (value instanceof admin.firestore.Timestamp) return 8;
  if (value instanceof admin.firestore.GeoPoint) return 16;
  if (value instanceof admin.firestore.DocumentReference) return Buffer.byteLength(value.path) + 1;
  let total = 0;
  if (Array.isArray(value)) {
    for (const item of value) total += approxBytes(item);
    return total;
  }
  for (const [key, item] of Object.entries(value)) total += Buffer.byteLength(key) + 1 + approxBytes(item);
  return total;
}

function isTooBig(err) {
  return Boolean(err) && (err.code === 3 || err.code === 'invalid-argument') &&
    /too big|too large|exceeds/i.test(err.message || '');
}

// Firestore also refuses a batch whose index entries are too many ("Transaction too big"), which no
// byte count predicts: every key in a progress document's `entries` map is indexed. On production,
// 400 learner documents per batch were refused while the emulator, which does not enforce the
// limit, accepted them. A batch is all-or-nothing, so a refused one is split in half and retried,
// down to single documents.
async function commitSplitting(db, writes) {
  const batch = db.batch();
  for (const w of writes) batch.set(w.ref, w.data);
  try {
    await batch.commit();
  } catch (err) {
    if (!isTooBig(err) || writes.length === 1) throw err;
    const mid = Math.ceil(writes.length / 2);
    await commitSplitting(db, writes.slice(0, mid));
    await commitSplitting(db, writes.slice(mid));
  }
}

/** Writes entries addressed by full `path`, falling back to `id` for backups made before paths. */
async function writeAllDocsByPath(db, collectionName, entries) {
  let pending = [];
  let batchBytes = 0;
  let count = 0;
  for (const entry of entries) {
    // A missing parent was never a real document; recreating it would invent data that never existed.
    if (entry.missing || entry.data === null) continue;
    const ref = entry.path ? db.doc(entry.path) : db.collection(collectionName).doc(entry.id);
    const size = approxBytes(entry.data);
    if (pending.length > 0 && (pending.length >= BATCH_MAX_DOCS || batchBytes + size > BATCH_MAX_BYTES)) {
      await commitSplitting(db, pending);
      pending = [];
      batchBytes = 0;
    }
    pending.push({ ref, data: entry.data });
    batchBytes += size;
    count++;
  }
  if (pending.length > 0) await commitSplitting(db, pending);
  return count;
}

// Documents whose Firestore rules cap how far one client write may move them: totalXp +2,000,
// streak +1, words +50, level +5 over what the cloud already holds (firestore.rules,
// isValidMainProgress and isValidLeaderboardEntry). After a wipe, phones re-upload their full
// progress into the empty database. Writing the older backup copy over that would leave the phone
// further ahead than the caps allow, and every write it makes afterwards is rejected - silently,
// and for good, because the gap never closes. The cloud copy is kept when it is at least as far
// along as the backup.
const DELTA_CAPPED = [/^users\/[^/]+\/progress\/main$/, /^leaderboard_public\/[^/]+$/];

// The app saves its current FCM token at every start (KasiGuruApp.registerFcmToken), so a token
// already in the cloud is newer than the backup's, which FCM may have retired since.
const KEEP_IF_PRESENT = [/^device_tokens\/[^/]+$/];

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  }
  return value;
}

/**
 * Decides, document by document, what a restore writes. `entries` are a backup file's documents as
 * stored (serialized); `current` maps each document path now in Firestore to its serialized data.
 *
 * Every other learner document is overwritten when it differs. That is safe without the rule above:
 * achievements, game levels, lesson progress and word states carry no caps, a reward receipt only
 * refuses a value lower than the cloud's (a backup copy is never ahead of the phone's), and the app
 * merges them additively at its next sync (LearningStateMerge), so a phone that was ahead puts its
 * own newer entries straight back.
 *
 * `ignoreFields` are top-level fields left out of the comparison: the restore stamps a fresh
 * updatedAt on what it writes, which would otherwise make every re-run rewrite the same documents.
 */
function planRestore(collection, entries, current, ignoreFields = []) {
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

const BACKUP_FOLDER = /^\d{4}-\d{2}-\d{2}T[\d-]+Z$/;

/**
 * The finished backups in a folder, oldest first. A run writes manifest.json last, so a folder
 * without one is a run that died part-way and is not a backup anyone should restore from.
 */
function listBackups(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true })
    .filter((e) => e.isDirectory() && BACKUP_FOLDER.test(e.name))
    .filter((e) => fs.existsSync(path.join(dir, e.name, 'manifest.json')))
    .map((e) => {
      const manifest = JSON.parse(fs.readFileSync(path.join(dir, e.name, 'manifest.json'), 'utf8'));
      const total = Object.values(manifest.collections || {}).reduce((sum, n) => sum + n, 0);
      return { name: e.name, total, manifest };
    })
    .sort((a, b) => a.name.localeCompare(b.name));
}

/**
 * Which backups a rotation removes: everything but the newest `keep` and the oldest of each
 * calendar month, except a backup holding more than twice the documents of the newest one.
 *
 * That exception is what a wipe looks like from here. Without it, the empty backups taken every
 * day after an attack would rotate the last good ones out within `keep` days. A larger backup is
 * held until the newest is comparable again - after a restore - or until someone removes it by hand.
 */
function planRotation(backups, keep) {
  const sorted = [...backups].sort((a, b) => a.name.localeCompare(b.name));
  if (sorted.length === 0) return { remove: [], held: [] };
  const newest = sorted[sorted.length - 1];
  const kept = new Set(sorted.slice(-keep).map((b) => b.name));
  const months = new Set();
  for (const b of sorted) {
    const month = b.name.slice(0, 7);
    if (!months.has(month)) {
      months.add(month);
      kept.add(b.name);
    }
  }
  const remove = [];
  const held = [];
  for (const b of sorted) {
    if (kept.has(b.name)) continue;
    (b.total > newest.total * 2 ? held : remove).push(b.name);
  }
  return { remove, held };
}

/** Collections that lost more than half their documents between two manifests. */
function shrunkCollections(previous, next) {
  const before = previous.collections || {};
  const after = next.collections || {};
  return Object.keys(before)
    .filter((name) => before[name] >= 10 && (after[name] || 0) < before[name] / 2)
    .map((name) => `${name} ${before[name]} -> ${after[name] || 0}`);
}

/**
 * Deletes every document in a collection, including anything in its subcollections.
 *
 * Depth-first: a document is removed only after its subcollections are, so an interrupted run can
 * be re-run without leaving orphaned nested documents that nothing can reach or list.
 */
async function deleteCollectionDeep(db, colRef, onProgress) {
  let deleted = 0;
  // The same guard readAllDocsDeep already applies: checking every document in every collection
  // for subcollections it can never have is a real, avoidable cost against a live database - one
  // extra round trip per document, for six of the seven collections a learner reset touches, none
  // of which ever have children. Found by running this for real: the dry-run count against
  // production took over four minutes and was still on its first collection when this was added.
  const mightHaveChildren = DEEP_COLLECTIONS.includes(colRef.id);

  for (const ref of await colRef.listDocuments()) {
    if (mightHaveChildren) {
      for (const sub of await ref.listCollections()) {
        deleted += await deleteCollectionDeep(db, sub, onProgress);
      }
    }
    await ref.delete();
    deleted++;
    if (onProgress && deleted % 100 === 0) onProgress(colRef.path, deleted);
  }

  return deleted;
}

/** Counts documents the same way [deleteCollectionDeep] would remove them, without deleting. */
async function countCollectionDeep(db, colRef) {
  let total = 0;
  const mightHaveChildren = DEEP_COLLECTIONS.includes(colRef.id);
  for (const ref of await colRef.listDocuments()) {
    if (mightHaveChildren) {
      for (const sub of await ref.listCollections()) {
        total += await countCollectionDeep(db, sub);
      }
    }
    total++;
  }
  return total;
}

module.exports = {
  initAdmin,
  serialize,
  deserialize,
  readAllDocs,
  readAllDocsDeep,
  writeAllDocs,
  writeAllDocsByPath,
  approxBytes,
  deleteCollectionDeep,
  countCollectionDeep,
  planRestore,
  listBackups,
  planRotation,
  shrunkCollections,
  BATCH_MAX_BYTES,
  DEEP_COLLECTIONS
};
