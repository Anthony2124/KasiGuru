// The portal's Backup & Restore page: zip, value format, deep read, restore decisions, REST client and
// key signing. No network and no credentials: Firestore and Google are stand-ins below. The Admin
// SDK copy of the same restore decisions is checked against this one by functions/verify_backup_util.js.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const nodeCrypto = require('node:crypto');

const load = (name) => import(`../../admin-website/admin/js/${name}`);

// ── Zip ────────────────────────────────────────────────────────────────────────────────────────

test('a zip round-trips text and bytes, stored or deflated, with folder paths and UTF-8 names', async () => {
  const { createZip, readZip } = await load('zip.mjs');
  const random = nodeCrypto.randomBytes(4096); // does not deflate, so it is stored
  const text = JSON.stringify({ word: 'mabuhay ñ', n: Array.from({ length: 500 }, (_, i) => i) }, null, 2);
  const blob = await createZip([
    { name: '2026-10-13T04-00-00-000Z/vocabulary.json', data: text },
    { name: '2026-10-13T04-00-00-000Z/ñame.bin', data: new Uint8Array(random) }
  ]);
  const files = await readZip(await blob.arrayBuffer());
  assert.deepEqual(files.map((f) => f.name), ['2026-10-13T04-00-00-000Z/vocabulary.json', '2026-10-13T04-00-00-000Z/ñame.bin']);
  assert.equal(new TextDecoder().decode(files[0].data), text);
  assert.deepEqual(Buffer.from(files[1].data), random);
  assert.ok(blob.size < text.length + random.length, 'the text was not compressed');
});

test('a zip read skips unwanted files and refuses damage', async () => {
  const { createZip, readZip } = await load('zip.mjs');
  const bytes = new Uint8Array(await (await createZip([
    { name: 'a/manifest.json', data: '{"format":2}' },
    { name: 'a/notes.txt', data: 'x'.repeat(100) }
  ])).arrayBuffer());
  const wanted = await readZip(bytes, (name) => name.endsWith('.json'));
  assert.deepEqual(wanted.map((f) => f.name), ['a/manifest.json']);
  await assert.rejects(readZip(new TextEncoder().encode('not a zip at all, just some text')), /damaged or is not a zip/);
  const flipped = bytes.slice();
  flipped[30 + 'a/manifest.json'.length + 1] ^= 0xFF; // inside the first file's data, past its header and name
  await assert.rejects(readZip(flipped), /damaged/);
});

// ── Values: Firestore REST <-> backup format ───────────────────────────────────────────────────

const REST_FIELDS = {
  word: { stringValue: 'apak' },
  count: { integerValue: '1200' },
  ratio: { doubleValue: 0.5 },
  whole: { doubleValue: 3 },
  ok: { booleanValue: false },
  none: { nullValue: null },
  at: { timestampValue: '2025-09-27T19:06:41.123456789Z' },
  atSecond: { timestampValue: '2025-09-27T19:06:41Z' },
  audio: { bytesValue: 'AAEC/w==' },
  owner: { referenceValue: 'projects/kasiguru-86042/databases/(default)/documents/users/u1' },
  place: { geoPointValue: { latitude: 14.5, longitude: 121 } },
  equator: { geoPointValue: { longitude: 10 } },
  tags: { arrayValue: { values: [{ stringValue: 'a' }, { mapValue: { fields: { n: { integerValue: '1' } } } }] } },
  empty: { arrayValue: {} },
  meta: { mapValue: {} }
};

const SERIALIZED = {
  word: 'apak',
  count: 1200,
  ratio: 0.5,
  whole: 3,
  ok: false,
  none: null,
  at: { __t: 'timestamp', v: '2025-09-27T19:06:41.123Z', s: 1759000001, n: 123456789 },
  atSecond: { __t: 'timestamp', v: '2025-09-27T19:06:41.000Z', s: 1759000001, n: 0 },
  audio: { __t: 'bytes', v: 'AAEC/w==' },
  owner: { __t: 'ref', v: 'users/u1' },
  place: { __t: 'geopoint', v: [14.5, 121] },
  equator: { __t: 'geopoint', v: [0, 10] },
  tags: ['a', { n: 1 }],
  empty: [],
  meta: {}
};

test('REST values read into the format functions/backup_firestore.js writes', async () => {
  const { fromRestFields } = await load('firestore-rest.mjs');
  assert.deepEqual(fromRestFields(REST_FIELDS), SERIALIZED);
});

test('backup values write back as the same Firestore values', async () => {
  const { fromRestFields, toRestFields } = await load('firestore-rest.mjs');
  const rest = toRestFields(SERIALIZED, 'kasiguru-86042');
  assert.equal(rest.at.timestampValue, '2025-09-27T19:06:41.123456789Z');
  assert.equal(rest.whole.integerValue, '3', 'a whole number is an integer, as the Admin SDK writes it');
  assert.equal(rest.ratio.doubleValue, 0.5);
  assert.equal(rest.owner.referenceValue, REST_FIELDS.owner.referenceValue);
  assert.deepEqual(fromRestFields(rest), SERIALIZED);
  // A backup from before nanoseconds were kept stores milliseconds only.
  assert.equal(toRestFields({ at: { __t: 'timestamp', v: '2025-09-27T19:06:41.123Z' } }, 'p').at.timestampValue, '2025-09-27T19:06:41.123000000Z');
});

// ── Deep read and backup files ─────────────────────────────────────────────────────────────────

// A Firestore with users whose parent documents are *missing*: no fields, but they own progress. A
// path is a document when it holds data, and a missing one when only documents below it do.
function fakeClient(tree, log = {}) {
  const docsUnder = (path) => {
    const depth = path.split('/').length + 1;
    const found = new Set(Object.keys(tree).filter((p) => p.startsWith(path + '/') && p.split('/').length >= depth)
      .map((p) => p.split('/').slice(0, depth).join('/')));
    return [...found];
  };
  const collectionsUnder = (path) => {
    const depth = path ? path.split('/').length + 1 : 1;
    const ids = new Set(Object.keys(tree).filter((p) => !path || p.startsWith(path + '/')).map((p) => p.split('/')[depth - 1]));
    return [...ids].sort();
  };
  const writes = [];
  return {
    projectId: 'demo-test',
    writes,
    listCollectionIds: async (path) => { log.lists = (log.lists || 0) + 1; return collectionsUnder(path); },
    listDocuments: async (path, { showMissing }) => {
      log.reads = (log.reads || 0) + 1;
      const exists = (p) => tree[p] !== null && tree[p] !== undefined;
      const listed = docsUnder(path).filter((p) => showMissing || exists(p));
      return listed.sort().map((p) => ({ path: p, exists: exists(p), data: exists(p) ? tree[p] : null }));
    },
    getDocuments: async (paths) => new Map(paths.filter((p) => tree[p]).map((p) => [p, tree[p]])),
    writeDocuments: async (entries, onWritten) => {
      for (const e of entries) { writes.push(e); tree[e.path] = e.data; }
      onWritten(entries.length);
      return entries.length;
    },
    deleteDocuments: async (paths, onDeleted) => {
      let done = 0;
      for (const p of paths) {
        if (log.failDeleteAt === p) throw Object.assign(new Error('Quota exceeded.'), { status: 'RESOURCE_EXHAUSTED' });
        (log.deleted = log.deleted || []).push(p);
        delete tree[p];
        onDeleted(++done);
      }
      return done;
    }
  };
}

const TREE = () => ({
  'users/uidAAA': null,
  'users/uidAAA/progress/main': { totalXp: 1200, currentStreak: 7 },
  'users/uidAAA/progress/wordStates': { payload: '{}' },
  'users/uidBBB': null,
  'users/uidBBB/progress/main': { totalXp: 40 },
  'users/uidCCC': { email: 'learner@example.com' },
  'users/uidCCC/progress/lessonProgress': { payload: '[]' },
  'vocabulary/w1': { kasiguranin: 'apak', updatedAt: 1 },
  'vocabulary/w2': { kasiguranin: 'lima', updatedAt: 1 },
  'device_tokens/uidAAA': { token: 'old' }
});

test('a backup finds learner progress under missing parents, and records the parents without data', async () => {
  const { takeBackup } = await load('database-backup.mjs');
  const { stamp, manifest, files } = await takeBackup(fakeClient(TREE()), { now: new Date('2026-10-13T04:00:00.000Z') });
  assert.equal(stamp, '2026-10-13T04-00-00-000Z');
  assert.equal(files.at(-1).name, 'manifest.json', 'the manifest is written last');
  assert.deepEqual(manifest, {
    projectId: 'demo-test', createdAt: '2026-10-13T04:00:00.000Z', format: 2,
    collections: { device_tokens: 1, users: 5, vocabulary: 2 }, nestedParents: { users: 2 }
  });
  const users = JSON.parse(files.find((f) => f.name === 'users.json').text);
  assert.equal(users.count, 7);
  assert.deepEqual(users.documents.filter((d) => d.missing).map((d) => [d.path, d.data]), [['users/uidAAA', null], ['users/uidBBB', null]]);
  assert.ok(users.documents.some((d) => d.path === 'users/uidAAA/progress/main' && d.data.totalXp === 1200 && d.id === 'main'));
  assert.ok(!('missing' in users.documents.find((d) => d.path === 'users/uidCCC')), 'missing is left out when false, as the CLI writes it');
});

test('a dropped backup is found by its manifest, and refused without one or with two', async () => {
  const { readBackupFiles, selectBackupFiles } = await load('database-backup.mjs');
  const manifest = JSON.stringify({ projectId: 'p', format: 2, collections: { vocabulary: 1 } });
  const vocab = JSON.stringify({ collection: 'vocabulary', count: 1, documents: [{ id: 'w1', path: 'vocabulary/w1', data: { kasiguranin: 'apak' } }] });
  const backup = readBackupFiles([
    { path: 'Backups\\2026-10-13T04-00-00-000Z\\manifest.json', text: manifest },
    { path: 'Backups\\2026-10-13T04-00-00-000Z\\vocabulary.json', text: vocab },
    { path: 'Backups\\backup_daily.json', text: 'not part of it' }
  ]);
  assert.equal(backup.name, '2026-10-13T04-00-00-000Z');
  assert.deepEqual(backup.collections.map((c) => [c.collection, c.real]), [['vocabulary', 1]]);
  assert.throws(() => selectBackupFiles(['a/vocabulary.json']), /No manifest\.json/);
  assert.throws(() => selectBackupFiles(['a/manifest.json', 'b/manifest.json']), /more than one backup/);
  assert.throws(() => readBackupFiles([{ path: 'manifest.json', text: '{' }]), /manifest\.json is damaged/);
  assert.throws(() => readBackupFiles([{ path: 'manifest.json', text: manifest }, { path: 'users.json', text: '[]' }]), /users\.json is damaged/);
});

// ── Restore decisions: the same cases as functions/verify_backup_util.js ───────────────────────

test('restore keeps progress a phone re-uploaded past the backup, and restores what is behind or missing', async () => {
  const { planRestore } = await load('database-backup.mjs');
  const plan = planRestore('users', [
    { id: 'uidAAA', path: 'users/uidAAA', data: null, missing: true },
    { id: 'main', path: 'users/uidAAA/progress/main', data: { totalXp: 1200, currentStreak: 7 } },
    { id: 'main', path: 'users/uidBBB/progress/main', data: { totalXp: 900, currentStreak: 3 } },
    { id: 'main', path: 'users/uidCCC/progress/main', data: { totalXp: 500, currentStreak: 2 } },
    { id: 'wordStates', path: 'users/uidAAA/progress/wordStates', data: { entries: { a: 1 }, updatedAt: 1 } }
  ], new Map([
    ['users/uidAAA/progress/main', { totalXp: 3900, currentStreak: 10 }],
    ['users/uidBBB/progress/main', { totalXp: 40, currentStreak: 1 }],
    ['users/uidAAA/progress/wordStates', { updatedAt: 1, entries: { a: 1 } }]
  ]));
  assert.deepEqual(plan.writes.map((e) => e.path).sort(), ['users/uidBBB/progress/main', 'users/uidCCC/progress/main']);
  assert.deepEqual([plan.restore, plan.overwrite, plan.same, plan.kept, plan.parents], [1, 1, 1, 1, 1]);
});

test('restore keeps leaderboard rows at the backup XP and present device tokens, and ignores a stamped updatedAt', async () => {
  const { planRestore } = await load('database-backup.mjs');
  const rows = planRestore('leaderboard_public', [{ id: 'u1', path: 'leaderboard_public/u1', data: { totalXp: 100 } }],
    new Map([['leaderboard_public/u1', { totalXp: 100, weeklyXp: 5 }]]));
  const tokens = planRestore('device_tokens', [{ id: 'u1', path: 'device_tokens/u1', data: { token: 'old' } }],
    new Map([['device_tokens/u1', { token: 'new' }]]));
  assert.equal(rows.kept + tokens.kept, 2);
  const words = planRestore('vocabulary', [
    { id: 'w1', data: { kasiguranin: 'apak', updatedAt: 1 } },
    { id: 'w2', data: { kasiguranin: 'lima', updatedAt: 1 } }
  ], new Map([['vocabulary/w1', { kasiguranin: 'vandalised', updatedAt: 9 }], ['vocabulary/w2', { kasiguranin: 'lima', updatedAt: 9 }]]), ['updatedAt']);
  assert.deepEqual(words.writes.map((e) => e.id), ['w1']);
});

test('a restore after a wipe writes the backup, stamps synced content, and a second run writes nothing', async () => {
  const { takeBackup, readBackupFiles, previewRestore, runRestore, sumPlans } = await load('database-backup.mjs');
  const { files } = await takeBackup(fakeClient(TREE()));
  const backup = readBackupFiles(files.map((f) => ({ path: `b/${f.name}`, text: f.text })));

  const wiped = { 'vocabulary/w1': { kasiguranin: 'vandalised', updatedAt: 5 }, 'vocabulary/w2': { kasiguranin: 'lima', updatedAt: 1 } };
  const client = fakeClient(wiped);
  const plans = await previewRestore(client, backup);
  assert.deepEqual(sumPlans(plans), { restore: 6, overwrite: 1, same: 1, kept: 0, writes: 7 });
  const written = await runRestore(client, plans, { now: 777 });
  assert.equal(written, 7);
  assert.equal(wiped['vocabulary/w1'].updatedAt, 777, 'restored words get a fresh updatedAt');
  assert.equal(wiped['users/uidAAA/progress/main'].updatedAt, undefined, 'learner documents are written as they were');
  assert.ok(!('users/uidAAA' in wiped), 'a missing parent is never invented');

  const again = await previewRestore(fakeClient(wiped), backup);
  assert.equal(sumPlans(again).writes, 0);
});

test('a phone that syncs between the preview and the restore is kept, not overwritten', async () => {
  const { takeBackup, readBackupFiles, previewRestore, recheckGuarded, runRestore, sumPlans } = await load('database-backup.mjs');
  const { files } = await takeBackup(fakeClient(TREE()));
  const backup = readBackupFiles(files.map((f) => ({ path: f.name, text: f.text })));
  const live = {};
  const client = fakeClient(live);
  const plans = await previewRestore(client, backup, { only: new Set(['users']) });
  assert.equal(sumPlans(plans).writes, 5);

  live['users/uidAAA/progress/main'] = { totalXp: 1300, currentStreak: 8 }; // the phone re-uploads
  assert.equal(await recheckGuarded(client, plans), 1);
  await runRestore(client, plans);
  assert.equal(live['users/uidAAA/progress/main'].totalXp, 1300);
  assert.equal(client.writes.length, 4);
});

test('write-only reads nothing and writes every chosen document', async () => {
  const { takeBackup, readBackupFiles, previewRestore, sumPlans } = await load('database-backup.mjs');
  const { files } = await takeBackup(fakeClient(TREE()));
  const backup = readBackupFiles(files.map((f) => ({ path: f.name, text: f.text })));
  const log = {};
  const plans = await previewRestore(fakeClient({ 'device_tokens/uidAAA': { token: 'new' } }, log), backup, { only: new Set(['device_tokens', 'users']), writeOnly: true });
  assert.equal(log.reads, undefined);
  assert.deepEqual(plans.map((p) => p.collection), ['device_tokens', 'users']);
  assert.equal(sumPlans(plans).writes, 6, 'nothing to compare against, so nothing is kept');
});

// ── Factory reset: the same lists and order as functions/reset_firestore.js ────────────────────

// A database with learner data, content, and what a reset leaves alone.
const LIVE = () => ({
  ...TREE(),
  'vocabulary/w3': { kasiguranin: 'added after the backup', updatedAt: 9 },
  'word_audio/w1': { data: { __t: 'bytes', v: 'AAEC' } },
  'leaderboard_public/uidAAA': { totalXp: 1200 },
  'sentence_submissions/s1': { status: 'pending' },
  'admin_audit_log/a1': { action: 'database.backup' },
  'admin_staff/v@example.com': { role: 'verifier' },
  'user_bans/uidBBB': { isBanned: true }
});

async function backupOf(tree) {
  const { takeBackup, readBackupFiles } = await load('database-backup.mjs');
  const { files } = await takeBackup(fakeClient(tree));
  return readBackupFiles(files.map((f) => ({ path: `b/${f.name}`, text: f.text })));
}

test('the reset preview counts what each collection holds now and what the backup writes back', async () => {
  const { previewFactoryReset, RESET_LEARNER_COLLECTIONS, RESET_CONTENT_COLLECTIONS } = await load('database-backup.mjs');
  const backup = await backupOf(TREE()); // has vocabulary, but no word_audio or sentence_submissions
  const log = {};
  const rows = await previewFactoryReset(fakeClient(LIVE(), log), backup);
  assert.deepEqual(rows.map((r) => r.collection), [...RESET_LEARNER_COLLECTIONS, ...RESET_CONTENT_COLLECTIONS]);
  const row = (name) => rows.find((r) => r.collection === name);
  assert.deepEqual(row('users'), { collection: 'users', content: false, delete: 5, restore: null }, 'missing parents are not documents');
  assert.deepEqual(row('vocabulary'), { collection: 'vocabulary', content: true, delete: 3, restore: 2 });
  assert.deepEqual(row('word_audio'), { collection: 'word_audio', content: true, delete: 1, restore: null }, 'not in the backup: stays empty');
  assert.equal(log.deleted, undefined, 'the preview deletes nothing');
});

test('a factory reset empties learner data, puts the content back exactly, and leaves the rest alone', async () => {
  const { runFactoryReset } = await load('database-backup.mjs');
  const backup = await backupOf({ ...TREE(), 'word_audio/w1': { data: { __t: 'bytes', v: 'AAEC' } }, 'sentence_submissions/s0': { status: 'pending' } });
  const live = LIVE();
  live['vocabulary/w1'] = { kasiguranin: 'vandalised', updatedAt: 5 };
  const log = {};
  const result = await runFactoryReset(fakeClient(live, log), backup);

  // A null entry is a missing parent, which is no document: it is gone once nothing is under it.
  assert.deepEqual(Object.keys(live).filter((p) => live[p] !== null).sort(), [
    'admin_audit_log/a1', 'admin_staff/v@example.com', 'sentence_submissions/s0', 'user_bans/uidBBB',
    'vocabulary/w1', 'vocabulary/w2', 'word_audio/w1'
  ]);
  assert.deepEqual(live['vocabulary/w1'], { kasiguranin: 'apak', updatedAt: 1 }, "written back with the backup's own updatedAt");
  assert.equal(result.deleted.users, 5);
  assert.equal(result.deleted.device_tokens, 1);
  assert.equal(result.deleted.vocabulary, 3);
  assert.deepEqual(result.restored, { vocabulary: 2, word_audio: 1, sentence_submissions: 1 });
  const users = log.deleted.filter((p) => p.startsWith('users/'));
  assert.ok(users.indexOf('users/uidCCC/progress/lessonProgress') < users.indexOf('users/uidCCC'), 'children before their parent');
});

test('a factory reset refuses a backup with no content, and says how far it got when it stops', async () => {
  const { runFactoryReset, previewFactoryReset } = await load('database-backup.mjs');
  const learnersOnly = await backupOf({ 'users/u1/progress/main': { totalXp: 1 } });
  const untouched = LIVE();
  await assert.rejects(previewFactoryReset(fakeClient(untouched), learnersOnly), /would only delete/);
  await assert.rejects(runFactoryReset(fakeClient(untouched), learnersOnly), /would only delete/);
  assert.deepEqual(untouched, LIVE(), 'nothing was deleted');

  const live = LIVE();
  const err = await runFactoryReset(fakeClient(live, { failDeleteAt: 'vocabulary/w2' }), await backupOf(TREE())).catch((e) => e);
  assert.equal(err.status, 'RESOURCE_EXHAUSTED');
  assert.equal(err.deleted.users, 5);
  assert.equal(err.deleted.vocabulary, 1, 'w1 went before the quota ran out');
  assert.deepEqual(err.restored, {});
  assert.ok(live['vocabulary/w2'], 'what it had not reached is still there');
});

// ── REST client ────────────────────────────────────────────────────────────────────────────────

function fakeFetch(handler) {
  const calls = [];
  const fetchImpl = async (url, init = {}) => {
    calls.push({ url: decodeURIComponent(url), method: init.method || 'GET', body: init.body ? JSON.parse(init.body) : null, auth: init.headers?.Authorization });
    const [status, body] = await handler(calls.at(-1), calls.length);
    return { ok: status < 300, status, json: async () => body };
  };
  return { calls, fetchImpl };
}

const ROOT = 'projects/demo-test/databases/(default)/documents';

test('the client pages through a collection and tells missing documents from real ones', async () => {
  const { firestoreRest } = await load('firestore-rest.mjs');
  const { calls, fetchImpl } = fakeFetch(({ url }) => url.includes('pageToken=next')
    ? [200, { documents: [{ name: `${ROOT}/users/u2`, createTime: 't', fields: { n: { integerValue: '2' } } }] }]
    : [200, { documents: [{ name: `${ROOT}/users/u1` }], nextPageToken: 'next' }]);
  const client = firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl });
  const docs = await client.listDocuments('users', { showMissing: true });
  assert.deepEqual(docs, [{ path: 'users/u1', exists: false, data: null }, { path: 'users/u2', exists: true, data: { n: 2 } }]);
  assert.ok(calls[0].url.includes('showMissing=true'));
  assert.equal(calls[0].auth, 'Bearer owner');
});

test('the client splits a commit Firestore refuses as too big, and keeps batches under the request limit', async () => {
  const { firestoreRest } = await load('firestore-rest.mjs');
  const committed = [];
  const { fetchImpl } = fakeFetch(({ body }) => {
    if (body.writes.length > 60) return [400, { error: { status: 'INVALID_ARGUMENT', message: 'Transaction too big. Decrease transaction size.' } }];
    committed.push(body.writes);
    return [200, {}];
  });
  const client = firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl });
  const n = await client.writeDocuments(Array.from({ length: 400 }, (_, i) => ({ path: `users/u${i}/progress/main`, data: { totalXp: i } })));
  assert.equal(n, 400);
  assert.equal(new Set(committed.flat().map((w) => w.update.name)).size, 400);
  assert.ok(committed.every((w) => w.length <= 60));

  const sizes = [];
  const sized = fakeFetch(({ body }) => { sizes.push(JSON.stringify(body).length); return [200, {}]; });
  const clip = { __t: 'bytes', v: Buffer.alloc(29 * 1024).toString('base64') };
  await firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl: sized.fetchImpl })
    .writeDocuments(Array.from({ length: 400 }, (_, i) => ({ path: `word_audio/w${i}`, data: { audio: clip } })));
  assert.ok(sizes.length >= 2 && sizes.every((s) => s <= 8 * 1024 * 1024 + 64), `batches of ${sizes.join(', ')} bytes`);
});

test('the client deletes by full document name, at most 400 to a commit', async () => {
  const { firestoreRest } = await load('firestore-rest.mjs');
  const commits = [];
  const { fetchImpl } = fakeFetch(({ url, body }) => { commits.push({ url, writes: body.writes }); return [200, {}]; });
  const client = firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl });
  const progress = [];
  const paths = Array.from({ length: 900 }, (_, i) => `users/u${i}/progress/main`);
  assert.equal(await client.deleteDocuments(paths, (n) => progress.push(n)), 900);
  assert.deepEqual(commits.map((c) => c.writes.length), [400, 400, 100]);
  assert.ok(commits.every((c) => c.url.endsWith(':commit')));
  assert.deepEqual(commits[0].writes[0], { delete: `${ROOT}/users/u0/progress/main` });
  assert.deepEqual(progress, [400, 800, 900]);
});

test('the client retries a brief outage but not a used-up quota', async () => {
  const { firestoreRest, isQuotaError } = await load('firestore-rest.mjs');
  const flaky = fakeFetch((_, n) => n < 3 ? [503, { error: { status: 'UNAVAILABLE', message: 'try again' } }] : [200, { collectionIds: ['users'] }]);
  const ok = firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl: flaky.fetchImpl, retryDelayMs: 0 });
  assert.deepEqual(await ok.listCollectionIds(''), ['users']);

  const spent = fakeFetch(() => [429, { error: { status: 'RESOURCE_EXHAUSTED', message: 'Quota exceeded.' } }]);
  const quota = firestoreRest({ projectId: 'demo-test', getToken: async () => 'owner', fetchImpl: spent.fetchImpl, retryDelayMs: 0 });
  const err = await quota.listCollectionIds('').catch((e) => e);
  assert.ok(isQuotaError(err));
  assert.match(err.message, /3 PM Philippine time/);
  assert.equal(spent.calls.length, 1);
});

// ── Service-account key ────────────────────────────────────────────────────────────────────────

function serviceAccount() {
  const { privateKey, publicKey } = nodeCrypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  const key = {
    type: 'service_account', project_id: 'kasiguru-86042', private_key_id: 'kid1',
    private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
    client_email: 'firebase-adminsdk-x@kasiguru-86042.iam.gserviceaccount.com', token_uri: 'https://oauth2.googleapis.com/token'
  };
  return { text: JSON.stringify(key), publicKey };
}

test('the key signs a token request Google can verify, and the token is reused until near expiry', async () => {
  const { loadServiceAccount } = await load('firestore-rest.mjs');
  const { text, publicKey } = serviceAccount();
  const requests = [];
  let time = 1_760_000_000_000;
  const fetchImpl = async (url, init) => {
    requests.push({ url, body: new URLSearchParams(init.body) });
    return { ok: true, status: 200, json: async () => ({ access_token: `token${requests.length}`, expires_in: 3600 }) };
  };
  const account = await loadServiceAccount(text, { fetchImpl, now: () => time });
  assert.equal(account.projectId, 'kasiguru-86042');
  assert.equal(await account.getToken(), 'token1');
  assert.equal(await account.getToken(), 'token1');
  time += 56 * 60_000;
  assert.equal(await account.getToken(), 'token2', 'renewed five minutes before it expires');

  const { url, body } = requests[0];
  assert.equal(url, 'https://oauth2.googleapis.com/token');
  assert.equal(body.get('grant_type'), 'urn:ietf:params:oauth:grant-type:jwt-bearer');
  const [header, claims, signature] = body.get('assertion').split('.');
  assert.ok(nodeCrypto.verify('sha256', Buffer.from(`${header}.${claims}`), publicKey, Buffer.from(signature, 'base64url')));
  assert.deepEqual(JSON.parse(Buffer.from(header, 'base64url')), { alg: 'RS256', typ: 'JWT', kid: 'kid1' });
  const c = JSON.parse(Buffer.from(claims, 'base64url'));
  assert.equal(c.iss, 'firebase-adminsdk-x@kasiguru-86042.iam.gserviceaccount.com');
  assert.equal(c.scope, 'https://www.googleapis.com/auth/datastore');
  assert.equal(c.aud, 'https://oauth2.googleapis.com/token');
  assert.equal(c.exp - c.iat, 3600);
});

test('a file that is not a key, or a key Google refuses, says so plainly', async () => {
  const { loadServiceAccount } = await load('firestore-rest.mjs');
  await assert.rejects(loadServiceAccount('{"apiKey":"AIza..."}'), /not a service-account key/);
  await assert.rejects(loadServiceAccount('not json'), /not a service-account key/);
  const refused = await loadServiceAccount(serviceAccount().text, {
    fetchImpl: async () => ({ ok: false, status: 400, json: async () => ({ error: 'invalid_grant', error_description: 'Invalid JWT Signature.' }) })
  });
  await assert.rejects(refused.getToken(), /deleted or replaced/);
});
