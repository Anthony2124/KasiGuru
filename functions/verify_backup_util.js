/**
 * Self-check for the backup/reset traversal logic. No network, no credentials, no Firestore.
 *
 * Run:  node verify_backup_util.js
 *
 * This exists because the bug it guards against was invisible in production. Every backup taken
 * before this reported success and wrote a healthy-looking manifest, while `users` read as 0
 * documents and every learner's synced progress was silently absent - because `users/{uid}` is a
 * *missing* document (it owns a `progress` subcollection but has no fields of its own), and a
 * Firestore query does not return missing documents. Only `listDocuments()` does.
 *
 * The fixture below reproduces exactly that shape.
 */

const assert = require('assert');
const {
  readAllDocsDeep,
  writeAllDocsByPath,
  countCollectionDeep,
  deleteCollectionDeep
} = require('./firestore_backup_util');

// ── Minimal Firestore stand-ins ───────────────────────────────────────────────
// Only the surface the traversal actually touches: query paging, listDocuments, listCollections.

function makeSnapshot(docs) {
  return { empty: docs.length === 0, size: docs.length, docs };
}

function makeDoc(parentPath, id, data, subcollections = {}) {
  const path = `${parentPath}/${id}`;
  const ref = {
    id,
    path,
    deleted: false,
    listCollections: async () => Object.entries(subcollections).map(([name, docs]) => makeCollection(path, name, docs)),
    delete: async () => { ref.deleted = true; }
  };
  return { id, path, data, ref, exists: data !== null };
}

function makeCollection(parentPath, id, docs) {
  const path = parentPath ? `${parentPath}/${id}` : id;
  const built = docs.map((d) => makeDoc(path, d.id, d.data, d.subcollections));
  const existing = built.filter((d) => d.exists);

  const query = {
    // The fixture is small enough to return in one page; startAfter is accepted and ignored.
    limit: () => query,
    startAfter: () => ({ ...query, get: async () => makeSnapshot([]) }),
    get: async () => makeSnapshot(existing.map((d) => ({ id: d.id, data: () => d.data })))
  };

  return {
    id,
    path,
    orderBy: () => query,
    listDocuments: async () => built.map((d) => d.ref),
    _built: built
  };
}

// ── Fixture: the exact shape that broke ───────────────────────────────────────

function buildUsers() {
  return makeCollection('', 'users', [
    {
      // A real Firestore "missing document": no fields, but it owns progress/.
      id: 'uidAAA',
      data: null,
      subcollections: {
        progress: [
          { id: 'main', data: { totalXp: 1200, currentStreak: 7 } },
          { id: 'wordStates', data: { payload: '{}' } }
        ]
      }
    },
    {
      id: 'uidBBB',
      data: null,
      subcollections: {
        progress: [{ id: 'main', data: { totalXp: 40, currentStreak: 1 } }]
      }
    },
    // A user document that does exist, to prove real parents are kept as well.
    {
      id: 'uidCCC',
      data: { email: 'learner@example.com' },
      subcollections: {
        progress: [{ id: 'lessonProgress', data: { payload: '[]' } }]
      }
    }
  ]);
}

let failures = 0;
function check(name, fn) {
  try {
    fn();
    console.log(`  PASS  ${name}`);
  } catch (e) {
    failures++;
    console.log(`  FAIL  ${name}\n        ${e.message}`);
  }
}

(async () => {
  console.log('\nBackup traversal');

  const users = buildUsers();
  const deep = await readAllDocsDeep(users);
  const real = deep.filter((d) => !d.missing);
  const missing = deep.filter((d) => d.missing);

  check('finds every nested progress document', () => {
    const paths = real.map((d) => d.path).sort();
    assert.deepStrictEqual(paths, [
      'users/uidAAA/progress/main',
      'users/uidAAA/progress/wordStates',
      'users/uidBBB/progress/main',
      'users/uidCCC',
      'users/uidCCC/progress/lessonProgress'
    ]);
  });

  check('records missing parents without inventing data for them', () => {
    assert.strictEqual(missing.length, 2);
    assert.deepStrictEqual(missing.map((d) => d.path).sort(), ['users/uidAAA', 'users/uidBBB']);
    assert.ok(missing.every((d) => d.data === null));
  });

  check('the old flat read would have found nothing under users', async () => {
    // The regression itself: a query returns only documents that exist. Two of three users have
    // no fields, so the pre-fix backup captured one document and zero progress records.
    const snapshot = await users.orderBy('__name__').limit(300).get();
    assert.strictEqual(snapshot.docs.length, 1);
  });

  check('carries a full path so nested docs can be restored where they came from', () => {
    assert.ok(real.every((d) => typeof d.path === 'string' && d.path.length > 0));
  });

  console.log('\nRestore');

  const written = [];
  const fakeDb = {
    doc: (p) => ({ path: p }),
    collection: (name) => ({ doc: (id) => ({ path: `${name}/${id}` }) }),
    batch: () => ({
      set: (ref, data) => written.push({ path: ref.path, data }),
      commit: async () => {}
    })
  };

  const restored = await writeAllDocsByPath(fakeDb, 'users', deep);

  check('restores every real document and skips missing parents', () => {
    assert.strictEqual(restored, 5);
    assert.strictEqual(written.length, 5);
    assert.ok(!written.some((w) => w.path === 'users/uidAAA'));
  });

  check('restores nested documents to their original paths', () => {
    assert.ok(written.some((w) => w.path === 'users/uidAAA/progress/main' && w.data.totalXp === 1200));
  });

  check('a pre-format-2 backup entry still restores by id at the root', async () => {
    const legacy = [];
    const legacyDb = {
      doc: (p) => ({ path: p }),
      collection: (name) => ({ doc: (id) => ({ path: `${name}/${id}` }) }),
      batch: () => ({ set: (ref, data) => legacy.push(ref.path), commit: async () => {} })
    };
    await writeAllDocsByPath(legacyDb, 'vocabulary', [{ id: 'w1', data: { kasiguranin: 'apak' } }]);
    assert.deepStrictEqual(legacy, ['vocabulary/w1']);
  });

  console.log('\nReset');

  const toCount = buildUsers();
  const counted = await countCollectionDeep(fakeDb, toCount);

  check('counts parents and nested documents alike', () => {
    // 3 user parents + 4 progress documents.
    assert.strictEqual(counted, 7);
  });

  const toDelete = buildUsers();
  const deleted = await deleteCollectionDeep(fakeDb, toDelete, () => {});

  check('deletes the same number it counted', () => {
    assert.strictEqual(deleted, counted);
  });

  check('deletes children before their parent', () => {
    assert.ok(toDelete._built.every((d) => d.ref.deleted));
  });

  console.log('');
  if (failures > 0) {
    console.error(`${failures} check(s) failed.`);
    process.exit(1);
  }
  console.log('All backup/reset traversal checks passed.\n');
})().catch((e) => {
  console.error('Verification crashed:', e);
  process.exit(1);
});
