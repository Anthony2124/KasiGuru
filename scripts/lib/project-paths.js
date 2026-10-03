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
// Credentials live in the workspace's private folder, outside the source tree.
const privateFile = (filename) => path.join(PROJECT_ROOT, '..', 'private', filename);

module.exports = {
  PROJECT_ROOT, DATABASE_SEEDER, RAW_RESOURCES, dictionaryData, audioData, downloadsFile, privateFile,
};
