import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { APP_VERSION } from '../src/lib/version';

/**
 * The web app is the same app as the APK, so it carries the APK's version. It sat at "web 1.18.0"
 * through Android 1.25.0 because nothing compared the two.
 */
describe('version', () => {
  const gradle = fs.readFileSync(path.resolve(__dirname, '../../../app/build.gradle.kts'), 'utf8');
  const android = /versionName\s*=\s*"([^"]+)"/.exec(gradle)?.[1];
  const pkg = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../package.json'), 'utf8')).version;

  it('matches the Android versionName', () => {
    expect(android).toBeTruthy();
    expect(APP_VERSION).toBe(`web ${android}`);
  });
  it('package.json carries the same version', () => {
    expect(pkg).toBe(android);
  });
});
