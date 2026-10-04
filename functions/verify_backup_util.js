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
const admin = require('firebase-admin');
const {
  serialize,
  deserialize,
  readAllDocsDeep,
  writeAllDocsByPath,
  countCollectionDeep,
  deleteCollectionDeep,
  planRestore,
  approxBytes,
  planRotation,
  shrunkCollections,
  BATCH_MAX_BYTES
} = require('./firestore_backup_util');

// ── Minimal Firestore stand-ins ───────────────────────────────────────────────
// Only the surface the traversal actually touches: query paging, listDocuments, listCollections.

function makeSnapshot(docs) {
  return { empty: docs.length === 0, size: docs.length, docs };
}

const deletionOrder = [];

function makeDoc(parentPath, id, data, subcollections = {}) {
  const path = `${parentPath}/${id}`;
  const ref = {
    id,
    path,
    deleted: false,
    listCollections: async () => Object.entries(subcollections).map(([name, docs]) => makeCollection(path, name, docs)),
    delete: async () => { ref.deleted = true; deletionOrder.push(path); }
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
// Awaited, so an async check that throws is counted. Called synchronously, an async check's
// failure became an unhandled rejection after PASS had already been printed.
async function check(name, fn) {
  try {
    await fn();
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

  await check('finds every nested progress document', () => {
    const paths = real.map((d) => d.path).sort();
    assert.deepStrictEqual(paths, [
      'users/uidAAA/progress/main',
      'users/uidAAA/progress/wordStates',
      'users/uidBBB/progress/main',
      'users/uidCCC',
      'users/uidCCC/progress/lessonProgress'
    ]);
  });

  await check('records missing parents without inventing data for them', () => {
    assert.strictEqual(missing.length, 2);
    assert.deepStrictEqual(missing.map((d) => d.path).sort(), ['users/uidAAA', 'users/uidBBB']);
    assert.ok(missing.every((d) => d.data === null));
  });

  await check('the old flat read would have found nothing under users', async () => {
    // The regression itself: a query returns only documents that exist. Two of three users have
    // no fields, so the pre-fix backup captured one document and zero progress records.
    const snapshot = await users.orderBy('__name__').limit(300).get();
    assert.strictEqual(snapshot.docs.length, 1);
  });

  await check('carries a full path so nested docs can be restored where they came from', () => {
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

  await check('restores every real document and skips missing parents', () => {
    assert.strictEqual(restored, 5);
    assert.strictEqual(written.length, 5);
    assert.ok(!written.some((w) => w.path === 'users/uidAAA'));
  });

  await check('restores nested documents to their original paths', () => {
    assert.ok(written.some((w) => w.path === 'users/uidAAA/progress/main' && w.data.totalXp === 1200));
  });

  await check('a pre-format-2 backup entry still restores by id at the root', async () => {
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

  await check('counts parents and nested documents alike', () => {
    // 3 user parents + 4 progress documents.
    assert.strictEqual(counted, 7);
  });

  const toDelete = buildUsers();
  const deleted = await deleteCollectionDeep(fakeDb, toDelete, () => {});

  await check('deletes the same number it counted', () => {
    assert.strictEqual(deleted, counted);
  });

  await check('deletes children before their parent', () => {
    assert.ok(toDelete._built.every((d) => d.ref.deleted));
    for (const parent of ['users/uidAAA', 'users/uidBBB', 'users/uidCCC']) {
      const children = deletionOrder.filter((p) => p.startsWith(parent + '/'));
      assert.ok(children.length > 0, `${parent} owns no deleted children`);
      assert.ok(children.every((c) => deletionOrder.indexOf(c) < deletionOrder.indexOf(parent)), `${parent} went before its children`);
    }
  });

  console.log('\nSerialization');

  // Through JSON and back, as a backup file carries it.
  const precise = new admin.firestore.Timestamp(1759000001, 123456789);
  const roundTrip = deserialize(null, JSON.parse(JSON.stringify(
    serialize({ at: precise, photoUrl: null, meanings: [{ gloss: 'now', note: null }] })
  )));

  await check('keeps null fields, top-level and nested, apart from absent ones', () => {
    assert.ok('photoUrl' in roundTrip && roundTrip.photoUrl === null);
    assert.ok('note' in roundTrip.meanings[0] && roundTrip.meanings[0].note === null);
  });

  await check('keeps timestamps to the nanosecond', () => {
    assert.ok(roundTrip.at.isEqual(precise), `got ${roundTrip.at.seconds}s ${roundTrip.at.nanoseconds}ns`);
  });

  await check('still reads a timestamp from a backup that stored only milliseconds', () => {
    const legacy = deserialize(null, { __t: 'timestamp', v: '2025-09-27T19:06:41.123Z' });
    assert.strictEqual(legacy.toMillis(), Date.parse('2025-09-27T19:06:41.123Z'));
  });

  console.log('\nRestore plan (after a wipe)');

  const progressPlan = planRestore('users', [
    { id: 'uidAAA', path: 'users/uidAAA', data: null, missing: true },
    { id: 'main', path: 'users/uidAAA/progress/main', data: { totalXp: 1200, currentStreak: 7 } },
    { id: 'main', path: 'users/uidBBB/progress/main', data: { totalXp: 900, currentStreak: 3 } },
    { id: 'main', path: 'users/uidCCC/progress/main', data: { totalXp: 500, currentStreak: 2 } },
    { id: 'wordStates', path: 'users/uidAAA/progress/wordStates', data: { entries: { a: 1 }, updatedAt: 1 } }
  ], new Map([
    // This phone re-uploaded into the empty database and is further along than the backup.
    ['users/uidAAA/progress/main', { totalXp: 3900, currentStreak: 10 }],
    // This phone was reinstalled during the outage and started over.
    ['users/uidBBB/progress/main', { totalXp: 40, currentStreak: 1 }],
    ['users/uidAAA/progress/wordStates', { updatedAt: 1, entries: { a: 1 } }]
  ]));
  const writtenPaths = progressPlan.writes.map((e) => e.path).sort();

  await check('keeps progress a phone re-uploaded past the backup, so its next sync is not rejected', () => {
    assert.ok(!writtenPaths.includes('users/uidAAA/progress/main'));
    assert.strictEqual(progressPlan.kept, 1);
  });

  await check('restores progress over a cloud copy that is behind the backup', () => {
    assert.ok(writtenPaths.includes('users/uidBBB/progress/main'));
    assert.strictEqual(progressPlan.overwrite, 1);
  });

  await check('restores progress missing from the cloud, and never a missing parent', () => {
    assert.deepStrictEqual(writtenPaths, ['users/uidBBB/progress/main', 'users/uidCCC/progress/main']);
    assert.strictEqual(progressPlan.restore, 1);
    assert.strictEqual(progressPlan.parents, 1);
  });

  await check('does not rewrite a document that already matches, whatever its key order', () => {
    assert.strictEqual(progressPlan.same, 1);
  });

  await check('keeps a leaderboard row at the backup XP and a device token already present', () => {
    const rows = planRestore('leaderboard_public', [{ id: 'u1', path: 'leaderboard_public/u1', data: { totalXp: 100 } }],
      new Map([['leaderboard_public/u1', { totalXp: 100, weeklyXp: 5 }]]));
    const tokens = planRestore('device_tokens', [{ id: 'u1', path: 'device_tokens/u1', data: { token: 'old' } }],
      new Map([['device_tokens/u1', { token: 'new' }]]));
    assert.strictEqual(rows.kept + tokens.kept, 2);
    assert.strictEqual(rows.writes.length + tokens.writes.length, 0);
  });

  await check('overwrites a vandalised word, and ignores the updatedAt a previous restore stamped', () => {
    const words = planRestore('vocabulary', [
      { id: 'w1', data: { kasiguranin: 'apak', updatedAt: 1 } },
      { id: 'w2', data: { kasiguranin: 'lima', updatedAt: 1 } }
    ], new Map([
      ['vocabulary/w1', { kasiguranin: 'vandalised', updatedAt: 9 }],
      ['vocabulary/w2', { kasiguranin: 'lima', updatedAt: 9 }]
    ]), ['updatedAt']);
    assert.deepStrictEqual(words.writes.map((e) => e.id), ['w1']);
    assert.strictEqual(words.same, 1);
  });

  console.log('\nBatching');

  await check('closes a batch before the 10 MiB request limit (400 pronunciation clips)', async () => {
    const commits = [];
    const sizedDb = {
      doc: (p) => ({ path: p }),
      collection: (name) => ({ doc: (id) => ({ path: `${name}/${id}` }) }),
      batch: () => {
        const docs = [];
        return { set: (ref, data) => docs.push(data), commit: async () => { commits.push(docs); } };
      }
    };
    const clip = Buffer.alloc(29 * 1024);
    const n = await writeAllDocsByPath(sizedDb, 'word_audio',
      Array.from({ length: 400 }, (_, i) => ({ id: `w${i}`, data: { audio: clip } })));
    assert.strictEqual(n, 400);
    assert.strictEqual(commits.flat().length, 400);
    assert.ok(commits.length >= 2, `${commits.length} batch for ~11.6 MB`);
    for (const docs of commits) {
      assert.ok(docs.reduce((sum, d) => sum + approxBytes(d), 0) <= BATCH_MAX_BYTES);
    }
  });

  await check('splits a batch Firestore refuses as too big, until every document is written', async () => {
    const committed = [];
    const pickyDb = {
      doc: (p) => ({ path: p }),
      collection: (name) => ({ doc: (id) => ({ path: `${name}/${id}` }) }),
      batch: () => {
        const docs = [];
        return {
          set: (ref) => docs.push(ref.path),
          commit: async () => {
            // Production refused 400 learner documents at once; refuse anything over 60 here.
            if (docs.length > 60) throw Object.assign(new Error('3 INVALID_ARGUMENT: Transaction too big. Decrease transaction size.'), { code: 3 });
            committed.push(docs);
          }
        };
      }
    };
    const n = await writeAllDocsByPath(pickyDb, 'users',
      Array.from({ length: 400 }, (_, i) => ({ path: `users/u${i}/progress/main`, data: { totalXp: i } })));
    assert.strictEqual(n, 400);
    assert.strictEqual(new Set(committed.flat()).size, 400);
    assert.ok(committed.every((docs) => docs.length <= 60));
  });

  await check('does not retry a batch that failed for any other reason', async () => {
    const failingDb = {
      doc: (p) => ({ path: p }),
      collection: (name) => ({ doc: (id) => ({ path: `${name}/${id}` }) }),
      batch: () => ({ set: () => {}, commit: async () => { throw Object.assign(new Error('7 PERMISSION_DENIED'), { code: 7 }); } })
    };
    await assert.rejects(writeAllDocsByPath(failingDb, 'users', [{ path: 'users/u1', data: {} }, { path: 'users/u2', data: {} }]), /PERMISSION_DENIED/);
  });

  console.log('\nRotation');

  const daily = (count, total, startDay = 0) => Array.from({ length: count }, (_, i) => ({
    name: new Date(Date.UTC(2026, 7, 2 + startDay + i)).toISOString().replace(/[:.]/g, '-'),
    total
  }));

  await check('keeps the newest backups and the first of each month, removes the rest', () => {
    const backups = daily(40, 4800); // 2026-08-02 .. 2026-09-10
    const { remove, held } = planRotation(backups, 14);
    assert.strictEqual(remove.length, 40 - 14 - 1);
    assert.strictEqual(held.length, 0);
    assert.ok(!remove.includes(backups[0].name), 'the first August backup was removed');
    assert.ok(backups.slice(-14).every((b) => !remove.includes(b.name)));
  });

  await check('after a wipe, empty daily backups never rotate the good ones out', () => {
    const good = daily(20, 4800);
    const wiped = daily(15, 0, 20);
    const { remove, held } = planRotation([...good, ...wiped], 14);
    assert.ok(good.every((b) => !remove.includes(b.name)), 'a good backup was removed');
    assert.ok(held.length > 0);
  });

  await check('flags a collection that lost more than half its documents', () => {
    const shrunk = shrunkCollections(
      { collections: { users: 820, vocabulary: 1151, stories: 3 } },
      { collections: { users: 0, vocabulary: 1151 } }
    );
    assert.deepStrictEqual(shrunk, ['users 820 -> 0']);
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
