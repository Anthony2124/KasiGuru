/**
 * One-off backfill for `vocabulary` and `stories`: gives every document a numeric
 * `updatedAt` so the app's incremental content sync can see it.
 *
 * FirestoreSyncManager used to read both collections in full on every pull — roughly
 * 400 document reads a time against the Spark plan's 50,000/day, a ceiling shared
 * project-wide by every user. It now queries whereGreaterThan("updatedAt", lastSync)
 * instead, which only works for documents that carry the field.
 *
 * Two shapes need repairing:
 *
 *  1. `vocabulary` documents written before the admin dashboard started stamping
 *     updatedAt have no such field at all. A missing field does not match a range
 *     query, so those words would never reach a user through the incremental path.
 *
 *  2. `stories` documents store updatedAt as an ISO 8601 *string*
 *     (new Date().toISOString()). Firestore orders values by type and never returns a
 *     string as greater-than a number, so those never match either. They are converted
 *     to epoch millis, preserving the instant the string recorded rather than stamping
 *     "now" — the timestamp is a real edit time and pretending everything changed today
 *     would make every client re-download every story on its next sync.
 *
 * The app's weekly full reconcile would eventually pick these up regardless, so this is
 * a repair that shortens the window rather than a correctness prerequisite. Running it
 * means edits show up on the next sync instead of within a week.
 *
 * Not needed going forward: every write path in admin/js/app.js goes through
 * withUpdatedAt(), and migrate.html stamps its own updates.
 *
 * Usage (from the functions/ directory):
 *   node backfill_content_updatedat.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only reports what it would change.
 */

const admin = require('firebase-admin');
const path = require('path');

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node backfill_content_updatedat.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyPath))
});

const db = admin.firestore();

// Firestore caps a batch at 500 writes.
const BATCH_LIMIT = 400;

/**
 * Decides the new value for one document, or null to leave it alone.
 *
 * Anything already numeric is correct and is skipped, so this is safe to re-run.
 */
function resolveUpdatedAt(data, fallbackMillis) {
  const current = data.updatedAt;

  if (typeof current === 'number' && Number.isFinite(current) && current > 0) {
    return null;
  }

  if (typeof current === 'string' && current.trim() !== '') {
    const parsed = Date.parse(current);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
    // An unparseable string is worse than nothing, since it will never match a range
    // query and never be retried. Fall through to the fallback rather than keep it.
  }

  // A Firestore Timestamp, if any path ever wrote one.
  if (current && typeof current.toMillis === 'function') {
    return current.toMillis();
  }

  return fallbackMillis;
}

async function backfillCollection(name, fallbackMillis) {
  const snapshot = await db.collection(name).get();
  console.log(`\n${name}: ${snapshot.size} document(s)`);

  const changes = [];
  snapshot.forEach((docSnap) => {
    const next = resolveUpdatedAt(docSnap.data(), fallbackMillis);
    if (next !== null) {
      changes.push({ id: docSnap.id, from: docSnap.data().updatedAt, to: next });
    }
  });

  if (changes.length === 0) {
    console.log(`  nothing to change — every document already has a numeric updatedAt`);
    return 0;
  }

  console.log(`  ${changes.length} document(s) need updatedAt`);
  changes.slice(0, 5).forEach((c) => {
    console.log(`    ${c.id}: ${JSON.stringify(c.from)} -> ${c.to}`);
  });
  if (changes.length > 5) {
    console.log(`    ... and ${changes.length - 5} more`);
  }

  if (!apply) return changes.length;

  for (let i = 0; i < changes.length; i += BATCH_LIMIT) {
    const slice = changes.slice(i, i + BATCH_LIMIT);
    const batch = db.batch();
    slice.forEach((c) => {
      batch.update(db.collection(name).doc(c.id), { updatedAt: c.to });
    });
    await batch.commit();
    console.log(`  committed ${Math.min(i + BATCH_LIMIT, changes.length)}/${changes.length}`);
  }

  return changes.length;
}

(async () => {
  // One instant for every document missing a timestamp entirely, so a single backfill
  // reads as a single event rather than smearing across the run's duration.
  const fallbackMillis = Date.now();

  console.log(apply ? 'APPLYING changes' : 'DRY RUN — pass --apply to write');

  const vocab = await backfillCollection('vocabulary', fallbackMillis);
  const stories = await backfillCollection('stories', fallbackMillis);

  console.log(`\n${apply ? 'Updated' : 'Would update'} ${vocab + stories} document(s).`);
  if (!apply) console.log('Re-run with --apply to write.');
})().catch((err) => {
  console.error('Backfill failed:', err);
  process.exit(1);
});
