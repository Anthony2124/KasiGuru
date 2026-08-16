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
const { defineSecret } = require('firebase-functions/params');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
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
