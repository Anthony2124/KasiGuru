/**
 * Firestore backup export that works on the FREE (Spark) plan.
 *
 * Runs from your machine (or any machine with Node) using a service-account
 * key — no Blaze plan, no Cloud Functions, no Secret Manager required.
 *
 * Usage:
 *   node backup_firestore.js <path-to-service-account.json> [bucket-name]
 *
 *   bucket-name defaults to kasiguru-86042-backups (or $env:BACKUP_BUCKET).
 *
 * Prerequisites (one-time):
 *   1. Create the GCS bucket, e.g. kasiguru-86042-backups
 *      (gsutil mb -l asia-east1 gs://kasiguru-86042-backups)
 *   2. Grant the service account:
 *        - role "Cloud Datastore Import Export Admin" (project level)
 *        - role "Storage Object Admin" on the bucket
 *   3. Schedule it (Windows): Task Scheduler -> create task -> daily ->
 *      program: node, arguments: backup_firestore.js <key.json>, start in:
 *      C:\KasiGuru\KasiGuru-main\functions
 *
 * The service-account JSON must NOT be committed to the repo.
 */

const fs = require('fs');
const { GoogleAuth } = require('google-auth-library');

const keyFile = process.argv[2];
const bucket = process.argv[3] || process.env.BACKUP_BUCKET || 'kasiguru-86042-backups';

if (!keyFile) {
  console.error('Usage: node backup_firestore.js <path-to-service-account.json> [bucket-name]');
  process.exit(1);
}

(async () => {
  const key = JSON.parse(fs.readFileSync(keyFile, 'utf8'));
  const projectId = key.project_id;
  if (!projectId) {
    console.error('Failed: service-account file has no project_id');
    process.exit(1);
  }

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const outputUriPrefix = `gs://${bucket}/${timestamp}`;

  const auth = new GoogleAuth({
    keyFile,
    scopes: ['https://www.googleapis.com/auth/datastore']
  });
  const client = await auth.getClient();

  const res = await client.request({
    url: `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default):exportDocuments`,
    method: 'POST',
    data: {
      outputUriPrefix,
      collectionIds: [] // export all collections
    }
  });

  console.log(`Export started for ${projectId} -> ${outputUriPrefix}`);
  console.log(`Operation: ${res.data.name}`);
  console.log('Check completion: gcloud firestore operations describe ' + res.data.name);
})().catch((err) => {
  console.error('Export failed:', err.message);
  if (err.response && err.response.data && err.response.data.error) {
    console.error(JSON.stringify(err.response.data.error));
  }
  process.exit(1);
});
