/**
 * Shared helpers for backup_firestore.js / restore_firestore.js:
 * type-safe JSON serialization (Timestamps, refs, GeoPoints, bytes) and
 * paginated reads / batched writes.
 */

const admin = require('firebase-admin');

const PAGE_SIZE = 300;

function serialize(value) {
  if (value === null || value === undefined) return null;
  if (value instanceof admin.firestore.Timestamp) {
    return { __t: 'timestamp', v: value.toDate().toISOString() };
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
      const s = serialize(value[key]);
      if (s !== null) out[key] = s;
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

/** Writes entries addressed by full `path`, falling back to `id` for backups made before paths. */
async function writeAllDocsByPath(db, collectionName, entries) {
  let batch = db.batch();
  let count = 0;
  for (const entry of entries) {
    // A missing parent was never a real document; recreating it would invent data that never existed.
    if (entry.missing || entry.data === null) continue;
    const ref = entry.path ? db.doc(entry.path) : db.collection(collectionName).doc(entry.id);
    batch.set(ref, entry.data);
    count++;
    if (count % 400 === 0) {
      await batch.commit();
      batch = db.batch();
    }
  }
  if (count % 400 !== 0) await batch.commit();
  return count;
}

/**
 * Deletes every document in a collection, including anything in its subcollections.
 *
 * Depth-first: a document is removed only after its subcollections are, so an interrupted run can
 * be re-run without leaving orphaned nested documents that nothing can reach or list.
 */
async function deleteCollectionDeep(db, colRef, onProgress) {
  let deleted = 0;

  for (const ref of await colRef.listDocuments()) {
    for (const sub of await ref.listCollections()) {
      deleted += await deleteCollectionDeep(db, sub, onProgress);
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
  for (const ref of await colRef.listDocuments()) {
    for (const sub of await ref.listCollections()) {
      total += await countCollectionDeep(db, sub);
    }
    total++;
  }
  return total;
}

module.exports = {
  serialize,
  deserialize,
  readAllDocs,
  readAllDocsDeep,
  writeAllDocs,
  writeAllDocsByPath,
  deleteCollectionDeep,
  countCollectionDeep,
  DEEP_COLLECTIONS
};
