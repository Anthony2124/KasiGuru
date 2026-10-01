/// <reference types="vitest/config" />
import { defineConfig, type Plugin } from 'vite';
import preact from '@preact/preset-vite';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

/**
 * Emits /sw.js with the exact list of files this build produced, so the service worker can precache
 * the whole app shell. Art (Jepjep, avatars, scenes) is left to runtime caching: it is most of the
 * weight, and a learner should not download every pose before the first lesson.
 */
function serviceWorker(): Plugin {
  return {
    name: 'kasiguru-sw',
    apply: 'build',
    generateBundle(_, bundle) {
      const built = Object.keys(bundle).filter((f) => !f.endsWith('.map'));
      const publicDir = path.resolve(__dirname, 'public');
      const statics = ['manifest.webmanifest', 'content/vocabulary.json', 'content/stories.json', 'icons/icon-192.png', 'icons/apple-touch-icon.png', 'icons/wordmark.svg']
        .concat(fs.readdirSync(path.join(publicDir, 'fonts')).map((f) => `fonts/${f}`))
        .filter((f) => fs.existsSync(path.join(publicDir, f)));
      const files = ['/', '/index.html', ...[...built, ...statics].filter((f) => f !== 'index.html').map((f) => `/${f}`)];
      const version = crypto.createHash('sha1').update(files.join('|')).digest('hex').slice(0, 10);
      const template = fs.readFileSync(path.resolve(__dirname, 'src/sw.template.js'), 'utf8');
      this.emitFile({
        type: 'asset',
        fileName: 'sw.js',
        source: template.replace('__VERSION__', version).replace('__PRECACHE__', JSON.stringify(files)),
      });
    },
  };
}

export default defineConfig({
  plugins: [preact(), serviceWorker()],
  build: {
    target: 'es2020',
    assetsDir: 'assets',
    sourcemap: false,
    // Firebase changes far less often than the app, so it gets its own long-cached file: a release
    // of new screens should not make every learner download the SDK again.
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/firebase') || id.includes('node_modules/@firebase')) return 'firebase';
          if (id.includes('node_modules/preact') || id.includes('node_modules/idb-keyval')) return 'vendor';
        },
      },
    },
    chunkSizeWarningLimit: 400,
  },
  test: {
    include: ['tests/**/*.test.ts'],
  },
});
