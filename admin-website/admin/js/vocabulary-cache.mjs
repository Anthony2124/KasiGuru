// Only public dictionary content is stored here, separately for each signed-in admin.
const FULL_READ_MS = 24 * 60 * 60 * 1000;
const CHECK_MS = 5 * 60 * 1000;
const OVERLAP_MS = 2 * 60 * 1000;

export function vocabularyStore(uid) {
  let connection;
  const key = `vocabulary:v1:${uid}`;
  const open = () => connection ??= new Promise((resolve, reject) => {
    const request = indexedDB.open('kasiguru-admin', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('content');
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  async function access(mode, value) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction('content', mode);
      const store = transaction.objectStore('content');
      const request = mode === 'readonly' ? store.get(key) : store.put(value, key);
      transaction.oncomplete = () => resolve(request.result);
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  }
  return { load: () => access('readonly'), save: (value) => access('readwrite', value) };
}

/**
 * Load once, then listen only for stamped edits. A cheap count catches withdrawals outside the
 * delta query; a daily full read also catches unstamped edits and delete/add pairs with equal counts.
 * The checkpoint is taken BEFORE a read/listen, with overlap for timestamp boundaries. Local or
 * pending snapshots never advance it. Failed reads keep the saved dictionary and retry later.
 */
export function watchVocabulary({ store, readAll, count, listen, onRows, onError,
  now = Date.now, schedule = setTimeout, cancel = clearTimeout, isVisible = () => true }) {
  let rows = new Map();
  let fullAt = 0;
  let since = 0;
  let stopped = false;
  let unsubscribe;
  let listening = false;
  let generation = 0;
  let timer;
  let refreshing;
  let saving = Promise.resolve();
  let warnedStorage = false;
  const publish = () => { if (!stopped) onRows([...rows.values()]); };
  function persist() {
    if (stopped) return;
    const value = { rows: [...rows.values()], fullAt, since };
    saving = saving.then(() => { if (!stopped) return store.save(value); }).catch((error) => {
      if (!warnedStorage) console.warn('Dictionary cache unavailable:', error);
      warnedStorage = true;
    });
  }
  function later(delay = CHECK_MS) {
    cancel(timer);
    if (!stopped) timer = schedule(check, delay);
  }
  async function check() {
    if (stopped) return;
    if (!isVisible()) { later(); return; }
    try {
      if (now() - fullAt >= FULL_READ_MS || await count() !== rows.size) await refresh();
      else if (!listening) subscribe(false);
    } catch (error) { if (!stopped) onError(error); }
    finally { later(); }
  }
  function subscribe(verifyCount) {
    if (stopped) return;
    unsubscribe?.();
    listening = true;
    const currentGeneration = ++generation;
    const startedAt = now();
    unsubscribe = listen(Math.max(0, since - OVERLAP_MS), (snapshot) => {
      if (stopped || currentGeneration !== generation) return;
      for (const change of snapshot.changes) {
        if (change.type === 'removed') rows.delete(change.id);
        else rows.set(change.id, change.row);
      }
      publish();
      if (!snapshot.fromCache && !snapshot.pending) {
        since = Math.max(since, startedAt);
        persist();
        if (verifyCount) { verifyCount = false; void check(); }
      }
    }, (error) => {
      if (stopped || currentGeneration !== generation) return;
      listening = false;
      if (!stopped) onError(error);
      later();
    });
    later();
  }
  function refresh() {
    if (stopped) return Promise.resolve();
    if (refreshing) return refreshing;
    refreshing = (async () => {
      const startedAt = now();
      const all = await readAll();
      if (stopped) return;
      rows = new Map(all.map((row) => [row.id, row]));
      fullAt = since = startedAt;
      publish();
      persist();
      subscribe(false);
    })().finally(() => { refreshing = undefined; });
    return refreshing;
  }
  const ready = (async () => {
    let cached;
    try { cached = await store.load(); }
    catch (error) { console.warn('Dictionary cache unavailable:', error); }
    if (stopped) return;
    const valid = Array.isArray(cached?.rows) && cached.rows.every((row) => row && typeof row.id === 'string') &&
      Number.isFinite(cached.fullAt) && cached.fullAt > 0 && cached.fullAt <= now() &&
      Number.isFinite(cached.since) && cached.since >= cached.fullAt && cached.since <= now();
    if (valid) {
      rows = new Map(cached.rows.map((row) => [row.id, row]));
      fullAt = cached.fullAt;
      since = cached.since;
      publish();
    }
    try {
      if (!valid || now() - fullAt >= FULL_READ_MS) await refresh();
      else subscribe(true);
    } catch (error) {
      if (!stopped) {
        onError(error);
        if (valid) subscribe(true);
        else later();
      }
    }
  })();
  return {
    ready,
    remove(id) { if (!stopped && rows.delete(id)) { publish(); persist(); } },
    stop() { stopped = true; unsubscribe?.(); cancel(timer); },
  };
}
