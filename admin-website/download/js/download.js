// download.js — Read-only public download page logic for KasiGuru
// IMPORTANT: This file has ZERO write operations.
// It only reads app_releases to keep the download button and QR code up to date.
import { db, collection, query, orderBy, onSnapshot } from './firebase-config.js';

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
  if (canvas && window.QRCode && url && url !== '#') {
    try {
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      QRCode.toCanvas(canvas, url, { width: 180, margin: 1 }, (err) => {
        if (err) console.warn('QR generation error:', err);
      });
    } catch (e) {
      console.warn('QR render error:', e);
    }
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
