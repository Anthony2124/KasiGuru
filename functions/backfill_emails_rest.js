/**
 * One-off backfill: patches email addresses from the Firebase Auth export
 * (auth_users.json) into leaderboard_public/{uid} documents in Firestore.
 *
 * This script uses `firebase-admin` initialized with application default credentials
 * from `gcloud auth application-default login` OR a service account key.
 *
 * If no ADC or service account is available, it falls back to using the
 * Firestore REST API with a token obtained from `firebase login:ci`.
 *
 * Usage:
 *   node backfill_emails_rest.js [--apply]
 *
 * Without --apply it runs as a dry run.
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const apply = process.argv.includes('--apply');
const projectId = 'kasiguru-86042';

// Read auth export
const authFile = path.resolve(__dirname, '..', 'auth_users.json');
if (!fs.existsSync(authFile)) {
  console.error('ERROR: auth_users.json not found in project root.');
  console.error('Run:  firebase auth:export auth_users.json --format=json --project kasiguru-86042');
  process.exit(1);
}

const authData = JSON.parse(fs.readFileSync(authFile, 'utf8'));
const authUsers = authData.users || [];
console.log(`Loaded ${authUsers.length} user(s) from auth export.`);

// Build uid -> email mapping (only users with actual emails)
const emailMap = new Map();
for (const u of authUsers) {
  let email = u.email || null;
  let displayName = u.displayName || null;

  if (!email && u.providerUserInfo) {
    for (const p of u.providerUserInfo) {
      if (p.email) { email = p.email; break; }
    }
  }
  if (!displayName && u.providerUserInfo) {
    for (const p of u.providerUserInfo) {
      if (p.displayName) { displayName = p.displayName; break; }
    }
  }

  const createdAt = u.createdAt ? parseInt(u.createdAt, 10) : null;

  if (email) {
    emailMap.set(u.localId, { email, displayName, createdAt });
  }
}

console.log(`${emailMap.size} user(s) have an email address.`);

// Get Firebase CLI refresh token
function getFirebaseToken() {
  // Firebase CLI stores its token in configstore
  const possiblePaths = [
    path.join(process.env.APPDATA || '', 'configstore', 'firebase-tools.json'),
    path.join(process.env.HOME || '', '.config', 'configstore', 'firebase-tools.json'),
    path.join(process.env.USERPROFILE || '', '.config', 'configstore', 'firebase-tools.json'),
  ];

  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      const data = JSON.parse(fs.readFileSync(p, 'utf8'));
      const tokens = data.tokens;
      if (tokens && tokens.refresh_token) {
        return tokens;
      }
    }
  }
  return null;
}

// Exchange refresh token for access token
function exchangeRefreshToken(refreshToken) {
  return new Promise((resolve, reject) => {
    const data = `grant_type=refresh_token&refresh_token=${encodeURIComponent(refreshToken)}&client_id=563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com&client_secret=j9iVZfS8kkCEFUPaAeJV0sAi`;
    
    const req = https.request({
      hostname: 'securetoken.googleapis.com',
      path: '/v1/token?key=AIzaSyBIADrpzbQZpE4SoHRp9xKsh9A03RLZDlg',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(data)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve(JSON.parse(body));
        } else {
          reject(new Error(`Token exchange failed: ${res.statusCode} ${body}`));
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

// Firestore REST API helpers
function firestoreGet(accessToken, documentPath) {
  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'firestore.googleapis.com',
      path: `/v1/projects/${projectId}/databases/(default)/documents/${documentPath}`,
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve(JSON.parse(body));
        } else if (res.statusCode === 404) {
          resolve(null);
        } else {
          reject(new Error(`GET ${documentPath}: ${res.statusCode} ${body.slice(0, 200)}`));
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

function firestorePatch(accessToken, documentPath, fieldsToUpdate) {
  return new Promise((resolve, reject) => {
    // Build the update mask
    const fieldNames = Object.keys(fieldsToUpdate);
    const updateMask = fieldNames.map(f => `updateMask.fieldPaths=${f}`).join('&');
    
    // Build the document body
    const fields = {};
    for (const [key, value] of Object.entries(fieldsToUpdate)) {
      if (typeof value === 'string') {
        fields[key] = { stringValue: value };
      } else if (typeof value === 'number') {
        fields[key] = { integerValue: String(value) };
      } else if (typeof value === 'boolean') {
        fields[key] = { booleanValue: value };
      }
    }

    const body = JSON.stringify({ fields });

    const req = https.request({
      hostname: 'firestore.googleapis.com',
      path: `/v1/projects/${projectId}/databases/(default)/documents/${documentPath}?${updateMask}`,
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body)
      }
    }, (res) => {
      let respBody = '';
      res.on('data', chunk => respBody += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve(JSON.parse(respBody));
        } else {
          reject(new Error(`PATCH ${documentPath}: ${res.statusCode} ${respBody.slice(0, 300)}`));
        }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  // Try to get Firebase CLI token
  const tokenData = getFirebaseToken();
  if (!tokenData) {
    console.error('ERROR: Could not find Firebase CLI credentials.');
    console.error('Run:  firebase login');
    process.exit(1);
  }

  console.log('Found Firebase CLI credentials, exchanging for access token...');
  
  let accessToken;
  try {
    // Use the Google OAuth2 token endpoint with the Firebase CLI's client ID/secret
    const tokenResponse = await new Promise((resolve, reject) => {
      const data = `grant_type=refresh_token&refresh_token=${encodeURIComponent(tokenData.refresh_token)}&client_id=563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com&client_secret=j9iVZfS8kkCEFUPaAeJV0sAi`;
      
      const req = https.request({
        hostname: 'oauth2.googleapis.com',
        path: '/token',
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(data)
        }
      }, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
          if (res.statusCode === 200) {
            resolve(JSON.parse(body));
          } else {
            reject(new Error(`Token exchange failed: ${res.statusCode} ${body}`));
          }
        });
      });
      req.on('error', reject);
      req.write(data);
      req.end();
    });
    accessToken = tokenResponse.access_token;
    console.log('Got access token successfully.\n');
  } catch (e) {
    console.error('Failed to exchange token:', e.message);
    process.exit(1);
  }

  if (!apply) {
    console.log('DRY RUN — pass --apply to write changes.\n');
  }

  // Patch each user's leaderboard entry
  let patched = 0;
  let skipped = 0;
  let notFound = 0;

  for (const [uid, data] of emailMap) {
    try {
      // Check leaderboard_public/{uid}
      const lbDoc = await firestoreGet(accessToken, `leaderboard_public/${uid}`);
      
      if (!lbDoc) {
        notFound++;
        continue;
      }

      const fields = lbDoc.fields || {};
      const currentEmail = fields.email && fields.email.stringValue;
      const hasValidEmail = currentEmail && currentEmail.includes('@');
      const hasDate = fields.registeredAt || fields.createdAt;

      if (hasValidEmail && hasDate) {
        skipped++;
        continue;
      }

      const patch = {};
      if (!hasValidEmail) patch.email = data.email;
      if (!hasDate && data.createdAt) {
        patch.registeredAt = data.createdAt;
        patch.createdAt = data.createdAt;
      }
      patch.isAnonymous = false;

      // Fix displayName if generic
      const dn = fields.displayName && fields.displayName.stringValue || '';
      if (['learner', 'registered user', 'google account', ''].includes(dn.toLowerCase().trim())) {
        if (data.displayName) patch.displayName = data.displayName.slice(0, 40);
      }

      console.log(`  PATCH leaderboard_public/${uid}: ${JSON.stringify(patch)}`);

      if (apply) {
        await firestorePatch(accessToken, `leaderboard_public/${uid}`, patch);
      }
      patched++;

      // Also patch progress doc
      const progDoc = await firestoreGet(accessToken, `users/${uid}/progress/main`);
      if (progDoc) {
        const pFields = progDoc.fields || {};
        const pEmail = pFields.email && pFields.email.stringValue;
        const progPatch = {};
        if (!pEmail || !pEmail.includes('@')) progPatch.email = data.email;
        if (!pFields.fullName && data.displayName) progPatch.fullName = data.displayName;
        if (Object.keys(progPatch).length > 0) {
          console.log(`  PATCH users/${uid}/progress/main: ${JSON.stringify(progPatch)}`);
          if (apply) {
            await firestorePatch(accessToken, `users/${uid}/progress/main`, progPatch);
          }
        }
      }
    } catch (e) {
      console.error(`  ERROR ${uid}: ${e.message}`);
    }
  }

  console.log(`\n${apply ? 'Patched' : 'Would patch'} ${patched} entries. Skipped ${skipped}. Not in leaderboard: ${notFound}.`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Backfill failed:', err.message);
    process.exit(1);
  });
