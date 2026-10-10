// Only public dictionary content is stored here, separately for each signed-in admin.
// The longest a saved dictionary goes without reading every word, fingerprint or not: the backstop
// for an edit that carries no stamp, such as one made in the Firebase console. It was a day, which
// cost ~1,150 reads per staff member and browser every day even when nothing had changed.
const FULL_READ_MS = 7 * 24 * 60 * 60 * 1000;
const CHECK_MS = 5 * 60 * 1000;
// A fingerprint that disagrees is checked once more after this long before it costs a full read:
// a save still on its way, or an edit the listener has not delivered yet, settles within seconds.
const CONFIRM_MS = 30 * 1000;
const OVERLAP_MS = 2 * 60 * 1000;

/**
 * Numeric fields whose collection-wide sums, with the count, fingerprint the dictionary, as the
 * Android sync does. `updatedAt` moves with every stamped edit; `themeProposedAt` is what the stage
 * tagger (functions/tag_themes.js) stamps instead, so its proposals reach Stage Review without
 * waiting for a full read.
 */
export const FINGERPRINT_SUMS = ['updatedAt', 'themeProposedAt'];

/** The fingerprint of saved rows. Like Firestore's sum(), it skips values that are not numbers. */
export function fingerprintOf(rows) {
  const sums = Object.fromEntries(FINGERPRINT_SUMS.map((field) => [field, 0]));
  for (const row of rows) {
    for (const field of FINGERPRINT_SUMS) if (Number.isFinite(row[field])) sums[field] += row[field];
  }
  return { count: rows.length, sums };
}

// Exact while a sum fits in a double; past 2^53 the server and the browser may round a few
// milliseconds apart, far less than any real edit moves it.
function sameSum(a, b) {
  if (a === b) return true;
  const larger = Math.max(Math.abs(a), Math.abs(b));
  return larger > Number.MAX_SAFE_INTEGER && Math.abs(a - b) <= larger * 1e-15;
}

function sameFingerprint(a, b) {
  return a.count === b.count &&
    FINGERPRINT_SUMS.every((field) => sameSum(a.sums[field] ?? 0, b.sums[field] ?? 0));
}

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
 * Load once, then listen only for stamped edits. A fingerprint (count and sums, a few reads)
 * compared with the saved rows catches withdrawals outside the delta query and tagger runs; a
 * full read happens only when it disagrees twice in a row, or weekly as the backstop.
 * A fingerprint that disagrees right after a full read cannot be trusted (an odd stored value, or
 * an edit landing mid-read), so the count alone decides until the two agree again. That keeps one
 * odd value from causing a full read at every check.
 * The checkpoint is taken BEFORE a read/listen, with overlap for timestamp boundaries. Local or
 * pending snapshots never advance it. Failed reads keep the saved dictionary and retry later.
 */
export function watchVocabulary({ store, readAll, fingerprint, listen, onRows, onError,
  now = Date.now, schedule = setTimeout, cancel = clearTimeout, isVisible = () => true }) {
  let rows = new Map();
  let fullAt = 0;
  let since = 0;
  let exact = true;
  let doubted = false;
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
    const value = { rows: [...rows.values()], fullAt, since, exact };
    saving = saving.then(() => { if (!stopped) return store.save(value); }).catch((error) => {
      if (!warnedStorage) console.warn('Dictionary cache unavailable:', error);
      warnedStorage = true;
    });
  }
  function later(delay = CHECK_MS) {
    cancel(timer);
    if (!stopped) timer = schedule(check, delay);
  }
  // Whether the saved rows still match the server. A full match also restores trust in the sums.
  async function inStep() {
    const server = await fingerprint();
    const local = fingerprintOf([...rows.values()]);
    if (sameFingerprint(server, local)) {
      if (!exact) { exact = true; persist(); }
      return true;
    }
    return !exact && server.count === local.count;
  }
  async function check() {
    if (stopped) return;
    if (!isVisible()) { later(); return; }
    let delay = CHECK_MS;
    try {
      if (now() - fullAt >= FULL_READ_MS) await refresh();
      // Without a live listener the rows lack recent stamped edits; catch up first, then compare.
      else if (!listening) subscribe(true);
      else if (await inStep()) doubted = false;
      else if (doubted) await refresh();
      else { doubted = true; delay = CONFIRM_MS; }
    } catch (error) { if (!stopped) onError(error); }
    finally { later(delay); }
  }
  function subscribe(verify) {
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
        if (verify) { verify = false; void check(); }
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
      const [server, all] = await Promise.all([fingerprint().catch(() => null), readAll()]);
      if (stopped) return;
      rows = new Map(all.map((row) => [row.id, row]));
      exact = Boolean(server) && sameFingerprint(server, fingerprintOf(all));
      doubted = false;
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
      exact = cached.exact !== false;
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
