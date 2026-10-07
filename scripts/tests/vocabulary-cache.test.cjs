const { test } = require('node:test');
const assert = require('node:assert/strict');

const settle = () => new Promise((resolve) => setImmediate(resolve));

async function setup(cached, options = {}) {
  const { watchVocabulary } = await import('../../admin-website/admin/js/vocabulary-cache.mjs');
  let time = options.now ?? 1_000_000;
  let next, fail, timer;
  let reads = 0, listens = 0, removals = 0;
  const published = [], saved = [], errors = [];
  let remote = options.remote ?? [{ id: '1', updatedAt: 100 }];
  const watcher = watchVocabulary({
    store: { load: async () => cached, save: async (value) => saved.push(structuredClone(value)) },
    readAll: async () => { reads++; if (options.offline) throw Error('offline'); return remote; },
    count: async () => { await options.count?.(); return remote.length; },
    listen: (since, callback, error) => {
      listens++; next = callback; fail = error;
      assert.ok(since < time);
      return () => { removals++; };
    },
    onRows: (rows) => published.push(structuredClone(rows)), onError: (error) => errors.push(error),
    now: () => time, schedule: (callback) => { timer = callback; return 1; }, cancel: () => {},
  });
  await watcher.ready;
  return {
    watcher, published, saved, errors,
    get reads() { return reads; }, get listens() { return listens; }, get removals() { return removals; },
    snapshot(changes = [], extra = {}) { next({ changes, fromCache: false, pending: false, ...extra }); },
    fail() { fail(Error('disconnected')); },
    advance(ms) { time += ms; }, setRemote(rows) { remote = rows; },
    tick: () => timer(),
  };
}
const cached = () => ({ rows: [{ id: '1', updatedAt: 100 }], fullAt: 900_000, since: 950_000 });

test('first visit reads once, while a warm reload uses the delta listener', async () => {
  const first = await setup();
  assert.equal(first.reads, 1);
  const warm = await setup(cached());
  warm.snapshot();
  await settle();
  assert.equal(warm.reads, 0);
  assert.equal(warm.listens, 1);
  assert.equal(warm.published.at(-1).length, 1);
});

test('a delta preserves old entries and overlays edits and additions by document id', async () => {
  const s = await setup(cached(), { remote: [{ id: '1' }, { id: '2' }] });
  s.snapshot([
    { type: 'modified', id: '1', row: { id: '1', updatedAt: 990_000 } },
    { type: 'added', id: '2', row: { id: '2', updatedAt: 995_000 } },
  ]);
  await settle();
  assert.equal(s.reads, 0);
  assert.deepEqual(s.published.at(-1).map((r) => r.id), ['1', '2']);
  assert.equal(s.saved.at(-1).rows[0].updatedAt, 990_000);
});

test('offline and pending snapshots do not advance the saved checkpoint', async () => {
  const s = await setup(cached());
  s.snapshot([], { fromCache: true });
  s.snapshot([], { pending: true });
  await settle();
  assert.equal(s.saved.length, 0);
  s.snapshot();
  await settle();
  assert.equal(s.saved.at(-1).since, 1_000_000);
});

test('a withdrawal outside the delta query is caught by the count check', async () => {
  const s = await setup(cached(), { remote: [] });
  s.snapshot();
  await settle();
  assert.equal(s.reads, 1);
  assert.deepEqual(s.published.at(-1), []);
  assert.equal(s.removals, 1);
});

test('daily reconciliation catches an unstamped edit even when the count stays equal', async () => {
  const s = await setup(cached());
  s.setRemote([{ id: '1', updatedAt: 100, acceptedStage: 'changed' }]);
  s.advance(24 * 60 * 60 * 1000);
  await s.tick();
  assert.equal(s.reads, 1);
  assert.equal(s.published.at(-1)[0].acceptedStage, 'changed');
});

test('an offline refresh preserves cached rows and retries after reconnecting', async () => {
  const options = { offline: true, now: 24 * 60 * 60 * 1000 + 1 };
  const old = cached(); old.fullAt = old.since = 1;
  const s = await setup(old, options);
  assert.equal(s.errors.length, 1);
  assert.equal(s.published.at(-1)[0].id, '1');
  options.offline = false;
  await s.tick();
  assert.equal(s.reads, 2);
});

test('listener failures reconnect without downloading an unchanged dictionary', async () => {
  const s = await setup(cached());
  s.fail();
  await s.tick();
  assert.equal(s.reads, 0);
  assert.equal(s.listens, 2);
});

test('a local deletion updates the cache and all dictionary views immediately', async () => {
  const s = await setup(cached());
  s.watcher.remove('1');
  await settle();
  assert.deepEqual(s.published.at(-1), []);
  assert.deepEqual(s.saved.at(-1).rows, []);
});

test('sign-out stops callbacks, timers and further cache writes', async () => {
  const s = await setup(cached());
  s.watcher.stop();
  s.snapshot([{ type: 'added', id: '2', row: { id: '2' } }]);
  await s.tick();
  assert.equal(s.published.length, 1);
  assert.equal(s.saved.length, 0);
  assert.equal(s.removals, 1);
});

test('sign-out during a count check cannot start a full dictionary read', async () => {
  let finish;
  const waiting = new Promise((resolve) => { finish = resolve; });
  const s = await setup(cached(), { remote: [], count: () => waiting });
  s.snapshot();
  s.watcher.stop();
  finish();
  await settle();
  assert.equal(s.reads, 0);
});

test('an invalid stored record is replaced by a full read', async () => {
  const s = await setup({ ...cached(), rows: [null] });
  assert.equal(s.reads, 1);
  assert.equal(s.published.at(-1)[0].id, '1');
});
