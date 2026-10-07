'use strict';

const { execSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

/** Reuse a local CLI sign-in without printing, accepting on argv, or saving access tokens. */
function googleAccessToken() {
  try {
    const token = execSync('gcloud auth print-access-token', {
      encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'], timeout: 60000,
    }).trim();
    if (token) return token;
  } catch { /* Firebase CLI can already be signed in on PCs without gcloud. */ }

  const file = path.join(process.env.XDG_CONFIG_HOME || path.join(os.homedir(), '.config'),
    'configstore', 'firebase-tools.json');
  const read = () => {
    const { tokens } = JSON.parse(fs.readFileSync(file, 'utf8'));
    return tokens?.expires_at > Date.now() + 60000 && typeof tokens.access_token === 'string'
      ? tokens.access_token : null;
  };
  try {
    let token = read();
    if (!token) {
      // Let Firebase's own CLI refresh its credentials. This requests project metadata, never documents.
      execSync('npx --yes firebase-tools projects:list --json --non-interactive', {
        stdio: ['ignore', 'pipe', 'ignore'], timeout: 60000,
      });
      token = read();
    }
    if (token) return token;
  } catch { /* Report the required sign-in, without leaking credential or CLI output. */ }
  throw new Error('No usable Google sign-in. Run gcloud auth login or npx --yes firebase-tools login.');
}

module.exports = { googleAccessToken };
