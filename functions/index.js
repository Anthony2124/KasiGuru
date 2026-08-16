/**
 * KasiGuru Cloud Functions
 * ------------------------
 * 1. bootstrapAdmin  – one-time, secret-protected call that grants the
 *                      `admin` custom claim to an existing Firebase Auth user.
 * 2. scheduledFirestoreBackup – nightly export of Firestore to Cloud Storage.
 *
 * Deployment: firebase deploy --only functions
 * Requires the Firebase project on the Blaze (pay-as-you-go) plan for
 * scheduled exports and the Datastore export permission on the service account.
 */

const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { onDocumentWritten } = require('firebase-functions/v2/firestore');
const { defineSecret } = require('firebase-functions/params');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { GoogleAuth } = require('google-auth-library');

initializeApp();

const BOOTSTRAP_SECRET = defineSecret('BOOTSTRAP_ADMIN_SECRET');

/**
 * Grants the `admin` custom claim to a Firebase Auth user.
 *
 * Call once per admin account, e.g. from a local machine:
 *   curl -X POST https://<region>-<project>.cloudfunctions.net/bootstrapAdmin \
 *     -H "Content-Type: application/json" \
 *     -d '{"data":{"email":"admin@example.com","bootstrapSecret":"<secret>"}}'
 *
 * The secret is stored as a Firebase secret (firebase functions:secrets:set BOOTSTRAP_ADMIN_SECRET)
 * and never shipped to clients. The function itself enforces the claim only
 * after the secret matches, so anonymous callers cannot grant themselves access.
 */
exports.bootstrapAdmin = onCall(
  { secrets: [BOOTSTRAP_SECRET] },
  async (request) => {
    const { email, bootstrapSecret } = request.data || {};

    if (!email || typeof email !== 'string') {
      throw new HttpsError('invalid-argument', 'email is required');
    }
    if (!bootstrapSecret || bootstrapSecret !== BOOTSTRAP_SECRET.value()) {
      throw new HttpsError('permission-denied', 'invalid bootstrap secret');
    }

    try {
      const user = await getAuth().getUserByEmail(email.trim().toLowerCase());
      await getAuth().setCustomUserClaims(user.uid, { admin: true });
      return { ok: true, uid: user.uid, email: user.email };
    } catch (err) {
      if (err.code === 'auth/user-not-found') {
        throw new HttpsError('not-found', 'no Firebase Auth user with that email');
      }
      throw new HttpsError('internal', 'failed to set admin claim');
    }
  }
);

/** Largest XP gain a single progress write may contribute to the leaderboard. */
const MAX_XP_DELTA_PER_WRITE = 2000;

/**
 * Maintains the public leaderboard from each user's own progress document.
 *
 * Clients can only write their own users/{uid}/progress/main; `leaderboard_public`
 * is closed to them (see firestore.rules), so rankings always come from data the
 * database actually holds rather than from whatever a client claims.
 *
 * Only the fields needed to render a row are copied out — never email, address,
 * age or any other profile detail.
 */
exports.syncLeaderboardEntry = onDocumentWritten(
  'users/{uid}/progress/main',
  async (event) => {
    const uid = event.params.uid;
    const db = getFirestore();
    const entryRef = db.collection('leaderboard_public').doc(uid);

    const after = event.data?.after?.data();
    // Progress deleted (account removal): drop the row rather than leave a ghost.
    if (!after) {
      await entryRef.delete().catch(() => {});
      return;
    }

    const before = event.data?.before?.data() || {};
    const previousXp = Number(before.totalXp) || 0;
    const claimedXp = Number(after.totalXp) || 0;

    // Reject impossible jumps instead of publishing them. The write to the user's
    // own document still stands; only its leaderboard projection is clamped, so a
    // sync glitch can never mint a #1 rank. Not cryptographic anti-cheat: XP is
    // still computed on-device.
    const existing = await entryRef.get();
    const publishedXp = existing.exists ? Number(existing.data().totalXp) || 0 : 0;
    const delta = claimedXp - previousXp;
    const isFirstPublish = !existing.exists;
    const acceptedXp =
      isFirstPublish || delta <= MAX_XP_DELTA_PER_WRITE
        ? claimedXp
        : Math.max(publishedXp, previousXp);

    if (!isFirstPublish && delta > MAX_XP_DELTA_PER_WRITE) {
      console.warn(
        `leaderboard: clamped suspicious XP jump for ${uid}: ${previousXp} -> ${claimedXp}`
      );
    }

    const displayName =
      (after.fullName || '').trim() || (after.userName || '').trim() || 'Learner';

    await entryRef.set(
      {
        displayName: displayName.slice(0, 40),
        totalXp: acceptedXp,
        level: Number(after.level) || 1,
        currentStreak: Number(after.currentStreak) || 0,
        profileIconId: Number(after.profileIconId) || 1,
        titleBadge: after.titleBadge || 'Kasiguranin Apprentice',
        // Weekly board: accumulate the accepted gain, reset by resetWeeklyLeaderboard.
        weeklyXp: FieldValue.increment(
          isFirstPublish ? 0 : Math.max(0, Math.min(delta, MAX_XP_DELTA_PER_WRITE))
        ),
        updatedAt: Date.now()
      },
      { merge: true }
    );
  }
);

/** Starts a fresh weekly board every Monday. */
exports.resetWeeklyLeaderboard = onSchedule(
  {
    schedule: 'every monday 00:05',
    timeZone: 'Asia/Manila',
    memory: '256MiB',
    timeoutSeconds: 540
  },
  async () => {
    const db = getFirestore();
    const snapshot = await db.collection('leaderboard_public').get();

    let batch = db.batch();
    let pending = 0;
    for (const doc of snapshot.docs) {
      batch.set(doc.ref, { weeklyXp: 0 }, { merge: true });
      pending += 1;
      // Firestore caps a batch at 500 writes.
      if (pending === 450) {
        await batch.commit();
        batch = db.batch();
        pending = 0;
      }
    }
    if (pending > 0) await batch.commit();

    return { reset: snapshot.size };
  }
);

/**
 * Nightly Firestore export to Cloud Storage.
 *
 * Prerequisites:
 *  - Firebase project on the Blaze plan (scheduled functions require billing).
 *  - A GCS bucket (e.g. kasiguru-86042-backups) with a lifecycle rule to
 *    expire exports after N days (e.g. 30) to control storage cost.
 *  - The project's default compute/service account has the Datastore Import
 *    Export Admin role and Storage Object Admin on the backup bucket.
 *
 * Manual alternative (works on any plan):
 *   gcloud firestore export gs://kasiguru-86042-backups/manual-$(date +%F)
 */
exports.scheduledFirestoreBackup = onSchedule(
  {
    schedule: 'every day 02:00',
    timeZone: 'Asia/Shanghai',
    memory: '256MiB',
    timeoutSeconds: 540
  },
  async () => {
    const projectId = process.env.GCLOUD_PROJECT;
    const bucketName = process.env.BACKUP_BUCKET || `${projectId}-backups`;
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const outputUriPrefix = `gs://${bucketName}/${timestamp}`;

    const auth = new GoogleAuth({
      scopes: ['https://www.googleapis.com/auth/datastore']
    });
    const client = await auth.getClient();

    await client.request({
      url: `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default):exportDocuments`,
      method: 'POST',
      data: {
        outputUriPrefix,
        collectionIds: [] // export all collections
      }
    });

    return { exportedTo: outputUriPrefix };
  }
);
