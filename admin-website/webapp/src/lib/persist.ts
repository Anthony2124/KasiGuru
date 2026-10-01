/**
 * Device storage. IndexedDB through idb-keyval, because localStorage's ~5 MB and synchronous writes
 * are a poor fit for review state on a 1,200-word corpus plus cached audio.
 *
 * Every call is wrapped: private browsing, a full disk or a blocked origin must degrade to "nothing
 * saved on this device", never to a broken app.
 */
import { createStore, del, get, set } from 'idb-keyval';

const store = (() => {
  try {
    return createStore('kasiguru', 'kv');
  } catch {
    return undefined;
  }
})();

export async function load<T>(key: string): Promise<T | undefined> {
  if (!store) return undefined;
  try {
    return (await get(key, store)) as T | undefined;
  } catch {
    return undefined;
  }
}

export async function save(key: string, value: unknown): Promise<void> {
  if (!store) return;
  try {
    await set(key, value, store);
  } catch {
    // Storage unavailable: the session still works, it just won't survive a reload.
  }
}

export async function remove(key: string): Promise<void> {
  if (!store) return;
  try {
    await del(key, store);
  } catch {
    /* ignore */
  }
}

/** Asks the browser not to evict our data under storage pressure (Safari honours this for installed apps). */
export async function requestPersistence() {
  try {
    if (navigator.storage?.persist && !(await navigator.storage.persisted())) await navigator.storage.persist();
  } catch {
    /* ignore */
  }
}
