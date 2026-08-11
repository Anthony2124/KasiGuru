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

module.exports = { serialize, deserialize, readAllDocs, writeAllDocs };
