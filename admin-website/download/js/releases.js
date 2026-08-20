import { db, collection, query, orderBy, onSnapshot } from './firebase-config.js';

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

try {
  const q = query(collection(db, 'app_releases'), orderBy('versionCode', 'desc'));
  onSnapshot(q, (snapshot) => {
    const container = document.getElementById('releases-container');
    if (!container) return;
    const releases = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

    if (releases.length === 0) {
      container.innerHTML = '<div class="state"><p class="state__title">No releases yet</p><p class="state__body">Check back soon for the first release.</p></div>';
      return;
    }

    container.innerHTML = releases.map(rel => {
      const date = rel.releasedAt ? new Date(rel.releasedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Unknown date';
      const notes = rel.releaseNotes || rel.notes || 'No release notes.';
      const isForce = rel.forceUpdate ? '<span class="tag-chip" style="background:var(--gold); color:var(--reward-ink);">Required Update</span>' : '';
      return `
        <div class="soft-card release-entry">
          <div class="release-header">
            <div>
              <h2 class="t-headline-sm">v${escapeHtml(rel.versionName)}</h2>
              <p class="t-body-sm" style="color:var(--muted);">${date} · Build ${rel.versionCode} ${isForce}</p>
            </div>
            ${rel.apkUrl ? `<a href="${escapeHtml(rel.apkUrl)}" class="clay clay--quiet">Download APK</a>` : '<span class="t-body-sm" style="color:var(--faint);">Download unavailable</span>'}
          </div>
          <p class="t-body-md" style="margin-top:var(--space-sm); color:var(--muted);">${escapeHtml(notes)}</p>
        </div>`;
    }).join('');
  }, (error) => {
    console.warn('Release listener error:', error);
    const container = document.getElementById('releases-container');
    if (container) container.innerHTML = '<div class="state state--error"><p class="state__title">Couldn\'t load releases</p><p class="state__body">Please try again shortly.</p></div>';
  });
} catch (e) {
  console.error('Releases query error:', e);
}
