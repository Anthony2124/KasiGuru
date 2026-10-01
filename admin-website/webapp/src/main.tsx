import { render } from 'preact';
import './styles.css';
import { App } from './app';
import { initAuth } from './lib/auth';
import { loadContent, refreshContent } from './lib/content';
import { requestPersistence } from './lib/persist';
import { fetchAnnouncements } from './lib/remote';
import { act, loadLocal } from './lib/store';
import { initSync } from './lib/sync';

async function boot() {
  initSync();
  await Promise.all([loadLocal(), loadContent()]);
  // A streak that lapsed while the app was closed is reset before anything reads it, and progress
  // from before XP policy 2 is normalized once, now that the stories it may have opened are loaded.
  act(
    (d) => {
      d.validateStreak();
      d.ensureNormalized();
    },
    { celebrate: false }
  );
  initAuth();
  void refreshContent();
  void fetchAnnouncements();
  void requestPersistence();
}

render(<App />, document.getElementById('app')!);
boot().catch((e) => {
  console.error('boot failed', e);
  document.getElementById('app')!.innerHTML =
    '<div style="padding:32px;text-align:center;font-family:system-ui;color:#fff">KasiGuru could not start. Check your connection and reload.</div>';
});

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((e) => console.warn('service worker', e));
  });
}
