/**
 * Sends an FCM push notification to all registered KasiGuru devices.
 * Works on the FREE (Spark) plan — no Cloud Functions, no Blaze.
 *
 * The app registers its FCM token in Firestore (device_tokens/{uid}); this
 * script reads those tokens and sends via the FCM v1 API using the
 * service-account key (same one used for backups).
 *
 * Usage:
 *   node send_push.js <service-account.json> "<title>" "<body>" [route] [category]
 *
 *   route examples: "" (home), "story/1", "vocabulary/12", "leaderboard"
 *   category: "Streak" | "WordOfDay" | "Leaderboard" | "Achievement" | "General"
 */

const fs = require('fs');
const admin = require('firebase-admin');
const { GoogleAuth } = require('google-auth-library');

const keyFile = process.argv[2];
const title = process.argv[3] || 'KasiGuru';
const body = process.argv[4] || '';
const route = process.argv[5] || '';
const category = process.argv[6] || 'General';

if (!keyFile || !fs.existsSync(keyFile)) {
  console.error('Usage: node send_push.js <service-account.json> "<title>" "<body>" [route] [category]');
  process.exit(1);
}

const key = JSON.parse(fs.readFileSync(keyFile, 'utf8'));
const projectId = key.project_id;

admin.initializeApp({ credential: admin.credential.cert(keyFile) });

(async () => {
  const snapshot = await admin.firestore().collection('device_tokens').get();
  const tokens = snapshot.docs
    .map((d) => d.data().token)
    .filter((t) => typeof t === 'string' && t.length > 0);

  if (tokens.length === 0) {
    console.error('No registered devices found in device_tokens.');
    console.error('Open the app once (with FCM enabled) so it registers its token.');
    process.exit(1);
  }

  const auth = new GoogleAuth({
    keyFile,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging']
  });
  const client = await auth.getClient();

  let sent = 0;
  let failed = 0;
  for (const token of tokens) {
    try {
      await client.request({
        url: `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
        method: 'POST',
        data: {
          message: {
            token,
            notification: { title, body },
            data: { route, category, title, message: body },
            android: { priority: 'HIGH' }
          }
        }
      });
      sent++;
    } catch (err) {
      failed++;
      console.warn('Send failed for one device:', err.message);
    }
  }

  console.log(`Sent to ${sent} device(s), ${failed} failed.`);
  if (failed > 0) process.exitCode = 1;
})().catch((err) => {
  console.error('Error:', err.message);
  process.exit(1);
});
