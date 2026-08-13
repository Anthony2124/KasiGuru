// download.js — Read-only public download page logic for KasiGuru
// IMPORTANT: This file has ZERO write operations.
// It only reads app_releases to keep the download button and QR code up to date.
import { db, collection, query, orderBy, onSnapshot, getCountFromServer } from './firebase-config.js';

// Live dictionary size: read-only count query, rounded down to the nearest 10.
// Falls back to the static numbers in index.html if the query fails.
try {
  getCountFromServer(collection(db, 'vocabulary'))
    .then((snap) => {
      const total = snap.data().count || 0;
      const rounded = Math.max(0, Math.floor(total / 10) * 10);
      const hero = document.getElementById('word-count-hero');
      const feat = document.getElementById('word-count-features');
      if (hero) hero.textContent = `${rounded}+ Kasiguranin words`;
      if (feat) feat.textContent = String(rounded);
    })
    .catch(() => { /* keep static fallback */ });
} catch (e) { /* keep static fallback */ }

// ── Smooth scroll for anchor links ──
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', (e) => {
      const target = document.querySelector(anchor.getAttribute('href'));
      if (target) {
        e.preventDefault();
        target.scrollIntoView({ behavior: 'smooth' });
      }
    });
  });

  // Initialize QR code with fallback placeholder URL
  renderQR('#');
});

function renderQR(url) {
  const canvas = document.getElementById('qr-canvas');
  if (!canvas || !window.qrcode || !url || url === '#') return;
  try {
    const ctx = canvas.getContext('2d');
    const size = Math.min(canvas.width, canvas.height);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    const qr = qrcode(0, 'M'); // type 0 = auto-size, medium error correction
    qr.addData(url);
    qr.make();

    const count = qr.getModuleCount();
    const cell = Math.floor(size / (count + 2)); // 1-module quiet zone
    const qrSize = cell * count;
    const offsetX = Math.floor((canvas.width - qrSize) / 2);
    const offsetY = Math.floor((canvas.height - qrSize) / 2);

    ctx.fillStyle = '#000000';
    for (let row = 0; row < count; row++) {
      for (let col = 0; col < count; col++) {
        if (qr.isDark(row, col)) {
          ctx.fillRect(offsetX + col * cell, offsetY + row * cell, cell, cell);
        }
      }
    }
  } catch (e) {
    console.warn('QR render error:', e);
  }
}

// ── Read-only: only listens to app_releases collection ──
try {
  const releaseQuery = query(
    collection(db, 'app_releases'),
    orderBy('versionCode', 'desc')
  );

  onSnapshot(releaseQuery, (snapshot) => {
    const releases = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
    const latest = releases[0];
    if (!latest) return;

    // Update main download button
    const mainBtn = document.getElementById('store-download-btn');
    if (mainBtn) mainBtn.setAttribute('href', latest.apkUrl || '#');

    // Update version tag text
    const tag = document.getElementById('store-version-tag');
    if (tag) {
      tag.textContent = `Version ${latest.versionName} (Build ${latest.versionCode}) • Free & Safe APK`;
    }

    // Update hero CTA button text
    const heroBtnSpan = document.querySelector('.download-btn-large span');
    if (heroBtnSpan) {
      heroBtnSpan.textContent = `Download APK (v${latest.versionName})`;
    }

    // Update hero CTA button href
    const heroBtn = document.querySelector('.hero-cta-group .download-btn-large');
    if (heroBtn) heroBtn.setAttribute('href', latest.apkUrl || '#download-section');

    // Re-render QR code with latest APK URL
    renderQR(latest.apkUrl);

  }, (error) => {
    console.warn('Release listener error:', error);
  });
} catch (e) {
  console.error('Firestore release query error:', e);
}
