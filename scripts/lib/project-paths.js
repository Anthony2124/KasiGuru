'use strict';

const path = require('node:path');
const os = require('node:os');

// Resolve from this module so tools work regardless of the caller's directory.
const PROJECT_ROOT = path.resolve(__dirname, '..', '..');
const DATABASE_SEEDER = path.join(
  PROJECT_ROOT, 'app', 'src', 'main', 'java', 'com', 'kasiguru',
  'data', 'local', 'DatabaseSeeder.kt',
);
const dictionaryData = (filename) => path.join(PROJECT_ROOT, 'data', 'dictionary', filename);
const downloadsFile = (filename) => path.join(os.homedir(), 'Downloads', filename);
const audioData = (filename) => path.join(PROJECT_ROOT, 'data', 'audio', filename);
const RAW_RESOURCES = path.join(PROJECT_ROOT, 'app', 'src', 'main', 'res', 'raw');
// Pronunciation clips shipped inside the APK, copied to the web app by scripts/web/sync-from-app.js.
const WORD_AUDIO_ASSETS = path.join(PROJECT_ROOT, 'app', 'src', 'main', 'assets', 'word_audio');
// Credentials live in the workspace's private folder, outside the source tree.
const privateFile = (filename) => path.join(PROJECT_ROOT, '..', 'private', filename);

module.exports = {
  PROJECT_ROOT, DATABASE_SEEDER, RAW_RESOURCES, WORD_AUDIO_ASSETS, dictionaryData, audioData, downloadsFile, privateFile,
};
