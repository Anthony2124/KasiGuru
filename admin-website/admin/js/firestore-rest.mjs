// firestore-rest.mjs — Firestore as the service account sees it, from the browser, for the Backup &
// Restore page. The Firebase web SDK signs in as a person and is bound by firestore.rules; this
// signs with the service-account key the operator drops on the page, so it reads and writes every
// collection, as functions/backup_firestore.js and restore_firestore.js do with the Admin SDK.
//
// The key never leaves the tab. Its private key is imported as a non-extractable CryptoKey, used
// only to sign a one-hour token request to Google, and forgotten with the tab. Values cross in the
// backup's own format (firestore_backup_util.serialize): Timestamps, bytes, references and GeoPoints
// as { __t, v } objects, so what this reads can be compared with, and written from, a backup file.

const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const SCOPE = 'https://www.googleapis.com/auth/datastore';
const PAGE_SIZE = 300;

// One commit is one request, capped at 10 MiB; a pronunciation clip is ~29 KB of bytes, ~39 KB as
// base64. A batch closes at whichever limit it reaches first, measured on the request body itself.
const BATCH_MAX_DOCS = 400;
const BATCH_MAX_BYTES = 8 * 1024 * 1024;

// ── Service-account key ─────────────────────────────────────────────────────────────────────────

function base64url(input) {
  const bytes = typeof input === 'string' ? new TextEncoder().encode(input) : input;
  let binary = '';
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function tokenError(body) {
  const text = `${body.error || ''} ${body.error_description || ''}`;
  if (/invalid_grant/i.test(text) && /signature|not found|deleted|disabled/i.test(text)) {
    return new Error('Google refused this key. It has been deleted or replaced: download the current one.');
  }
  if (/iat|exp|timeframe/i.test(text)) {
    return new Error("Google refused the sign-in because this computer's clock is wrong. Set the time automatically and try again.");
  }
  return new Error(`Google refused this key (${text.trim() || 'no reason given'}).`);
}

/**
 * Reads a service-account key file's text. Returns { projectId, clientEmail, getToken() }, where
 * getToken() returns a Google access token for Firestore, renewed five minutes before it expires.
 */
export async function loadServiceAccount(text, { fetchImpl = fetch, now = () => Date.now() } = {}) {
  let key;
  try { key = JSON.parse(text); } catch { key = null; }
  if (!key || key.type !== 'service_account' || !key.private_key || !key.client_email || !key.project_id) {
    throw new Error('That is not a service-account key. Firebase console → Project settings → Service accounts → Generate new private key makes one.');
  }
  const { project_id: projectId, client_email: clientEmail, private_key_id: keyId } = key;
  const pem = key.private_key.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, '').replace(/\s+/g, '');
  let signingKey;
  try {
    const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
    // Not extractable: no script on the page can read the private key back out of it.
    signingKey = await crypto.subtle.importKey('pkcs8', der, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign']);
  } catch {
    throw new Error('The private key in this file is damaged. Download a new key.');
  }

  let cached = null;
  async function getToken() {
    if (cached && cached.expiresAt - 5 * 60_000 > now()) return cached.token;
    const iat = Math.floor(now() / 1000);
    const unsigned = `${base64url(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid: keyId }))}.` +
      base64url(JSON.stringify({ iss: clientEmail, scope: SCOPE, aud: TOKEN_URL, iat, exp: iat + 3600 }));
    const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', signingKey, new TextEncoder().encode(unsigned));
    let res;
    try {
      res = await fetchImpl(TOKEN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
          assertion: `${unsigned}.${base64url(new Uint8Array(signature))}`
        })
      });
    } catch {
      throw new Error('Could not reach Google to sign in with the key. Check the internet connection.');
    }
    const body = await res.json().catch(() => ({}));
    if (!res.ok || !body.access_token) throw tokenError(body);
    cached = { token: body.access_token, expiresAt: now() + (body.expires_in || 3600) * 1000 };
    return cached.token;
  }

  return { projectId, clientEmail, getToken };
}

// ── Values: Firestore REST <-> backup format ────────────────────────────────────────────────────

function timestampFromRest(text) {
  const m = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:\d{2})$/.exec(text);
  if (!m) throw new Error(`Unreadable timestamp ${text}`);
  const s = Math.floor(Date.parse(m[1] + m[3]) / 1000);
  const n = Number((m[2] || '').padEnd(9, '0'));
  return { __t: 'timestamp', v: new Date(s * 1000 + Math.floor(n / 1e6)).toISOString(), s, n };
}

function timestampToRest(value) {
  let s;
  let n;
  if (typeof value.s === 'number') {
    s = value.s;
    n = value.n || 0;
  } else {
    // A backup from before nanoseconds were kept: milliseconds only.
    const ms = Date.parse(value.v);
    s = Math.floor(ms / 1000);
    n = (ms - s * 1000) * 1e6;
  }
  return `${new Date(s * 1000).toISOString().slice(0, 19)}.${String(n).padStart(9, '0')}Z`;
}

export function fromRestValue(value) {
  if ('nullValue' in value) return null;
  if ('booleanValue' in value) return value.booleanValue;
  if ('integerValue' in value) return Number(value.integerValue);
  if ('doubleValue' in value) return Number(value.doubleValue);
  if ('timestampValue' in value) return timestampFromRest(value.timestampValue);
  if ('stringValue' in value) return value.stringValue;
  if ('bytesValue' in value) return { __t: 'bytes', v: value.bytesValue };
  if ('referenceValue' in value) {
    return { __t: 'ref', v: value.referenceValue.replace(/^projects\/[^/]+\/databases\/[^/]+\/documents\//, '') };
  }
  if ('geoPointValue' in value) {
    return { __t: 'geopoint', v: [value.geoPointValue.latitude || 0, value.geoPointValue.longitude || 0] };
  }
  if ('arrayValue' in value) return (value.arrayValue.values || []).map(fromRestValue);
  if ('mapValue' in value) return fromRestFields(value.mapValue.fields);
  throw new Error(`Unknown Firestore value type: ${Object.keys(value).join(', ')}`);
}

export function fromRestFields(fields = {}) {
  const out = {};
  for (const key of Object.keys(fields)) out[key] = fromRestValue(fields[key]);
  return out;
}

export function toRestValue(value, projectId) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') {
    // As the Admin SDK encodes a number: a safe integer as an integer, anything else as a double.
    return Number.isSafeInteger(value) && !Object.is(value, -0) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (typeof value === 'string') return { stringValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map((v) => toRestValue(v, projectId)) } };
  if (typeof value === 'object') {
    if (value.__t === 'timestamp') return { timestampValue: timestampToRest(value) };
    if (value.__t === 'bytes') return { bytesValue: value.v };
    if (value.__t === 'ref') return { referenceValue: `projects/${projectId}/databases/(default)/documents/${value.v}` };
    if (value.__t === 'geopoint') return { geoPointValue: { latitude: value.v[0], longitude: value.v[1] } };
    return { mapValue: { fields: toRestFields(value, projectId) } };
  }
  throw new Error(`Cannot store a value of type ${typeof value}`);
}

export function toRestFields(data, projectId) {
  const fields = {};
  for (const key of Object.keys(data)) {
    if (data[key] !== undefined) fields[key] = toRestValue(data[key], projectId);
  }
  return fields;
}

// ── Client ──────────────────────────────────────────────────────────────────────────────────────

export function isQuotaError(err) {
  return Boolean(err) && err.status === 'RESOURCE_EXHAUSTED';
}

function isTooBig(err) {
  return Boolean(err) && (err.httpStatus === 413 ||
    (err.status === 'INVALID_ARGUMENT' && /too big|too large|exceeds/i.test(err.message || '')));
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * A Firestore REST client for one project. `host` and a getToken that returns 'owner' point it at
 * the emulator for tests. `reads` counts documents read, near enough to report against the quota.
 */
export function firestoreRest({ projectId, getToken, fetchImpl = fetch, host = 'https://firestore.googleapis.com', retryDelayMs = 500 }) {
  const root = `projects/${projectId}/databases/(default)/documents`;
  const base = `${host}/v1/${root}`;
  const encodePath = (path) => path.split('/').map(encodeURIComponent).join('/');
  const pathOf = (name) => name.slice(root.length + 1);
  const client = { projectId, reads: 0 };

  async function call(url, init = {}) {
    for (let attempt = 0; ; attempt++) {
      let res;
      try {
        res = await fetchImpl(url, {
          ...init,
          headers: { Authorization: `Bearer ${await getToken()}`, 'Content-Type': 'application/json' }
        });
      } catch (networkError) {
        if (attempt < 4) { await wait(retryDelayMs * 2 ** attempt); continue; }
        throw new Error('Lost the connection to Firestore. Check the internet connection and try again.');
      }
      if (res.ok) return res.json();
      let detail = {};
      try { detail = (await res.json()).error || {}; } catch { /* not JSON */ }
      const err = new Error(detail.message || `Firestore answered ${res.status}.`);
      err.status = detail.status || '';
      err.httpStatus = res.status;
      // Brief overloads pass; a used-up daily quota does not, and retrying it only wastes time.
      const transient = [500, 502, 503, 504].includes(res.status) || (res.status === 429 && !/quota/i.test(err.message));
      if (transient && attempt < 4) { await wait(retryDelayMs * 2 ** attempt); continue; }
      if (isQuotaError(err)) {
        err.message = "Firestore refused: today's free quota is used up (Quota exceeded). It resets at 3 PM Philippine time.";
      } else if (res.status === 403) {
        err.message = `This key may not use Firestore (${detail.message || 'permission denied'}).`;
      }
      throw err;
    }
  }

  client.listCollectionIds = async (parentPath) => {
    const url = `${base}${parentPath ? '/' + encodePath(parentPath) : ''}:listCollectionIds`;
    const ids = [];
    let pageToken = '';
    do {
      const body = await call(url, { method: 'POST', body: JSON.stringify({ pageSize: PAGE_SIZE, pageToken }) });
      ids.push(...(body.collectionIds || []));
      pageToken = body.nextPageToken || '';
    } while (pageToken);
    return ids;
  };

  client.listDocuments = async (collectionPath, { showMissing = false } = {}) => {
    const docs = [];
    let pageToken = '';
    do {
      const params = new URLSearchParams({ pageSize: String(PAGE_SIZE) });
      if (showMissing) params.set('showMissing', 'true');
      if (pageToken) params.set('pageToken', pageToken);
      const body = await call(`${base}/${encodePath(collectionPath)}?${params}`);
      const page = body.documents || [];
      client.reads += Math.max(page.length, 1);
      // A missing document comes back with its name only: no createTime, no fields.
      for (const d of page) docs.push({ path: pathOf(d.name), exists: Boolean(d.createTime), data: d.createTime ? fromRestFields(d.fields) : null });
      pageToken = body.nextPageToken || '';
    } while (pageToken);
    return docs;
  };

  /** Map of path -> data for those of `paths` that exist now. */
  client.getDocuments = async (paths) => {
    const found = new Map();
    for (let i = 0; i < paths.length; i += 100) {
      const chunk = paths.slice(i, i + 100);
      const body = await call(`${base}:batchGet`, { method: 'POST', body: JSON.stringify({ documents: chunk.map((p) => `${root}/${p}`) }) });
      client.reads += chunk.length;
      for (const item of body) if (item.found) found.set(pathOf(item.found.name), fromRestFields(item.found.fields));
    }
    return found;
  };

  // A commit is all-or-nothing, and Firestore also refuses one with too many index entries
  // ("Transaction too big"), which no size predicts: every key of a progress document's `entries`
  // map is indexed. A refused batch is split in half and retried, down to single documents.
  async function commitSplitting(writes) {
    try {
      await call(`${base}:commit`, { method: 'POST', body: `{"writes":[${writes.map((w) => w.json).join(',')}]}` });
    } catch (err) {
      if (!isTooBig(err) || writes.length === 1) throw err;
      const mid = Math.ceil(writes.length / 2);
      await commitSplitting(writes.slice(0, mid));
      await commitSplitting(writes.slice(mid));
    }
  }

  /** Sets each { path, data } (replacing the whole document); onWritten(count so far). */
  client.writeDocuments = async (entries, onWritten = () => {}) => {
    const encoder = new TextEncoder();
    let pending = [];
    let bytes = 0;
    let done = 0;
    const flush = async () => {
      await commitSplitting(pending);
      done += pending.length;
      onWritten(done);
      pending = [];
      bytes = 0;
    };
    for (const entry of entries) {
      const json = JSON.stringify({ update: { name: `${root}/${entry.path}`, fields: toRestFields(entry.data, projectId) } });
      const size = encoder.encode(json).length;
      if (pending.length > 0 && (pending.length >= BATCH_MAX_DOCS || bytes + size > BATCH_MAX_BYTES)) await flush();
      pending.push({ json });
      bytes += size;
    }
    if (pending.length > 0) await flush();
    return done;
  };

  return client;
}
