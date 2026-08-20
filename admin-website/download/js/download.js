// download.js — Read-only public download page logic for KasiGuru
// IMPORTANT: This file has ZERO write operations.
// It reads app_releases to keep the download button and QR code up to date.
//
// There is deliberately no hardcoded fallback release here. Firestore's
// `app_releases` collection is the single source of truth for what "latest"
// means — a hardcoded version string here would be a second source of truth
// that someone has to remember to update by hand on every release, and the
// two *will* eventually disagree. Until Firestore resolves, the page shows an
// honest "checking…" state instead of a guess.
import { db, collection, query, orderBy, onSnapshot, getCountFromServer } from './firebase-config.js';

// Apply release metadata to DOM
function applyReleaseInfo(release) {
  if (!release) return;

  // 1. Update main download button
  const mainBtn = document.getElementById('store-download-btn');
  if (mainBtn && release.apkUrl) mainBtn.setAttribute('href', release.apkUrl);

  // 2. Update navigation "Get App" button
  const navBtn = document.querySelector('.store-link-btn');
  if (navBtn && release.apkUrl) navBtn.setAttribute('href', release.apkUrl);

  // 3. Update version tag text
  const tag = document.getElementById('store-version-tag');
  if (tag) {
    tag.textContent = `Version ${release.versionName} (Build ${release.versionCode}) • Free & Safe APK`;
  }

  // 4. Update hero CTA button text & href
  const heroBtnSpan = document.querySelector('.hero-cta-group .download-btn-large span');
  if (heroBtnSpan) {
    heroBtnSpan.textContent = `Download APK (v${release.versionName})`;
  }
  const heroBtn = document.querySelector('.hero-cta-group .download-btn-large');
  if (heroBtn && release.apkUrl) {
    heroBtn.setAttribute('href', release.apkUrl);
  }

  // 5. Render QR Code
  renderQR(release.apkUrl);
}

// Live dictionary size: read-only count query, rounded down to the nearest 10.
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
      const href = anchor.getAttribute('href');
      if (!href || !href.startsWith('#') || href.length <= 1) return;
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        target.scrollIntoView({ behavior: 'smooth' });
      }
    });
  });
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

// ── Read-only: listens to app_releases collection ──
function showCheckFailedState() {
  const tag = document.getElementById('store-version-tag');
  if (tag) tag.textContent = "Couldn't check for the latest version — please try again shortly.";
}

try {
  const releaseQuery = query(
    collection(db, 'app_releases'),
    orderBy('versionCode', 'desc')
  );

  onSnapshot(releaseQuery, (snapshot) => {
    const releases = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
    const latest = releases[0];
    if (latest) {
      applyReleaseInfo(latest);
    } else {
      showCheckFailedState();
    }
  }, (error) => {
    console.warn('Release listener error:', error);
    showCheckFailedState();
  });
} catch (e) {
  console.error('Firestore release query error:', e);
  showCheckFailedState();
}
