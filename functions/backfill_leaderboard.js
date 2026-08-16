/**
 * One-off backfill: publishes a leaderboard row for every user who already has
 * progress saved in Firestore.
 *
 * The syncLeaderboardEntry Cloud Function only fires when a progress document is
 * written, so users who existed before it was deployed stay invisible on the
 * leaderboard until their app next syncs. This walks the existing documents once
 * and publishes them.
 *
 * Read-only with respect to user data: it reads users/{uid}/progress/main and
 * writes only leaderboard_public/{uid}. No progress is modified or deleted.
 *
 * Usage (from the functions/ directory):
 *   node backfill_leaderboard.js <path-to-service-account.json> [--apply]
 *
 * Without --apply it runs as a dry run and only reports what it would publish.
 */

const admin = require('firebase-admin');
const path = require('path');

const [keyPath, ...flags] = process.argv.slice(2);
const apply = flags.includes('--apply');

if (!keyPath) {
  console.error('Usage: node backfill_leaderboard.js <path-to-service-account.json> [--apply]');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(path.resolve(keyPath))
});

const db = admin.firestore();

async function main() {
  // collectionGroup finds every users/{uid}/progress/main without listing users.
  const snapshot = await db.collectionGroup('progress').get();
  const mainDocs = snapshot.docs.filter((doc) => doc.id === 'main');

  console.log(`Found ${mainDocs.length} user progress document(s).`);
  if (!apply) console.log('DRY RUN — pass --apply to publish.\n');

  let published = 0;
  let skipped = 0;
  let batch = db.batch();
  let pending = 0;

  for (const doc of mainDocs) {
    // users/{uid}/progress/main -> uid
    const uid = doc.ref.parent.parent.id;
    const data = doc.data() || {};
    const totalXp = Number(data.totalXp) || 0;
    const displayName =
      (data.fullName || '').trim() || (data.userName || '').trim() || 'Learner';

    // Nothing to rank yet; skip rather than fill the board with empty rows.
    if (totalXp <= 0) {
      skipped += 1;
      continue;
    }

    const entry = {
      displayName: displayName.slice(0, 40),
      totalXp,
      level: Number(data.level) || 1,
      currentStreak: Number(data.currentStreak) || 0,
      profileIconId: Number(data.profileIconId) || 1,
      titleBadge: data.titleBadge || 'Kasiguranin Apprentice',
      updatedAt: Number(data.updatedAt) || Date.now()
    };

    console.log(`  ${uid}  ${entry.displayName}  ${entry.totalXp} XP`);

    if (apply) {
      // merge keeps any weeklyXp the live trigger has already accumulated.
      batch.set(db.collection('leaderboard_public').doc(uid), entry, { merge: true });
      pending += 1;
      if (pending === 450) {
        await batch.commit();
        batch = db.batch();
        pending = 0;
      }
    }
    published += 1;
  }

  if (apply && pending > 0) await batch.commit();

  console.log(
    `\n${apply ? 'Published' : 'Would publish'} ${published} row(s); skipped ${skipped} with no XP.`
  );
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Backfill failed:', err.message);
    process.exit(1);
  });
