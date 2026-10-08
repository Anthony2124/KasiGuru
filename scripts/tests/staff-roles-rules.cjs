// Local Firestore emulator only. Never contacts a production project.
// Admin vs verifier vs learner, against firestore.rules:
//   firebase emulators:exec --only firestore --project demo-kasiguru-ui "node scripts/tests/staff-roles-rules.cjs"
const assert = require('node:assert/strict');
const project = 'demo-kasiguru-ui';
const host = process.env.FIRESTORE_EMULATOR_HOST;
assert.ok(host && /^(127\.0\.0\.1|localhost):\d+$/.test(host), 'Start a local Firestore emulator first');
const base = `http://${host}/v1/projects/${project}/databases/(default)/documents`;
const encode = (value) => value && typeof value === 'object'
  ? { mapValue: { fields: Object.fromEntries(Object.entries(value).map(([k, v]) => [k, encode(v)])) } }
  : typeof value === 'number' ? { integerValue: String(value) } : typeof value === 'boolean' ? { booleanValue: value } : { stringValue: value };
// The emulator accepts unsigned test ID tokens; these are never valid outside the emulator.
const token = (uid, claims = {}) => [
  { alg: 'none', typ: 'JWT' },
  { sub: uid, user_id: uid, aud: project, iss: `https://securetoken.google.com/${project}`,
    iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600,
    firebase: { sign_in_provider: 'google.com' }, ...claims },
].map((v) => Buffer.from(JSON.stringify(v)).toString('base64url')).join('.') + '.';

const ADMIN = token('admin', { admin: true, email: 'boss@example.com', email_verified: true });
const VERIFIER = token('ver', { email: 'Ver@Example.com', email_verified: true });
const UNVERIFIED = token('ver2', { email: 'ver@example.com', email_verified: false });
const LEARNER = token('learner', { email: 'learner@example.com', email_verified: true });
const OWNER = 'owner'; // the emulator's rule bypass, for seeding

const request = (method, path, data, auth) => fetch(`${base}/${path}`, {
  method,
  headers: { 'content-type': 'application/json', ...(auth ? { Authorization: `Bearer ${auth}` } : {}) },
  ...(data ? { body: JSON.stringify({ fields: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, encode(v)])) }) } : {}),
});

(async () => {
  let checks = 0;
  const expect = async (label, method, path, data, auth, status) => {
    const res = await request(method, path, data, auth);
    const body = await res.text();
    assert.equal(res.status, status, `${label}: ${method} ${path} -> ${res.status} ${body}`);
    checks++;
  };
  const word = { kasiguranin: 'bale', english: 'house', updatedAt: 1 };

  await expect('seed a ban', 'PATCH', 'user_bans/learner', { isBanned: true, reason: 'test' }, OWNER, 200);

  await expect('admin adds a verifier', 'PATCH', 'admin_staff/ver@example.com', { role: 'verifier', email: 'ver@example.com', addedBy: 'boss@example.com', addedAt: 1 }, ADMIN, 200);
  await expect('the key must be the email', 'PATCH', 'admin_staff/other@example.com', { role: 'verifier', email: 'ver@example.com' }, ADMIN, 403);
  await expect('a verifier cannot add verifiers', 'PATCH', 'admin_staff/friend@example.com', { role: 'verifier', email: 'friend@example.com' }, VERIFIER, 403);
  await expect('a verifier reads their own entry', 'GET', 'admin_staff/ver@example.com', null, VERIFIER, 200);
  await expect('a learner cannot read the team', 'GET', 'admin_staff/ver@example.com', null, LEARNER, 403);
  const entry = { role: 'verifier', email: 'ver@example.com', addedBy: 'boss@example.com', addedAt: 1 };
  await expect('a verifier stamps their own last sign-in', 'PATCH', 'admin_staff/ver@example.com', { ...entry, lastSeenAt: 5 }, VERIFIER, 200);
  await expect('a verifier cannot change their role', 'PATCH', 'admin_staff/ver@example.com', { ...entry, role: 'admin', lastSeenAt: 6 }, VERIFIER, 403);
  await expect("a verifier cannot stamp someone else's entry", 'PATCH', 'admin_staff/boss@example.com', { role: 'verifier', email: 'boss@example.com', lastSeenAt: 7 }, VERIFIER, 403);

  await expect('a verifier edits the dictionary', 'PATCH', 'vocabulary/w1', word, VERIFIER, 200);
  await expect('a verifier writes the audit log', 'PATCH', 'admin_audit_log/a1', { action: 'test', timestamp: 1 }, VERIFIER, 200);
  await expect('a verifier reads a user', 'GET', 'users/learner', null, VERIFIER, 404);
  await expect('a verifier reads ban status', 'GET', 'user_bans/learner', null, VERIFIER, 200);
  await expect('a verifier cannot block', 'PATCH', 'user_bans/someone', { isBanned: true }, VERIFIER, 403);
  await expect('a verifier cannot unblock', 'DELETE', 'user_bans/learner', null, VERIFIER, 403);
  await expect('a verifier cannot publish a release', 'PATCH', 'app_releases/v9.9.9', { versionCode: 999 }, VERIFIER, 403);

  await expect('an unverified email is not a verifier', 'PATCH', 'vocabulary/w2', word, UNVERIFIED, 403);
  await expect('a learner cannot edit the dictionary', 'PATCH', 'vocabulary/w3', word, LEARNER, 403);
  await expect('a learner still reads their own ban', 'GET', 'user_bans/learner', null, LEARNER, 200);

  const sentence = { id: 's1', kasiguranin: 'bale', english: 'house', sentence: 'Mapiya i bale ya', translation: 'The house is nice.', contributorName: 'Kiko', status: 'pending', submittedAt: 1, uid: 'learner' };
  await expect('a learner sends an example sentence', 'PATCH', 'sentence_submissions/s1', sentence, LEARNER, 200);
  await expect('a sentence cannot arrive approved', 'PATCH', 'sentence_submissions/s2', { ...sentence, id: 's2', status: 'approved' }, LEARNER, 403);
  await expect('a sentence cannot claim another uid', 'PATCH', 'sentence_submissions/s3', { ...sentence, id: 's3', uid: 'someone' }, LEARNER, 403);
  await expect('a learner cannot approve their own sentence', 'PATCH', 'sentence_submissions/s1', { ...sentence, status: 'approved' }, LEARNER, 403);
  await expect('a learner reads their own sentence', 'GET', 'sentence_submissions/s1', null, LEARNER, 200);

  await expect('an admin publishes a release', 'PATCH', 'app_releases/v9.9.9', { versionCode: 999 }, ADMIN, 200);
  await expect('an admin blocks', 'PATCH', 'user_bans/someone', { isBanned: true }, ADMIN, 200);

  await expect('a verifier approves a sentence', 'PATCH', 'sentence_submissions/s1', { ...sentence, status: 'approved' }, VERIFIER, 200);

  await expect('admin removes the verifier', 'DELETE', 'admin_staff/ver@example.com', null, ADMIN, 200);
  await expect('a removed verifier loses access', 'PATCH', 'vocabulary/w4', word, VERIFIER, 403);

  console.log(`Staff roles rules: ${checks} checks passed.`);
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
