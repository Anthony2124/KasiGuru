// app.js — KasiGuru Admin Dashboard Logic (Auth-Guarded)
// This file is loaded ONLY by dashboard.html, after Firebase Auth confirms the user is logged in.

import { 
  db, auth,
  collection, doc, getDoc, getDocs, setDoc, addDoc, updateDoc, deleteDoc,
  query, orderBy, where, onSnapshot, Bytes, writeBatch
} from './firebase-config.js';
import { 
  onAuthStateChanged, signOut 
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

/**
 * Stamps a content payload with the millisecond timestamp the app syncs against.
 *
 * FirestoreSyncManager queries `vocabulary` and `stories` with
 * whereGreaterThan("updatedAt", lastSync) instead of reading the whole collection on
 * every pull, so a document written without this field is invisible to that query and
 * simply never reaches users. Its weekly full reconcile is the backstop, but that means
 * a missed stamp shows up as "my edit took a week to appear", which is a miserable thing
 * to debug.
 *
 * Every write to those two collections goes through here so there is one place to get it
 * right rather than eight. Milliseconds, not serverTimestamp(), to match the numeric
 * comparison the Android query does and the `updatedAt` already stored elsewhere.
 */
function withUpdatedAt(payload) {
  return { ...payload, updatedAt: Date.now() };
}

// Global state
let submissions = [];
let literatureSubmissions = [];
let literatureSubmissionsLoaded = false;
let announcements = [];
let vocabulary = [];
let releases = [];
let stories = [];
let searchDebounceTimer = null;
let unsubscribeFns = [];

// Whether each collection has answered at least once. The Overview holds a shimmer until it has,
// because a count that renders as 0 before the snapshot arrives is not "loading", it is wrong.
let submissionsLoaded = false;
let vocabularyLoaded = false;
let releasesLoaded = false;
let storiesLoaded = false;

// -- Dialogs ------------------------------------------------------------------------------------
// confirm() and alert() block the whole page, cannot be styled, and some browsers suppress them
// outright after repeated use -- a moderator then sees a Delete button silently do nothing. These
// two replacements use the modal and the status region the stylesheet already covers.

function dialogHost() {
  let host = document.getElementById('confirm-dialog');
  if (host) return host;
  host = document.createElement('div');
  host.id = 'confirm-dialog';
  host.className = 'modal-overlay';
  host.innerHTML =
    '<div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">' +
      '<div class="modal-header"><h3 id="confirm-dialog-title"></h3></div>' +
      '<div class="modal-body" id="confirm-dialog-body"></div>' +
      '<div class="modal-actions">' +
        '<button type="button" class="btn btn-outline" data-act="cancel">Cancel</button>' +
        '<button type="button" class="btn btn-primary" data-act="ok"></button>' +
      '</div>' +
    '</div>';
  document.body.appendChild(host);
  return host;
}

// Resolves true/false. Escape and the backdrop both cancel, which is the escape route a blocking
// confirm() never offered, and focus returns to whatever opened it.
function confirmDialog(opts) {
  const o = opts || {};
  const host = dialogHost();
  const okBtn = host.querySelector('[data-act="ok"]');
  const cancelBtn = host.querySelector('[data-act="cancel"]');
  host.querySelector('#confirm-dialog-title').textContent = o.title || 'Are you sure?';
  host.querySelector('#confirm-dialog-body').innerHTML = o.body || '';
  okBtn.textContent = o.confirmLabel || 'Confirm';
  okBtn.className = 'btn ' + (o.danger ? 'btn-danger' : 'btn-primary');

  const opener = document.activeElement;
  return new Promise((resolve) => {
    function close(result) {
      host.classList.remove('active');
      host.removeEventListener('click', onBackdrop);
      document.removeEventListener('keydown', onKey);
      okBtn.onclick = null;
      cancelBtn.onclick = null;
      if (opener && opener.focus) opener.focus();
      resolve(result);
    }
    function onBackdrop(e) { if (e.target === host) close(false); }
    function onKey(e) { if (e.key === 'Escape') close(false); }
    okBtn.onclick = () => close(true);
    cancelBtn.onclick = () => close(false);
    host.addEventListener('click', onBackdrop);
    document.addEventListener('keydown', onKey);
    host.classList.add('active');
    okBtn.focus();
  });
}

// A toast, not a dialog: it reports what already happened, so it must not take focus or block the
// next action. polite rather than assertive for the same reason.
function notify(message, kind) {
  let host = document.getElementById('toast-host');
  if (!host) {
    host = document.createElement('div');
    host.id = 'toast-host';
    host.className = 'toast-host';
    host.setAttribute('role', 'status');
    host.setAttribute('aria-live', 'polite');
    document.body.appendChild(host);
  }
  const el = document.createElement('div');
  el.className = 'toast toast-' + (kind || 'info');
  el.textContent = message;
  host.appendChild(el);
  setTimeout(() => {
    el.classList.add('leaving');
    setTimeout(() => el.remove(), 220);
  }, kind === 'error' ? 6000 : 3800);
}


// ── Auth Guard ──────────────────────────────────────────────────────────────
// If the user is not authenticated, redirect to login page immediately.
// If the user is authenticated but lacks the `admin` custom claim, deny access.
// The claim is set with the bootstrapAdmin Cloud Function (see /functions).
onAuthStateChanged(auth, (user) => {
  const loadingScreen = document.getElementById('auth-loading-screen');

  if (!user) {
    // Not logged in — go to login
    window.location.href = 'index.html';
    return;
  }

  // Verify the admin custom claim (enforced server-side by Firestore rules too).
  user.getIdTokenResult().then((idTokenResult) => {
    if (idTokenResult.claims && idTokenResult.claims.admin === true) {
      // Admin — show the dashboard
      if (loadingScreen) {
        loadingScreen.classList.add('hidden');
        setTimeout(() => loadingScreen.remove(), 500);
      }

      // Display admin email
      const emailDisplay = document.getElementById('admin-email-display');
      if (emailDisplay) emailDisplay.textContent = user.email;

      // Initialize dashboard
      init();
    } else {
      // Signed in but not an admin — show access denied, then sign out.
      if (loadingScreen) {
        loadingScreen.classList.add('hidden');
        setTimeout(() => loadingScreen.remove(), 500);
      }
      const denied = document.getElementById('access-denied-screen');
      if (denied) denied.classList.remove('hidden');
      const deniedEmail = document.getElementById('access-denied-email-display');
      if (deniedEmail) deniedEmail.textContent = user.email;
      setTimeout(() => window.adminSignOut(), 4000);
    }
  }).catch((err) => {
    console.error('Failed to read admin claim:', err);
    if (loadingScreen) {
      loadingScreen.classList.add('hidden');
      setTimeout(() => loadingScreen.remove(), 500);
    }
    const denied = document.getElementById('access-denied-screen');
    if (denied) denied.classList.remove('hidden');
  });
});

// Sign out function (called from dashboard.html Sign Out button)
window.adminSignOut = async function () {
  try {
    // Unsubscribe all Firestore listeners before signing out
    unsubscribeFns.forEach(fn => fn());
    await signOut(auth);
    window.location.href = 'index.html';
  } catch (err) {
    console.error('Sign out error:', err);
  }
};

// ── Tab Switcher ────────────────────────────────────────────────────────────
// Tabs are routes, not just visual states. Without a URL per section the dashboard could not be
// bookmarked or shared, and browser Back left the panel entirely instead of returning to the
// previous tab -- the single most disorienting thing about the old console.
const TAB_ROUTES = {
  'tab-dashboard': 'overview',
  'tab-submissions': 'queue',
  'tab-vocabulary': 'dictionary',
  'tab-stories': 'stories',
  'tab-releases': 'releases'
};
const ROUTE_TABS = Object.fromEntries(Object.entries(TAB_ROUTES).map(([k, v]) => [v, k]));

function applyTab(targetTab) {
  document.querySelectorAll('nav button[data-tab]').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-tab') === targetTab);
  });
  document.querySelectorAll('.tab-content').forEach(tc => {
    const on = tc.id === targetTab;
    tc.classList.toggle('active', on);
    tc.style.setProperty('display', on ? 'flex' : 'none', 'important');
  });
}

window.switchTab = function(targetTab, fromHistory) {
  applyTab(targetTab);
  const route = TAB_ROUTES[targetTab];
  if (!route || fromHistory) return;
  if (location.hash.slice(1) !== route) {
    history.pushState({ tab: targetTab }, '', '#' + route);
  }
};

window.addEventListener('popstate', () => {
  const tab = ROUTE_TABS[location.hash.slice(1)] || 'tab-dashboard';
  window.switchTab(tab, true);
});

// ── Init ────────────────────────────────────────────────────────────────────
function init() {
  initNavigation();
  initRealtimeListeners();
  initExcelImporter();
  initSqlImporter();
  initFormListeners();
  initStoriesListener();
  initStoryForm();
  initStoryImageInput();
  initTopbar();
  initModalBehaviour();
  initDictionaryControls();

  // Open whatever the URL asks for, so a bookmarked or shared link lands on the right section.
  const routed = ROUTE_TABS[location.hash.slice(1)];
  applyTab(routed || 'tab-dashboard');
}

function initNavigation() {
  const navBtns = document.querySelectorAll('nav button[data-tab]');
  navBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const targetTab = btn.getAttribute('data-tab');
      window.switchTab(targetTab);
    });
  });

  const subnavBtns = document.querySelectorAll('.subnav-pill-btn');
  subnavBtns.forEach(sb => {
    sb.addEventListener('click', () => {
      subnavBtns.forEach(b => b.classList.remove('active'));
      sb.classList.add('active');
    });
  });
}

// ── Realtime Firestore Sync ─────────────────────────────────────────────────
function initRealtimeListeners() {
  // 1. Word Submissions Listener (with fallback for missing index/fields or permission checks)
  try {
    const subQueryPrimary = query(collection(db, "word_submissions"), orderBy("submittedAt", "desc"));
    const unsubSub = onSnapshot(subQueryPrimary, (snapshot) => {
      submissions = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      submissionsLoaded = true;
      renderSubmissionsTable();
      updateDashboardMetrics();
    }, (error) => {
      console.warn("Primary submissions query failed, attempting plain fallback query:", error);
      
      // Fallback: Plain query without orderBy (never fails on missing indexes or fields)
      try {
        const subQueryFallback = query(collection(db, "word_submissions"));
        const unsubFallback = onSnapshot(subQueryFallback, (snapshot) => {
          submissions = snapshot.docs
            .map(doc => ({ id: doc.id, ...doc.data() }))
            .sort((a, b) => (b.submittedAt || b.createdAt || 0) - (a.submittedAt || a.createdAt || 0));
          submissionsLoaded = true;
          renderSubmissionsTable();
          updateDashboardMetrics();
        }, (fallbackErr) => {
          console.error("Submissions fallback listener error:", fallbackErr);
          let msg = "Unable to connect to live Firestore submissions queue.";
          if (fallbackErr.code === 'permission-denied') {
            msg = "Permission denied. Ensure Firestore Rules allow read for authenticated admins.";
          }
          renderSubmissionsError(msg);
        });
        unsubscribeFns.push(unsubFallback);
      } catch (e) {
        renderSubmissionsError("Unable to connect to live Firestore submissions queue.");
      }
    });
    unsubscribeFns.push(unsubSub);
  } catch (e) {
    console.error("Firestore submission query error:", e);
  }

  // 1b. Literature Submissions Listener (stories/poems, extending the word-submissions queue).
  try {
    const litQuery = query(collection(db, "literature_submissions"), orderBy("submittedAt", "desc"));
    const unsubLit = onSnapshot(litQuery, (snapshot) => {
      literatureSubmissions = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      literatureSubmissionsLoaded = true;
      renderLiteratureSubmissionsTable();
      updateDashboardMetrics();
    }, (error) => {
      console.warn("Literature submissions listener error:", error);
      renderLiteratureSubmissionsError("Unable to connect to live Firestore literature queue.");
    });
    unsubscribeFns.push(unsubLit);
  } catch (e) {
    console.error("Firestore literature submission query error:", e);
  }

  // 1c. Announcements Listener - the admin's own view of what AnnouncementRepository serves live.
  try {
    const annQuery = query(collection(db, "announcements"), orderBy("createdAt", "desc"));
    const unsubAnn = onSnapshot(annQuery, (snapshot) => {
      announcements = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      renderAnnouncementsList();
    }, (error) => {
      console.warn("Announcements listener error:", error);
    });
    unsubscribeFns.push(unsubAnn);
  } catch (e) {
    console.error("Firestore announcements query error:", e);
  }

  // 2. Vocabulary Listener
  try {
    const vocabQuery = query(collection(db, "vocabulary"));
    const unsubVocab = onSnapshot(vocabQuery, (snapshot) => {
      vocabulary = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      vocabularyLoaded = true;
      renderVocabularyTable();
      updateDashboardMetrics();
    }, (error) => {
      console.warn("Vocabulary listener error:", error);
      let msg = "Unable to connect to live Firestore master dictionary.";
      if (error.code === 'permission-denied') {
        msg = "Permission denied. Ensure Firestore Rules allow read access.";
      }
      renderVocabularyError(msg);
    });
    unsubscribeFns.push(unsubVocab);
  } catch (e) {
    console.error("Firestore vocab query error:", e);
  }

  // 3. App Releases Listener (with fallback)
  try {
    const releaseQueryPrimary = query(collection(db, "app_releases"), orderBy("versionCode", "desc"));
    const unsubReleases = onSnapshot(releaseQueryPrimary, (snapshot) => {
      releases = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      releasesLoaded = true;
      renderReleasesList();
      updateDashboardMetrics();
    }, (error) => {
      console.warn("Primary release query failed, attempting plain fallback query:", error);
      try {
        const releaseQueryFallback = query(collection(db, "app_releases"));
        const unsubRelFallback = onSnapshot(releaseQueryFallback, (snapshot) => {
          releases = snapshot.docs
            .map(doc => ({ id: doc.id, ...doc.data() }))
            .sort((a, b) => (b.versionCode || 0) - (a.versionCode || 0));
          releasesLoaded = true;
          renderReleasesList();
          updateDashboardMetrics();
        }, (relErr) => {
          console.warn("Releases fallback listener error:", relErr);
        });
        unsubscribeFns.push(unsubRelFallback);
      } catch (e) {}
    });
    unsubscribeFns.push(unsubReleases);
  } catch (e) {
    console.error("Firestore release query error:", e);
  }
}

// ── Dashboard Metrics ───────────────────────────────────────────────────────
// ── Stories ─────────────────────────────────────────────────────────────────
// The app ships a built-in corpus and overwrites it with whatever this collection holds, matching on
// the numeric `id` field. That makes this the place a story is actually edited: a change here reaches
// every device on the next sync, while the built-in copy only changes with an app release.

function initStoriesListener() {
  const storiesQuery = query(collection(db, "stories"));
  const unsub = onSnapshot(storiesQuery, (snapshot) => {
    stories = snapshot.docs.map(d => ({ docId: d.id, ...d.data() }));
    stories.sort((a, b) => (a.id || 0) - (b.id || 0));
    storiesLoaded = true;
    renderStoriesTable();
    updateDashboardMetrics();
  }, (error) => {
    console.error('Stories listener failed:', error);
    const tbody = document.getElementById('stories-tbody');
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:2rem; color:var(--status-rejected);">
        Couldn't load stories: ${escapeHtml(error.message)}</td></tr>`;
    }
  });
  unsubscribeFns.push(unsub);
}

// ── Modals ──────────────────────────────────────────────────────────────────
// A dialog takes focus, keeps it, and gives it back. Without that a keyboard user tabs straight
// out of the open modal into the page behind it, which is still scrolling and still clickable.
let modalStack = [];

window.openModal = function(id) {
  const el = document.getElementById(id);
  if (!el) return;
  const card = el.querySelector('.modal-card');
  if (card) {
    card.setAttribute('role', 'dialog');
    card.setAttribute('aria-modal', 'true');
    card.setAttribute('tabindex', '-1');
  }
  modalStack.push({ id, returnTo: document.activeElement });
  el.classList.add('active');
  document.body.style.overflow = 'hidden';

  // Land on the first thing worth typing into, or the dialog itself when there is nothing.
  const first = card && card.querySelector(
    'input:not([type=hidden]):not([disabled]), select, textarea, button:not(.close-btn)'
  );
  (first || card)?.focus({ preventScroll: true });
};

window.closeModal = function(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.remove('active');
  const idx = modalStack.map(m => m.id).lastIndexOf(id);
  const entry = idx > -1 ? modalStack.splice(idx, 1)[0] : null;
  if (modalStack.length === 0) document.body.style.overflow = '';
  entry?.returnTo?.focus?.({ preventScroll: true });
};

function initModalBehaviour() {
  // Escape closes the topmost dialog; the backdrop closes the one that was clicked.
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modalStack.length) {
      e.preventDefault();
      window.closeModal(modalStack[modalStack.length - 1].id);
      return;
    }
    if (e.key !== 'Tab' || !modalStack.length) return;

    const card = document.getElementById(modalStack[modalStack.length - 1].id)?.querySelector('.modal-card');
    if (!card) return;
    const focusable = [...card.querySelectorAll(
      'a[href], button:not([disabled]), input:not([type=hidden]):not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )].filter(n => n.offsetParent !== null);
    if (!focusable.length) return;

    const first = focusable[0], last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  });

  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('mousedown', (e) => {
      if (e.target === overlay) window.closeModal(overlay.id);
    });
  });
}

// ── Counting figures ────────────────────────────────────────────────────────
// DESIGN.md's rule for counters, ported from the app: "the number counts, it does not fade in."
// This is the Overview's one authored moment; everything else on the console is quiet feedback.
const countTimers = new Map();

function countTo(el, target) {
  if (!el) return;
  const from = parseInt((el.dataset.value ?? '0'), 10) || 0;
  el.dataset.value = String(target);

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (reduced || from === target || target > 100000) {
    el.textContent = target.toLocaleString();
    return;
  }

  cancelAnimationFrame(countTimers.get(el) || 0);
  const start = performance.now();
  const dur = 600;
  const step = (now) => {
    const t = Math.min(1, (now - start) / dur);
    const eased = 1 - Math.pow(1 - t, 3);          // ease-out, matching the app's curve
    el.textContent = Math.round(from + (target - from) * eased).toLocaleString();
    if (t < 1) countTimers.set(el, requestAnimationFrame(step));
    else countTimers.delete(el);
  };
  countTimers.set(el, requestAnimationFrame(step));
}

// ── Dictionary ──────────────────────────────────────────────────────────────
let vocabLetter = '';

const POS_SHORT = {
  'Noun': 'n.', 'Verb': 'v.', 'Adjective': 'adj.', 'Adverb': 'adv.', 'Pronoun': 'pron.',
  'Preposition': 'prep.', 'Conjunction / Connector': 'conj.', 'Interjection': 'interj.',
  'Marker & Particle': 'part.'
};

window.setVocabLetter = function(letter) {
  vocabLetter = (vocabLetter === letter) ? '' : letter;
  vocabPage = 1;
  renderVocabularyTable();
};

function filteredVocabulary() {
  const rawSearch = document.getElementById('search-vocab-input')?.value || '';
  const term = rawSearch.trim().toLowerCase();
  const cat = document.getElementById('filter-vocab-category')?.value || '';

  return vocabulary.filter(item => {
    const matchesSearch = !term ||
      (item.kasiguranin || '').toLowerCase().includes(term) ||
      (item.tagalog || '').toLowerCase().includes(term) ||
      (item.english || '').toLowerCase().includes(term);
    const matchesCat = !cat || item.category === cat;
    const matchesLetter = !vocabLetter ||
      (item.kasiguranin || '').trim().charAt(0).toUpperCase() === vocabLetter;
    return matchesSearch && matchesCat && matchesLetter;
  });
}

function renderLetterRail() {
  const rail = document.getElementById('letter-rail');
  if (!rail) return;

  // Which letters the corpus actually has, given the search and category already applied. A letter
  // is disabled rather than hidden so the shape of the corpus stays visible.
  const term = (document.getElementById('search-vocab-input')?.value || '').trim().toLowerCase();
  const cat = document.getElementById('filter-vocab-category')?.value || '';
  const present = new Set(
    vocabulary
      .filter(i => (!term ||
          (i.kasiguranin || '').toLowerCase().includes(term) ||
          (i.tagalog || '').toLowerCase().includes(term) ||
          (i.english || '').toLowerCase().includes(term)) &&
        (!cat || i.category === cat))
      .map(i => (i.kasiguranin || '').trim().charAt(0).toUpperCase())
      .filter(Boolean)
  );

  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
  rail.innerHTML =
    `<button type="button" onclick="window.setVocabLetter('')" aria-pressed="${!vocabLetter}">All</button>` +
    letters.map(L => {
      const has = present.has(L);
      return `<button type="button" onclick="window.setVocabLetter('${L}')" aria-pressed="${vocabLetter === L}"${has ? '' : ' disabled'}>${L}</button>`;
    }).join('');
}

function renderVocabularyTable() {
  const host = document.getElementById('entry-list');
  if (!host) return;

  renderLetterRail();
  const filtered = filteredVocabulary();

  filtered.sort((a, b) => {
    const av = (a[vocabSort.key] || '').toString().toLowerCase();
    const bv = (b[vocabSort.key] || '').toString().toLowerCase();
    const cmp = av.localeCompare(bv);
    if (cmp !== 0) return vocabSort.dir === 'asc' ? cmp : -cmp;
    // Within a category or part of speech, fall back to the headword so the order is stable.
    return (a.kasiguranin || '').localeCompare(b.kasiguranin || '');
  });

  const countEl = document.getElementById('vocab-result-count');
  if (countEl) {
    const bits = [];
    if (vocabLetter) bits.push(`starting with ${vocabLetter}`);
    countEl.textContent = filtered.length === vocabulary.length
      ? `${vocabulary.length.toLocaleString()} entries`
      : `${filtered.length.toLocaleString()} of ${vocabulary.length.toLocaleString()} entries${bits.length ? ' ' + bits.join(', ') : ''}`;
  }

  if (filtered.length === 0) {
    host.innerHTML = `
      <div class="empty">
        <iconsax-icon name="book-1" type="bulk" size="30" color="currentColor"></iconsax-icon>
        <b>No entries match</b>
        Try a different spelling, clear the category, or choose All on the letter index.
      </div>`;
    renderVocabPager(0);
    return;
  }

  // Paged rather than rendering the whole corpus: every filtered entry used to be built into the
  // DOM on each keystroke of the debounced search, which is work nobody can see.
  const pages = Math.max(1, Math.ceil(filtered.length / VOCAB_PAGE_SIZE));
  if (vocabPage > pages) vocabPage = pages;
  const start = (vocabPage - 1) * VOCAB_PAGE_SIZE;
  const slice = filtered.slice(start, start + VOCAB_PAGE_SIZE);

  host.innerHTML = slice.map(item => {
    const pos = item.partOfSpeech || '';
    const posShort = POS_SHORT[pos] || (pos ? pos.toLowerCase() + '.' : '');
    const glosses = [];
    if (item.tagalog) glosses.push(`<span class="lang">TL</span>${escapeHtml(item.tagalog)}`);
    if (item.english) glosses.push(`<span class="lang">EN</span>${escapeHtml(item.english)}`);
    const aspects = ['neutralForm', 'perfectiveForm', 'imperfectiveForm', 'contemplativeForm']
      .filter(k => (item[k] || '').trim()).length;

    return `
      <button type="button" class="entry" onclick="window.openEntryModal('${item.id}')">
        <span>
          <span class="entry-head">
            <span class="headword">${escapeHtml(item.kasiguranin || '—')}</span>
            ${item.ipaNotation ? `<span class="ipa">/${escapeHtml(item.ipaNotation)}/</span>` : ''}
            ${posShort ? `<span class="pos">${escapeHtml(posShort)}</span>` : ''}
          </span>
          <span class="gloss">${glosses.join('<span class="sep">·</span>') || '<span class="lang">No gloss recorded yet</span>'}</span>
        </span>
        <span class="entry-side">
          ${aspects ? `<span class="badge badge-pending">${aspects} aspect${aspects === 1 ? '' : 's'}</span>` : ''}
          <span class="badge badge-category">${escapeHtml(item.category || 'General')}</span>
        </span>
      </button>`;
  }).join('');

  renderVocabPager(filtered.length);
}

window.openEntryModal = function(id) {
  const item = vocabulary.find(v => v.id === id);
  const body = document.getElementById('entry-modal-body');
  if (!item || !body) return;

  const pos = item.partOfSpeech || '';
  const aspects = [
    ['Neutral', item.neutralForm],
    ['Past', item.perfectiveForm],
    ['Present', item.imperfectiveForm],
    ['Future', item.contemplativeForm]
  ].filter(([, v]) => (v || '').trim());

  const row = (label, value) => value
    ? `<dt>${label}</dt><dd>${escapeHtml(value)}</dd>`
    : `<dt>${label}</dt><dd style="color:var(--muted); font-style:italic;">Not recorded</dd>`;

  body.innerHTML = `
    <div class="entry-detail-head">
      <span class="headword">${escapeHtml(item.kasiguranin || '—')}</span>
      ${item.ipaNotation ? `<span class="ipa">/${escapeHtml(item.ipaNotation)}/</span>` : ''}
      ${pos ? `<span class="pos">${escapeHtml(pos)}</span>` : ''}
    </div>
    <dl class="deflist">
      ${row('Tagalog', item.tagalog)}
      ${row('English', item.english)}
      <dt>Category</dt><dd><span class="badge badge-category">${escapeHtml(item.category || 'General')}</span></dd>
    </dl>
    ${aspects.length ? `
      <div style="margin-top:var(--s-5);">
        <dt style="font-size:var(--t-xs); font-weight:700; color:var(--muted);">Verb aspects</dt>
        <div class="aspect-grid">
          ${aspects.map(([label, value]) => `
            <div class="aspect"><span>${label}</span><b>${escapeHtml(value)}</b></div>`).join('')}
        </div>
      </div>` : ''}`;

  const editBtn = document.getElementById('entry-modal-edit');
  if (editBtn) {
    editBtn.onclick = () => {
      window.closeModal('entry-modal');
      window.openEditVocabModal(id);
    };
  }
  window.openModal('entry-modal');
};

// ── Stories ─────────────────────────────────────────────────────────────────
function renderStoriesTable() {
  const host = document.getElementById('story-grid');
  if (!host) return;

  if (stories.length === 0) {
    // An empty collection is the normal, correct condition for a project relying on the app's
    // built-in corpus, so this must not read as a failure.
    host.innerHTML = `
      <div class="panel" style="grid-column:1/-1; margin:0;">
        <div class="empty">
          <iconsax-icon name="document-text" type="bulk" size="30" color="currentColor"></iconsax-icon>
          <b>Learners are reading the built-in stories</b>
          The ten stories shipped with the app are live. Add one here only to change or extend that set.
        </div>
      </div>`;
    return;
  }

  host.innerHTML = stories.map(story => {
    let pages = story.totalPages || 0;
    try {
      const parsed = JSON.parse(story.pagesJson || '[]');
      if (Array.isArray(parsed)) pages = parsed.length;
    } catch (e) { /* keep totalPages */ }

    const kasi = (story.titleKasiguranin || '').trim();
    const xp = (story.requiredXp || 0) === 0
      ? '<span class="badge badge-approved">Free</span>'
      : `<span class="badge badge-category">${story.requiredXp} XP</span>`;

    // The cover is an optional slot. With no artwork the violet field plus the page count is a
    // finished cover, not a placeholder — which is what DESIGN.md asks of every art position.
    return `
      <article class="story-card">
        <div class="story-cover">
          <span class="story-cover-id">${escapeHtml(String(story.id ?? '·'))}</span>
          <span class="story-cover-pages">${pages} page${pages === 1 ? '' : 's'}</span>
        </div>
        <div class="story-body">
          <h3 class="story-title">${escapeHtml(story.title || 'Untitled story')}</h3>
          <p class="story-kasi${kasi ? '' : ' is-missing'}">${kasi ? escapeHtml(kasi) : 'Kasiguranin title not written yet'}</p>
          <div class="story-meta">
            <span class="badge badge-category">${escapeHtml(story.category || 'Story')}</span>
            ${xp}
          </div>
        </div>
        <div class="story-actions">
          <button class="btn btn-outline btn-sm" onclick="window.openStoryEditor('${story.docId}')">
            <iconsax-icon name="edit" type="bulk" size="15" color="currentColor"></iconsax-icon> Edit
          </button>
          <button class="btn btn-quiet-danger btn-sm" onclick="window.deleteStory('${story.docId}')">
            <iconsax-icon name="close-circle" type="bulk" size="15" color="currentColor"></iconsax-icon> Delete
          </button>
        </div>
      </article>`;
  }).join('');
}

// ── Releases ────────────────────────────────────────────────────────────────
function renderReleasesList() {
  const container = document.getElementById('releases-list-container');
  if (!container) return;

  if (releases.length === 0) {
    container.innerHTML = `
      <div class="empty">
        <iconsax-icon name="box-search" type="bulk" size="30" color="currentColor"></iconsax-icon>
        <b>No releases published yet</b>
        Publish one to give learners something to install.
      </div>`;
    return;
  }

  container.innerHTML = releases.map((rel, i) => {
    const ms = toMillis(rel.releasedAt);
    const when = ms
      ? new Date(ms).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
      : 'date not recorded';
    const notes = (rel.releaseNotes || '').trim();

    return `
      <div class="release-row${i === 0 ? ' is-live' : ''}">
        <div class="release-node"><span class="release-dot" aria-hidden="true"></span></div>
        <div class="release-main">
          <div class="release-title">
            <b>v${escapeHtml(String(rel.versionName || '?'))}</b>
            <small>Build ${escapeHtml(String(rel.versionCode ?? '—'))} · ${escapeHtml(when)}</small>
            ${i === 0 ? '<span class="badge badge-approved">Live</span>' : ''}
            ${rel.forceUpdate ? '<span class="badge badge-pending">Required</span>' : ''}
          </div>
          <p class="release-notes${notes ? '' : ' is-empty'}">${notes ? escapeHtml(notes) : 'No release notes were recorded for this build.'}</p>
        </div>
        <div class="release-side">
          ${rel.apkUrl
            ? `<a href="${escapeHtml(rel.apkUrl)}" target="_blank" rel="noopener" class="btn btn-outline btn-sm">
                 <iconsax-icon name="document-download" type="bulk" size="15" color="currentColor"></iconsax-icon> APK
               </a>`
            : '<span class="result-count">No link</span>'}
        </div>
      </div>`;
  }).join('');
}



// ── Page editor ─────────────────────────────────────────────────────────────
// Pages are stored as a JSON string on the story document, so the editor holds the array in memory
// and serialises it on save. Page numbers are assigned from position rather than typed, which removes
// a whole class of mistake — a duplicated or skipped number breaks the reader's paging.
//
// The array, not the DOM, is the source of truth. The previous version read every field back out of
// the DOM on each add or remove, which meant a page could only carry the fields this file happened to
// know about: editing any story silently stripped `audioFileName`, which the app reads and the admin
// has no input for. Pages now round-trip whole, and only the fields with inputs are overwritten.

let storyPages = [];              // the working array while the modal is open
let storyPageExpanded = 0;        // index of the one expanded page, -1 for none
const pendingImages = new Map();  // imageId -> { blob, url, width, height } awaiting save
const removedImageIds = new Set();// imageIds whose documents must be deleted on save

const STORY_IMAGE_EDGE = 1440;    // px, square
const STORY_IMAGE_QUALITY = 0.85;
const STORY_IMAGE_MAX_BYTES = 400 * 1024;  // must match the Firestore rule

function newImageId() {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);
}

// Release every object URL this modal created. Without this each reopen leaks a blob per picture.
function releasePendingImages() {
  pendingImages.forEach(entry => { if (entry.url) URL.revokeObjectURL(entry.url); });
  pendingImages.clear();
  removedImageIds.clear();
}

// Centre-crop to the shorter side, then draw to a fixed square. Cropping rather than squashing is
// what makes "square orientation" honest — a portrait photo keeps its proportions and loses its edges
// instead of being distorted into a square.
async function processStoryImage(file) {
  if (!file.type.startsWith('image/')) {
    throw new Error('That file is not an image.');
  }
  const bitmap = await createImageBitmap(file);
  const side = Math.min(bitmap.width, bitmap.height);
  const sx = Math.round((bitmap.width - side) / 2);
  const sy = Math.round((bitmap.height - side) / 2);

  const canvas = document.createElement('canvas');
  canvas.width = STORY_IMAGE_EDGE;
  canvas.height = STORY_IMAGE_EDGE;
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(bitmap, sx, sy, side, side, 0, 0, STORY_IMAGE_EDGE, STORY_IMAGE_EDGE);
  bitmap.close?.();

  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(b => b ? resolve(b) : reject(new Error('The image could not be encoded.')),
                  'image/webp', STORY_IMAGE_QUALITY);
  });

  if (blob.size > STORY_IMAGE_MAX_BYTES) {
    throw new Error(`That picture encodes to ${Math.round(blob.size / 1024)} KB, over the ${Math.round(STORY_IMAGE_MAX_BYTES / 1024)} KB limit. Try a less detailed image.`);
  }
  return { blob, width: STORY_IMAGE_EDGE, height: STORY_IMAGE_EDGE };
}

window.pickStoryPageImage = function(index) {
  const input = document.getElementById('story-page-file');
  if (!input) return;
  input.dataset.pageIndex = String(index);
  input.value = '';
  input.click();
};

async function acceptStoryPageImage(index, file) {
  const page = storyPages[index];
  if (!page || !file) return;
  try {
    const { blob, width, height } = await processStoryImage(file);
    // Replacing a saved picture leaves the old document behind, so mark it for deletion.
    if (page.imageId && !pendingImages.has(page.imageId)) removedImageIds.add(page.imageId);
    const prev = page.imageId && pendingImages.get(page.imageId);
    if (prev?.url) URL.revokeObjectURL(prev.url);

    const imageId = newImageId();
    page.imageId = imageId;
    pendingImages.set(imageId, { blob, url: URL.createObjectURL(blob), width, height });
    renderStoryPages();
    notify(`Picture added to page ${index + 1} (${Math.round(blob.size / 1024)} KB).`, 'success');
  } catch (err) {
    notify(err.message || 'That picture could not be read.', 'error');
  }
}

window.removeStoryPageImage = function(index) {
  const page = storyPages[index];
  if (!page || !page.imageId) return;
  const pending = pendingImages.get(page.imageId);
  if (pending?.url) URL.revokeObjectURL(pending.url);
  if (pending) pendingImages.delete(page.imageId);
  else removedImageIds.add(page.imageId);   // already saved: delete its document
  page.imageId = '';
  renderStoryPages();
};

// The preview source: a freshly picked blob, or the stored bytes fetched back for an existing page.
function storyPagePreviewSrc(page) {
  if (!page.imageId) return '';
  const pending = pendingImages.get(page.imageId);
  if (pending) return pending.url;
  return savedImageUrls.get(page.imageId) || '';
}

const savedImageUrls = new Map();   // imageId -> object URL for pictures already in Firestore

// Fetch the pictures an existing story already has, so reopening the editor shows them rather than an
// empty slot that looks like the image was lost.
async function loadSavedStoryImages(storyId, pages) {
  const ids = pages.map(p => p.imageId).filter(Boolean);
  for (const imageId of ids) {
    if (savedImageUrls.has(imageId)) continue;
    try {
      const snap = await getDoc(doc(db, 'story_page_images', `${storyId}_${imageId}`));
      if (!snap.exists()) continue;
      const bytes = snap.data().data?.toUint8Array?.();
      if (!bytes) continue;
      const blob = new Blob([bytes], { type: snap.data().mimeType || 'image/webp' });
      savedImageUrls.set(imageId, URL.createObjectURL(blob));
      renderStoryPages();
    } catch (err) {
      console.warn('Could not load story page image', imageId, err);
    }
  }
}

function releaseSavedStoryImages() {
  savedImageUrls.forEach(url => URL.revokeObjectURL(url));
  savedImageUrls.clear();
}

function storyPageSummary(page) {
  const text = (page.tagalog || page.english || page.kasiguranin || '').trim();
  if (!text) return 'Empty page';
  return text.length > 46 ? text.slice(0, 46) + '…' : text;
}

function storyPageBlock(index, page) {
  const expanded = index === storyPageExpanded;
  const hasImage = Boolean(page.imageId);
  const src = expanded ? storyPagePreviewSrc(page) : '';
  const last = index === storyPages.length - 1;

  return `
    <div class="page-row${expanded ? ' is-open' : ''}" data-story-page>
      <div class="page-summary">
        <button type="button" class="page-toggle" onclick="window.toggleStoryPage(${index})"
                aria-expanded="${expanded}">
          <span class="page-caret" aria-hidden="true">${expanded ? '▾' : '▸'}</span>
          <span class="page-num">${index + 1}</span>
          <span class="page-text">${escapeHtml(storyPageSummary(page))}</span>
        </button>
        <span class="page-imgflag ${hasImage ? 'has' : ''}" title="${hasImage ? 'Has a picture' : 'No picture yet'}">
          ${hasImage ? '▣' : '□'}<span class="sr-only">${hasImage ? 'Has a picture' : 'No picture yet'}</span>
        </span>
        <button type="button" class="page-move" onclick="window.moveStoryPage(${index},-1)" ${index === 0 ? 'disabled' : ''} aria-label="Move page ${index + 1} up">&uarr;</button>
        <button type="button" class="page-move" onclick="window.moveStoryPage(${index},1)" ${last ? 'disabled' : ''} aria-label="Move page ${index + 1} down">&darr;</button>
        <button type="button" class="page-move page-del" onclick="window.removeStoryPage(${index})" aria-label="Remove page ${index + 1}">&times;</button>
      </div>

      ${expanded ? `
      <div class="page-body">
        <div class="page-image">
          <div class="page-thumb${src ? ' has-img' : ''}" onclick="window.pickStoryPageImage(${index})"
               role="button" tabindex="0" aria-label="Choose a picture for page ${index + 1}">
            ${src ? `<img src="${escapeHtml(src)}" alt="">`
                  : `<span>Add a square picture</span>`}
          </div>
          <div class="page-image-actions">
            <button type="button" class="btn btn-outline btn-sm" onclick="window.pickStoryPageImage(${index})">
              ${hasImage ? 'Replace' : 'Choose picture'}
            </button>
            ${hasImage ? `<button type="button" class="btn btn-quiet-danger btn-sm" onclick="window.removeStoryPageImage(${index})">Remove</button>` : ''}
            <p class="page-image-note">Any shape is accepted and centre-cropped to a square, then saved at ${STORY_IMAGE_EDGE}&times;${STORY_IMAGE_EDGE}.</p>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Kasiguranin</label>
          <textarea class="form-control" data-field="kasiguranin" rows="2" placeholder="Leave blank until written. The app hides the word-tap and audio controls while this is empty.">${escapeHtml(page.kasiguranin || '')}</textarea>
        </div>
        <div class="form-group">
          <label class="form-label">Tagalog *</label>
          <textarea class="form-control" data-field="tagalog" rows="2">${escapeHtml(page.tagalog || '')}</textarea>
        </div>
        <div class="form-group">
          <label class="form-label">English *</label>
          <textarea class="form-control" data-field="english" rows="2">${escapeHtml(page.english || '')}</textarea>
        </div>
        <div class="form-group" style="margin-bottom:0;">
          <label class="form-label">Illustration description</label>
          <input type="text" class="form-control" data-field="illustrationDesc" value="${escapeHtml(page.illustrationDesc || '')}" placeholder="Describes the picture for screen readers, and stands in for it when none exists.">
        </div>
      </div>` : ''}
    </div>`;
}

// Copy whatever is typed in the one expanded page back into the array before re-rendering, so
// switching pages never loses keystrokes.
function commitExpandedPage() {
  const block = document.querySelector('#story-pages-editor .page-row.is-open');
  if (!block || storyPageExpanded < 0) return;
  const page = storyPages[storyPageExpanded];
  if (!page) return;
  block.querySelectorAll('[data-field]').forEach(el => {
    page[el.getAttribute('data-field')] = el.value.trim();
  });
}

function renderStoryPages(pages) {
  if (Array.isArray(pages)) storyPages = pages;
  const host = document.getElementById('story-pages-editor');
  if (!host) return;

  const count = document.getElementById('story-page-count');
  if (count) count.textContent = storyPages.length === 1 ? '1 page' : `${storyPages.length} pages`;

  if (storyPages.length === 0) {
    host.innerHTML = `<p style="color:var(--muted); font-size:0.9rem;">No pages yet. A story needs at least one.</p>`;
    return;
  }
  host.innerHTML = storyPages.map((pg, i) => storyPageBlock(i, pg)).join('');
}

window.toggleStoryPage = function(index) {
  commitExpandedPage();
  storyPageExpanded = (storyPageExpanded === index) ? -1 : index;
  renderStoryPages();
};

window.moveStoryPage = function(index, delta) {
  commitExpandedPage();
  const target = index + delta;
  if (target < 0 || target >= storyPages.length) return;
  [storyPages[index], storyPages[target]] = [storyPages[target], storyPages[index]];
  if (storyPageExpanded === index) storyPageExpanded = target;
  else if (storyPageExpanded === target) storyPageExpanded = index;
  renderStoryPages();
};

window.addStoryPage = function() {
  commitExpandedPage();
  storyPages.push({ kasiguranin: '', tagalog: '', english: '', illustrationDesc: '', imageId: '' });
  storyPageExpanded = storyPages.length - 1;
  renderStoryPages();
  document.querySelector('#story-pages-editor .page-row.is-open')?.scrollIntoView({ block: 'nearest' });
};

window.removeStoryPage = async function(index) {
  const page = storyPages[index];
  const hasContent = page && (page.tagalog || page.english || page.kasiguranin || page.imageId);
  if (hasContent) {
    const ok = await confirmDialog({
      title: `Remove page ${index + 1}?`,
      body: 'Its text and picture are discarded when the story is saved.',
      confirmLabel: 'Remove page',
      danger: true
    });
    if (!ok) return;
  }
  commitExpandedPage();
  if (page?.imageId) {
    const pending = pendingImages.get(page.imageId);
    if (pending?.url) URL.revokeObjectURL(pending.url);
    if (pending) pendingImages.delete(page.imageId);
    else removedImageIds.add(page.imageId);
  }
  storyPages.splice(index, 1);
  if (storyPageExpanded >= storyPages.length) storyPageExpanded = storyPages.length - 1;
  renderStoryPages();
};

// Pages round-trip whole: only fields with inputs are overwritten, so keys this admin has no UI for
// (audioFileName, and anything added later) survive an edit instead of being silently dropped.
function readStoryPagesFromDom() {
  commitExpandedPage();
  return storyPages.map((page, i) => ({ ...page, pageNumber: i + 1 }));
}

function initStoryImageInput() {
  const input = document.getElementById('story-page-file');
  if (!input) return;
  input.addEventListener('change', (e) => {
    const index = parseInt(input.dataset.pageIndex, 10);
    const file = e.target.files && e.target.files[0];
    if (file && Number.isInteger(index)) acceptStoryPageImage(index, file);
  });
}


window.openStoryEditor = function(docId) {
  const editing = docId ? stories.find(s => s.docId === docId) : null;

  document.getElementById('story-editor-title').textContent = editing ? 'Edit story' : 'Add story';
  document.getElementById('story-doc-id').value = editing ? editing.docId : '';
  document.getElementById('story-input-id').value = editing ? (editing.id ?? '') : nextFreeStoryId();
  document.getElementById('story-input-title').value = editing ? (editing.title || '') : '';
  document.getElementById('story-input-title-kasiguranin').value = editing ? (editing.titleKasiguranin || '') : '';
  document.getElementById('story-input-description').value = editing ? (editing.description || '') : '';
  document.getElementById('story-input-category').value = editing ? (editing.category || '') : 'Folklore';
  document.getElementById('story-input-required-xp').value = editing ? (editing.requiredXp ?? 0) : 0;

  let pages = [];
  if (editing) {
    try {
      const parsed = JSON.parse(editing.pagesJson || '[]');
      if (Array.isArray(parsed)) pages = parsed;
    } catch (e) {
      console.warn('Story pagesJson could not be parsed; starting from empty.', e);
    }
  }
  releasePendingImages();
  releaseSavedStoryImages();
  storyPageExpanded = pages.length ? 0 : -1;
  renderStoryPages(pages);
  window.openModal('story-editor-modal');

  // Existing pictures are fetched back so reopening shows them rather than an empty slot that
  // reads as "the image was lost".
  if (editing && editing.id) loadSavedStoryImages(editing.id, pages);
};

// Suggests an id past both what this collection holds and the app's built-in corpus, so a new story
// adds to the set rather than silently replacing one of the ten already shipping.
function nextFreeStoryId() {
  const BUILT_IN_STORY_COUNT = 10;
  const highest = stories.reduce((max, s) => Math.max(max, s.id || 0), 0);
  return Math.max(highest, BUILT_IN_STORY_COUNT) + 1;
}

// Every picture belonging to a story. Used when a story is deleted, and when its numeric id changes
// (the id is part of the image document key, so the pictures have to move with it).
async function deleteStoryImages(storyId) {
  try {
    const snap = await getDocs(query(collection(db, 'story_page_images'), where('storyId', '==', storyId)));
    for (const d of snap.docs) await deleteDoc(doc(db, 'story_page_images', d.id));
  } catch (err) {
    console.warn('Could not remove story page images for story', storyId, err);
  }
}

window.deleteStory = async function(docId) {
  const story = stories.find(s => s.docId === docId);
  const name = story ? story.title : 'this story';
  if (!(await confirmDialog({
    title: `Delete "${name}"?`,
    body: 'Learners stop receiving it on their next sync. If the app ships a built-in story with the same id, that built-in version takes over again.',
    confirmLabel: 'Delete story', danger: true
  }))) return;

  try {
    await deleteDoc(doc(db, "stories", docId));
    if (story && story.id) await deleteStoryImages(story.id);
    await logAudit("story.delete", { docId, title: name, id: story ? story.id : null });
  } catch (error) {
    console.error('Story delete failed:', error);
    notify("Couldn't delete the story: " + error.message, 'error');
  }
};

function initStoryForm() {
  const form = document.getElementById('story-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn?.disabled) return;   // a double-click used to fire two writes

    const pages = readStoryPagesFromDom();
    if (pages.length === 0) {
      notify('A story needs at least one page.', 'error');
      return;
    }
    // The fields are no longer `required`, because a collapsed page cannot receive native validation
    // focus — the browser would silently refuse to submit with nothing visible to fix. Report the
    // offending page and open it instead.
    const incomplete = pages.findIndex(p => !p.tagalog || !p.english);
    if (incomplete !== -1) {
      storyPageExpanded = incomplete;
      renderStoryPages();
      document.querySelector('#story-pages-editor .page-row.is-open')?.scrollIntoView({ block: 'center' });
      notify(`Page ${incomplete + 1} needs both Tagalog and English before it can be saved.`, 'error');
      return;
    }

    const docId = document.getElementById('story-doc-id').value;
    const numericId = parseInt(document.getElementById('story-input-id').value, 10);
    if (!Number.isInteger(numericId) || numericId < 1) {
      notify('Story id must be a whole number of 1 or more.', 'error');
      return;
    }

    const existing = docId ? stories.find(s => s.docId === docId) : null;
    const basePayload = {
      id: numericId,
      title: document.getElementById('story-input-title').value.trim(),
      titleKasiguranin: document.getElementById('story-input-title-kasiguranin').value.trim(),
      description: document.getElementById('story-input-description').value.trim(),
      category: document.getElementById('story-input-category').value.trim() || 'Story',
      requiredXp: parseInt(document.getElementById('story-input-required-xp').value, 10) || 0,
      // The app reads iconEmoji but this admin has no input for it, and setDoc replaces the whole
      // document — so carry the existing value through rather than dropping it on every save.
      iconEmoji: existing?.iconEmoji ?? '📖',
      pagesJson: JSON.stringify(pages),
      totalPages: pages.length
    };
    // Stamped here rather than at the three setDoc/updateDoc call sites below, which all
    // write this same object. Millis, not the ISO string this used to store: the Android
    // incremental query compares numerically, and Firestore never returns a string field
    // as greater-than a number, so an ISO timestamp here would have meant the stories
    // sync silently returned nothing on every incremental pull. Documents still carrying
    // the old ISO value are picked up by the weekly full reconcile and rewritten as
    // millis on their next save; the backfill script converts them in one pass.
    const payload = withUpdatedAt(basePayload);

    const setBusy = (on, label) => {
      if (!submitBtn) return;
      submitBtn.disabled = on;
      submitBtn.textContent = on ? label : 'Save story';
    };

    try {
      // Pictures first. If a write fails partway the story still points at whatever already existed,
      // rather than at a document that was never created.
      const toUpload = pages.filter(p => p.imageId && pendingImages.has(p.imageId));
      for (let i = 0; i < toUpload.length; i++) {
        const page = toUpload[i];
        const img = pendingImages.get(page.imageId);
        setBusy(true, `Uploading picture ${i + 1} of ${toUpload.length}…`);
        const buf = new Uint8Array(await img.blob.arrayBuffer());
        await setDoc(doc(db, 'story_page_images', `${numericId}_${page.imageId}`), {
          storyId: numericId,
          imageId: page.imageId,
          data: Bytes.fromUint8Array(buf),
          mimeType: 'image/webp',
          width: img.width,
          height: img.height,
          byteSize: buf.length,
          updatedAt: new Date().toISOString()
        });
      }

      setBusy(true, 'Saving…');
      if (docId) {
        // Keep the document path and the numeric id in step. An admin who changes the id would
        // otherwise leave a document at stories/3 carrying id 7 - which still works, because the app
        // matches on the field, but makes the collection unreadable in the Firebase console.
        if (docId !== String(numericId)) {
          await setDoc(doc(db, "stories", String(numericId)), payload);
          await deleteDoc(doc(db, "stories", docId));
          await logAudit("story.reid", { from: docId, to: numericId, title: payload.title });
        } else {
          await updateDoc(doc(db, "stories", docId), payload);
          await logAudit("story.update", { docId, id: numericId, title: payload.title });
        }
      } else {
        // Document id mirrors the numeric story id, so the collection stays readable in the Firebase
        // console and a story cannot be added twice under the same id by accident.
        await setDoc(doc(db, "stories", String(numericId)), payload);
        await logAudit("story.create", { id: numericId, title: payload.title });
      }

      // Only now that the story no longer references them: drop replaced and removed pictures.
      for (const imageId of removedImageIds) {
        try { await deleteDoc(doc(db, 'story_page_images', `${numericId}_${imageId}`)); }
        catch (err) { console.warn('Could not remove old story image', imageId, err); }
      }
      if (toUpload.length || removedImageIds.size) {
        await logAudit("story.images", { id: numericId, added: toUpload.length, removed: removedImageIds.size });
      }

      const withPictures = pages.filter(p => p.imageId).length;
      releasePendingImages();
      releaseSavedStoryImages();
      window.closeModal('story-editor-modal');
      notify(`"${payload.title}" saved — ${pages.length} page${pages.length === 1 ? '' : 's'}, ${withPictures} with a picture.`, 'success');
    } catch (error) {
      console.error('Story save failed:', error);
      notify("Couldn't save the story: " + error.message, 'error');
    } finally {
      setBusy(false);
    }
  });
}

// ── Overview ────────────────────────────────────────────────────────────────
// Every figure on this page is counted from a collection the console already streams. Nothing here
// is modelled, estimated or filled in: the app has no analytics, so a download count or an
// engagement trend would be an invention, and the page says only what the data can support.

// Firestore hands timestamps back as either a millisecond number or a Timestamp object depending
// on which writer produced the document, and CI and the admin panel disagree. Normalise both.
function toMillis(value) {
  if (!value) return 0;
  if (typeof value === 'number') return value;
  if (typeof value.toMillis === 'function') return value.toMillis();
  if (typeof value.seconds === 'number') return value.seconds * 1000;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function relativeTime(ms) {
  if (!ms) return 'date unknown';
  const diff = Date.now() - ms;
  if (diff < 0) return 'just now';
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins} min ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} day${days === 1 ? '' : 's'} ago`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months} month${months === 1 ? '' : 's'} ago`;
  return `${Math.floor(months / 12)} year${Math.floor(months / 12) === 1 ? '' : 's'} ago`;
}

// A count that arrives a beat after paint should not first render as a zero that is wrong.
function setFigure(id, value, ready) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.toggle('skeleton', !ready);
  // Numeric figures count; anything else (a version string) is set directly.
  const n = typeof value === 'number' ? value : Number(String(value).replace(/[^0-9]/g, ''));
  if (ready && Number.isFinite(n) && String(value).match(/^[\d,]+$/)) countTo(el, n);
  else el.textContent = value;
}

function setNote(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

function updateDashboardMetrics() {
  const pending  = submissions.filter(s => (s.status || 'pending') === 'pending');
  const approved = submissions.filter(s => s.status === 'approved');
  const rejected = submissions.filter(s => s.status === 'rejected');
  const haveSubs = submissions.length > 0 || submissionsLoaded;
  const haveVocab = vocabulary.length > 0 || vocabularyLoaded;

  // ── Figures ───────────────────────────────────────────────────────────────
  setFigure('metric-pending-sub', pending.length, haveSubs);
  if (pending.length === 0) {
    setNote('metric-pending-note', haveSubs ? 'Queue is clear' : '&nbsp;');
  } else {
    const oldest = Math.min(...pending.map(s => toMillis(s.submittedAt || s.createdAt)).filter(Boolean));
    setNote('metric-pending-note', Number.isFinite(oldest)
      ? `Oldest waiting ${escapeHtml(relativeTime(oldest))}`
      : `${pending.length} to review`);
  }

  const categories = new Set(vocabulary.map(v => (v.category || 'General')));
  setFigure('metric-total-words', vocabulary.length.toLocaleString(), haveVocab);
  setNote('metric-words-note', haveVocab
    ? `Across ${categories.size} categor${categories.size === 1 ? 'y' : 'ies'}`
    : '&nbsp;');

  const written = stories.filter(s => (s.titleKasiguranin || '').trim()).length;
  setFigure('metric-total-stories', stories.length, storiesLoaded);
  setNote('metric-stories-note', !storiesLoaded ? '&nbsp;'
    : stories.length === 0 ? 'Using the app’s built-in set'
    : `${written} with Kasiguranin text`);

  setFigure('metric-total-releases', releases.length, releasesLoaded);
  const latest = releases[0];
  setNote('metric-releases-note', !releasesLoaded ? '&nbsp;'
    : latest ? `Latest build ${escapeHtml(String(latest.versionCode ?? '—'))}` : 'None published yet');

  renderCategoryChart();
  renderOutcomeFigure(approved.length, pending.length, rejected.length);
  renderRecentSubmissions();
  renderStoryShelf();
  renderReleaseCard(latest);

  // The bell mirrors the queue count already in the sidebar, so the two can never disagree.
  const navCount = document.getElementById('nav-queue-count');
  if (navCount) {
    navCount.textContent = pending.length;
    navCount.hidden = pending.length === 0;
  }
  const dot = document.getElementById('topbar-queue-dot');
  if (dot) {
    dot.textContent = pending.length > 99 ? '99+' : pending.length;
    dot.hidden = pending.length === 0;
  }
  const bellLabel = document.getElementById('topbar-queue-label');
  if (bellLabel) {
    bellLabel.textContent = pending.length === 0
      ? 'Verification queue, nothing waiting'
      : `Verification queue, ${pending.length} submission${pending.length === 1 ? '' : 's'} waiting`;
  }
}

// One bar per category, longest first. Each bar states its own count, so the figure reads without
// relying on bar length or on colour — which is also what makes it legible to a screen reader.
function renderCategoryChart() {
  const host = document.getElementById('category-bars');
  const caption = document.getElementById('chart-caption');
  if (!host) return;

  if (vocabulary.length === 0) {
    host.innerHTML = vocabularyLoaded
      ? `<div class="empty">
           <iconsax-icon name="book-1" type="bulk" size="30" color="currentColor"></iconsax-icon>
           <b>No dictionary entries yet</b>
           Import a corpus or add the first word to see the shape of it here.
         </div>`
      : '<div class="bar-row"><span class="bar-track skeleton" style="grid-column:1/-1"></span></div>'.repeat(6);
    if (caption) caption.textContent = 'Where the corpus is thin, and where it is not.';
    return;
  }

  const counts = new Map();
  vocabulary.forEach(v => {
    const key = (v.category || 'General').trim() || 'General';
    counts.set(key, (counts.get(key) || 0) + 1);
  });

  const ranked = [...counts.entries()].sort((a, b) => b[1] - a[1]);
  const shown = ranked.slice(0, 8);
  const max = shown[0][1];

  host.innerHTML = shown.map(([name, count], i) => `
    <div class="bar-row${i === 0 ? ' is-top' : ''}">
      <span class="bar-name" title="${escapeHtml(name)}">${escapeHtml(name)}</span>
      <span class="bar-track"><span class="bar-fill" style="width:${Math.max(2, (count / max) * 100).toFixed(1)}%"></span></span>
      <span class="bar-value">${count}</span>
    </div>`).join('');

  if (caption) {
    const rest = ranked.length - shown.length;
    caption.textContent = rest > 0
      ? `Largest ${shown.length} of ${ranked.length} categories · ${vocabulary.length.toLocaleString()} entries in total`
      : `All ${ranked.length} categories · ${vocabulary.length.toLocaleString()} entries in total`;
  }
}

// A ring, because the proportion is the point. The legend carries every count in text beside it,
// so the figure never depends on telling three colours apart.
function renderOutcomeFigure(approved, pending, rejected) {
  const host = document.getElementById('outcome-figure');
  if (!host) return;

  const total = approved + pending + rejected;
  if (total === 0) {
    host.innerHTML = submissionsLoaded
      ? `<div class="empty">
           <iconsax-icon name="clock" type="bulk" size="30" color="currentColor"></iconsax-icon>
           <b>No submissions yet</b>
           Words contributed from the app land here for review.
         </div>`
      : '<div class="donut-wrap"><div class="donut skeleton" style="border-radius:50%"></div></div>';
    return;
  }

  const R = 54;
  const C = 2 * Math.PI * R;
  const segments = [
    { label: 'Approved', value: approved, color: 'var(--green)' },
    { label: 'Awaiting review', value: pending, color: 'var(--amber)' },
    { label: 'Rejected', value: rejected, color: 'var(--red)' }
  ];

  let offset = 0;
  const arcs = segments.filter(s => s.value > 0).map(s => {
    const len = (s.value / total) * C;
    const arc = `<circle cx="66" cy="66" r="${R}" fill="none" stroke="${s.color}" stroke-width="20"
      stroke-dasharray="${len.toFixed(2)} ${(C - len).toFixed(2)}" stroke-dashoffset="${(-offset).toFixed(2)}"></circle>`;
    offset += len;
    return arc;
  }).join('');

  const pct = Math.round((approved / total) * 100);
  host.innerHTML = `
    <div class="donut-wrap">
      <div class="donut">
        <svg width="132" height="132" viewBox="0 0 132 132" role="img"
             aria-label="Of ${total} submissions, ${approved} approved, ${pending} awaiting review, ${rejected} rejected.">
          <circle cx="66" cy="66" r="${R}" fill="none" stroke="var(--sunken)" stroke-width="20"></circle>
          ${arcs}
        </svg>
        <div class="donut-centre" aria-hidden="true"><b>${pct}%</b><span>approved</span></div>
      </div>
      <div class="legend">
        ${segments.map(s => `
          <div class="legend-row">
            <span class="legend-swatch" style="background:${s.color}" aria-hidden="true"></span>
            ${escapeHtml(s.label)} <b>${s.value}</b>
          </div>`).join('')}
      </div>
    </div>`;
}

function renderRecentSubmissions() {
  const host = document.getElementById('recent-submissions');
  if (!host) return;

  if (submissions.length === 0) {
    host.innerHTML = submissionsLoaded
      ? `<div class="empty">
           <iconsax-icon name="verify" type="bulk" size="30" color="currentColor"></iconsax-icon>
           <b>Nothing submitted yet</b>
           Contributions from the app appear here as they arrive.
         </div>`
      : '<div class="item"><span class="item-mark skeleton"></span><span class="item-body"><span class="item-title skeleton">&nbsp;</span></span></div>'.repeat(5);
    return;
  }

  host.innerHTML = submissions.slice(0, 6).map(sub => {
    const status = (sub.status || 'pending');
    const word = (sub.kasiguranin || '?').trim();
    return `
      <div class="item">
        <span class="item-mark is-${escapeHtml(status)}" aria-hidden="true">${escapeHtml(word.charAt(0).toUpperCase())}</span>
        <span class="item-body">
          <span class="item-title">${escapeHtml(word)}</span>
          <span class="item-sub">${escapeHtml(status === 'pending' ? 'Awaiting review' : status === 'approved' ? 'Approved' : 'Rejected')} · ${escapeHtml(sub.contributorName || 'Anonymous')}</span>
        </span>
        <span class="item-meta">${escapeHtml(relativeTime(toMillis(sub.submittedAt || sub.createdAt)))}</span>
      </div>`;
  }).join('');
}

function renderStoryShelf() {
  const host = document.getElementById('story-list');
  if (!host) return;

  if (stories.length === 0) {
    // This collection being empty is the normal, correct condition for a project relying on the
    // app's built-in corpus, so it must not read as a failure.
    host.innerHTML = storiesLoaded
      ? `<div class="empty">
           <iconsax-icon name="document-text" type="bulk" size="30" color="currentColor"></iconsax-icon>
           <b>Reading the built-in stories</b>
           Learners have the ten stories shipped with the app. Add one here only to change that set.
         </div>`
      : '<div class="item"><span class="item-mark skeleton"></span><span class="item-body"><span class="item-title skeleton">&nbsp;</span></span></div>'.repeat(4);
    return;
  }

  host.innerHTML = stories.slice(0, 5).map(story => {
    let pages = story.totalPages || 0;
    try {
      const parsed = JSON.parse(story.pagesJson || '[]');
      if (Array.isArray(parsed)) pages = parsed.length;
    } catch (e) { /* keep totalPages */ }
    const hasKasi = Boolean((story.titleKasiguranin || '').trim());
    return `
      <div class="item">
        <span class="item-mark" aria-hidden="true">${escapeHtml(String(story.id ?? '·'))}</span>
        <span class="item-body">
          <span class="item-title">${escapeHtml(story.title || 'Untitled story')}</span>
          <span class="item-sub">${pages} page${pages === 1 ? '' : 's'} · ${escapeHtml(story.category || 'Story')}</span>
        </span>
        <span class="badge ${hasKasi ? 'badge-approved' : 'badge-pending'}">${hasKasi ? 'Written' : 'Kasiguranin pending'}</span>
      </div>`;
  }).join('');
}

function renderReleaseCard(latest) {
  const version = document.getElementById('metric-latest-version');
  const meta = document.getElementById('release-meta');
  if (!version || !meta) return;

  if (!latest) {
    version.textContent = releasesLoaded ? 'None' : '…';
    meta.textContent = releasesLoaded
      ? 'No APK has been published yet. Publish one to give learners something to install.'
      : 'Loading release history…';
    return;
  }

  version.textContent = `v${latest.versionName}`;
  const released = toMillis(latest.releasedAt);
  const when = released ? new Date(released).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : 'date not recorded';
  meta.textContent = `Build ${latest.versionCode ?? '—'} · ${when} · ${latest.forceUpdate ? 'Required update' : 'Optional update'}`;
}

// ── Render Submissions Table ────────────────────────────────────────────────
// Below 760px the tables are re-laid out as cards with `display: block`, which silently drops the
// implicit table / row / cell roles a screen reader navigates by — on exactly the devices the card
// pattern exists for. Restating the roles explicitly keeps the structure announced either way.
function applyTableSemantics(root) {
  const scope = root || document;
  scope.querySelectorAll('.table-responsive table').forEach(table => {
    table.setAttribute('role', 'table');
    table.querySelectorAll('thead, tbody').forEach(g => g.setAttribute('role', 'rowgroup'));
    table.querySelectorAll('tr').forEach(r => r.setAttribute('role', 'row'));
    table.querySelectorAll('th').forEach(c => c.setAttribute('role', 'columnheader'));
    table.querySelectorAll('td').forEach(c => c.setAttribute('role', 'cell'));
  });
}

function renderSubmissionsTable() {
  const tbody = document.getElementById('submissions-tbody');
  if (!tbody) return;
  
  tbody.innerHTML = '';
  
  if (submissions.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align:center; padding:2.5rem; color:var(--muted);">
          <iconsax-icon name="clock" type="bulk" size="32" color="var(--gold-ink)"></iconsax-icon>
          <div style="margin-top:8px;">No pending word submissions in queue.</div>
        </td>
      </tr>`;
    return;
  }

  const queueCount = document.getElementById('queue-result-count');
  if (queueCount) {
    const pending = submissions.filter(s => (s.status || 'pending') === 'pending').length;
    queueCount.textContent = pending === 0
      ? `${submissions.length} reviewed, none waiting`
      : `${pending} waiting of ${submissions.length}`;
  }

  submissions.forEach(sub => {
    const tr = document.createElement('tr');
    const statusBadgeClass = sub.status === 'approved' ? 'badge-approved' : (sub.status === 'rejected' ? 'badge-rejected' : 'badge-pending');
    
    tr.innerHTML = `
      <td data-label="Kasiguranin"><strong>${escapeHtml(sub.kasiguranin)}</strong></td>
      <td data-label="Tagalog">${escapeHtml(sub.tagalog || '-')}</td>
      <td data-label="English">${escapeHtml(sub.english || '-')}</td>
      <td data-label="Category"><span class="badge badge-category">${escapeHtml(sub.category || 'General')}</span></td>
      <td data-label="Part of speech">${sub.partOfSpeech ? `<span class="badge badge-category">${escapeHtml(sub.partOfSpeech)}</span>` : '-'}</td>
      <td data-label="Contributor">${escapeHtml(sub.contributorName || 'Anonymous')}</td>
      <td data-label="Status"><span class="badge ${statusBadgeClass}">${(sub.status || 'pending').toUpperCase()}</span></td>
      <td data-label="Actions">
        ${sub.status === 'pending' ? `
        <div class="row-actions">
          <button class="btn btn-success btn-sm approve-btn" data-id="${sub.id}"><iconsax-icon name="tick-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Approve</button>
          <button class="btn btn-danger btn-sm reject-btn" data-id="${sub.id}"><iconsax-icon name="close-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Reject</button>
        </div>
        ` : `
          <span style="color:var(--muted); font-size:0.85rem;">Processed</span>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('.approve-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      approveSubmission(btn.getAttribute('data-id'));
    });
  });
  tbody.querySelectorAll('.reject-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      rejectSubmission(btn.getAttribute('data-id'));
    });
  });

  applyTableSemantics();
}

function renderSubmissionsError(message) {
  const tbody = document.getElementById('submissions-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--status-rejected); padding:2rem;">${escapeHtml(message)}</td></tr>`;
  }
}

// ── Approve Submission ──────────────────────────────────────────────────────
async function approveSubmission(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) return;

  try {
    const newVocabRef = doc(collection(db, "vocabulary"));
    await setDoc(newVocabRef, withUpdatedAt({
      kasiguranin: sub.kasiguranin.trim(),
      tagalog: (sub.tagalog || "").trim(),
      english: (sub.english || "").trim(),
      rootForm: (sub.rootForm || sub.kasiguranin).trim(),
      category: sub.category || "General",
      partOfSpeech: sub.partOfSpeech || null,
      ipaNotation: (sub.ipaNotation || "").trim(),
      perfectiveForm: (sub.pastTense || "").trim(),
      imperfectiveForm: (sub.presentTense || "").trim(),
      contemplativeForm: (sub.futureTense || "").trim(),
      verifiedByAdmin: true,
      approvedAt: Date.now()
    }));

    await updateDoc(doc(db, "word_submissions", id), {
      status: "approved",
      reviewedAt: Date.now()
    });

    await logAudit("submission.approve", { submissionId: id, word: sub.kasiguranin });
    notify(`Successfully approved "${sub.kasiguranin}" and migrated to master dictionary!`, 'success');
  } catch (error) {
    console.error("Error approving submission:", error);
    notify("Failed to approve submission: " + error.message, 'error');
  }
}

// ── Reject Submission ───────────────────────────────────────────────────────
async function rejectSubmission(id) {
  if (!(await confirmDialog({
    title: 'Reject this submission?',
    body: 'The contributor will not see it in the dictionary. This does not delete their account or their other submissions.',
    confirmLabel: 'Reject', danger: true
  }))) return;
  try {
    await updateDoc(doc(db, "word_submissions", id), {
      status: "rejected",
      reviewedAt: Date.now()
    });
    const sub = submissions.find(s => s.id === id);
    await logAudit("submission.reject", { submissionId: id, word: sub ? sub.kasiguranin : "" });
  } catch (error) {
    console.error("Error rejecting submission:", error);
  }
}

// ── Announcements ────────────────────────────────────────────────────────────
function renderAnnouncementsList() {
  const container = document.getElementById('announcements-list');
  if (!container) return;

  const active = announcements.filter(a => a.active !== false);
  if (active.length === 0) {
    container.innerHTML = '<p style="color:var(--muted); padding:0.5rem 0;">No active announcements.</p>';
    return;
  }

  container.innerHTML = '';
  active.forEach(a => {
    const row = document.createElement('div');
    row.style.cssText = 'display:flex; justify-content:space-between; align-items:flex-start; gap:12px; padding:0.75rem 0; border-bottom:1px solid var(--border, #eee);';
    row.innerHTML = `
      <div>
        ${a.title ? `<strong>${escapeHtml(a.title)}</strong><br>` : ''}
        <span style="color:var(--muted);">${escapeHtml(a.message || '')}</span>
      </div>
      <button class="btn btn-outline btn-sm deactivate-announcement-btn" data-id="${a.id}">Deactivate</button>
    `;
    container.appendChild(row);
  });

  container.querySelectorAll('.deactivate-announcement-btn').forEach(btn => {
    btn.addEventListener('click', () => deactivateAnnouncement(btn.getAttribute('data-id')));
  });
}

async function deactivateAnnouncement(id) {
  try {
    await updateDoc(doc(db, "announcements", id), { active: false });
    await logAudit("announcement.deactivate", { announcementId: id });
  } catch (error) {
    console.error("Error deactivating announcement:", error);
    notify("Failed to deactivate: " + error.message, 'error');
  }
}

// ── Literature Submissions (stories/poems) ──────────────────────────────────
function renderLiteratureSubmissionsTable() {
  const tbody = document.getElementById('literature-submissions-tbody');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (literatureSubmissions.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" style="text-align:center; padding:2.5rem; color:var(--muted);">
          No pending story or poem submissions.
        </td>
      </tr>`;
    return;
  }

  literatureSubmissions.forEach(sub => {
    const tr = document.createElement('tr');
    const statusBadgeClass = sub.status === 'approved' ? 'badge-approved' : (sub.status === 'rejected' ? 'badge-rejected' : 'badge-pending');
    let pageCount = 0;
    try { pageCount = JSON.parse(sub.pagesJson || '[]').length; } catch (e) { pageCount = 0; }

    tr.innerHTML = `
      <td data-label="Title"><strong>${escapeHtml(sub.titleKasiguranin || sub.title || '(untitled)')}</strong></td>
      <td data-label="Pages">${pageCount}</td>
      <td data-label="Contributor">${escapeHtml(sub.contributorName || 'Anonymous')}</td>
      <td data-label="Status"><span class="badge ${statusBadgeClass}">${(sub.status || 'pending').toUpperCase()}</span></td>
      <td data-label="Actions">
        ${sub.status === 'pending' ? `
        <div class="row-actions">
          <button class="btn btn-success btn-sm lit-approve-btn" data-id="${sub.id}"><iconsax-icon name="tick-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Approve</button>
          <button class="btn btn-danger btn-sm lit-reject-btn" data-id="${sub.id}"><iconsax-icon name="close-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Reject</button>
        </div>
        ` : `
          <span style="color:var(--muted); font-size:0.85rem;">Processed</span>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('.lit-approve-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      approveLiteratureSubmission(btn.getAttribute('data-id'));
    });
  });
  tbody.querySelectorAll('.lit-reject-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      rejectLiteratureSubmission(btn.getAttribute('data-id'));
    });
  });

  applyTableSemantics();
}

function renderLiteratureSubmissionsError(message) {
  const tbody = document.getElementById('literature-submissions-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--status-rejected); padding:2rem;">${escapeHtml(message)}</td></tr>`;
  }
}

// Approving copies the submission into the real `stories` collection - the same copy-on-approve
// pattern approveSubmission() uses for words, never mutating live tables directly. A numeric id
// is picked the same way the story editor picks one for a new story: one past the current max.
async function approveLiteratureSubmission(id) {
  const sub = literatureSubmissions.find(s => s.id === id);
  if (!sub) return;

  try {
    let pages = [];
    try { pages = JSON.parse(sub.pagesJson || '[]'); } catch (e) { pages = []; }

    const maxId = storiesLoaded && stories.length
      ? Math.max(0, ...stories.map(s => Number(s.id) || 0))
      : 0;
    const newId = maxId + 1;

    await setDoc(doc(db, "stories", String(newId)), withUpdatedAt({
      id: newId,
      title: (sub.title || sub.titleKasiguranin || '').trim(),
      titleKasiguranin: (sub.titleKasiguranin || '').trim(),
      description: `Submitted by ${sub.contributorName || 'Anonymous'}`,
      category: "Community",
      iconEmoji: "📖",
      pagesJson: JSON.stringify(pages),
      totalPages: pages.length,
      requiredXp: 0,
      isUnlocked: true,
      isCompleted: false,
      currentPage: 0
    }));

    await updateDoc(doc(db, "literature_submissions", id), {
      status: "approved",
      reviewedAt: Date.now()
    });

    await logAudit("literature_submission.approve", { submissionId: id, title: sub.title || sub.titleKasiguranin });
    notify(`Approved "${sub.title || sub.titleKasiguranin}" and added it to Stories.`, 'success');
  } catch (error) {
    console.error("Error approving literature submission:", error);
    notify("Failed to approve submission: " + error.message, 'error');
  }
}

async function rejectLiteratureSubmission(id) {
  if (!(await confirmDialog({
    title: 'Reject this submission?',
    body: 'The contributor will not see it in Stories. This does not delete their other submissions.',
    confirmLabel: 'Reject', danger: true
  }))) return;
  try {
    await updateDoc(doc(db, "literature_submissions", id), {
      status: "rejected",
      reviewedAt: Date.now()
    });
    const sub = literatureSubmissions.find(s => s.id === id);
    await logAudit("literature_submission.reject", { submissionId: id, title: sub ? (sub.title || sub.titleKasiguranin) : "" });
  } catch (error) {
    console.error("Error rejecting literature submission:", error);
  }
}

// ── Render Vocabulary Table ─────────────────────────────────────────────────
// Dictionary state that survives a re-render: which page, and which column orders the list.
let vocabPage = 1;
const VOCAB_PAGE_SIZE = 50;
let vocabSort = { key: 'kasiguranin', dir: 'asc' };



window.setVocabPage = function (n) {
  vocabPage = n;
  renderVocabularyTable();
  const panel = document.getElementById('tab-vocabulary');
  if (panel) panel.scrollIntoView({ block: 'start' });
};



function renderVocabPager(total) {
  const host = document.getElementById('vocab-pager');
  if (!host) return;
  const pages = Math.ceil(total / VOCAB_PAGE_SIZE);
  if (pages <= 1) { host.innerHTML = ''; return; }

  const from = (vocabPage - 1) * VOCAB_PAGE_SIZE + 1;
  const to = Math.min(vocabPage * VOCAB_PAGE_SIZE, total);

  // A window of pages around the current one, so 40 pages do not produce 40 buttons.
  const nums = [];
  for (let i = 1; i <= pages; i++) {
    if (i === 1 || i === pages || Math.abs(i - vocabPage) <= 1) nums.push(i);
    else if (nums[nums.length - 1] !== '...') nums.push('...');
  }

  host.innerHTML = `
    <span class="result-count">Showing ${from}–${to} of ${total}</span>
    <div class="pager-pages">
      <button onclick="window.setVocabPage(${vocabPage - 1})" ${vocabPage === 1 ? 'disabled' : ''} aria-label="Previous page">‹</button>
      ${nums.map(n => n === '...'
        ? '<button disabled>…</button>'
        : `<button onclick="window.setVocabPage(${n})" ${n === vocabPage ? 'aria-current="page"' : ''}>${n}</button>`).join('')}
      <button onclick="window.setVocabPage(${vocabPage + 1})" ${vocabPage === pages ? 'disabled' : ''} aria-label="Next page">›</button>
    </div>
  `;
}

function renderVocabularyError(message) {
  const tbody = document.getElementById('vocabulary-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--status-rejected); padding:2rem;">${escapeHtml(message)}</td></tr>`;
  }
}

// ── Delete Vocabulary Word ──────────────────────────────────────────────────
window.deleteVocabWord = async function(id) {
  if (!(await confirmDialog({
    title: 'Delete this word?',
    body: 'It is removed from the master dictionary and stops reaching learners on their next sync.',
    confirmLabel: 'Delete word', danger: true
  }))) return;
  try {
    await deleteDoc(doc(db, "vocabulary", id));
    await logAudit("vocabulary.delete", { id });
  } catch (e) {
    notify("Error deleting word: " + e.message, 'error');
  }
};

// ── Edit Vocabulary Word ────────────────────────────────────────────────────
window.openEditVocabModal = function(id) {
  const item = vocabulary.find(v => v.id === id);
  if (!item) {
    console.error('Edit: item not found for id', id);
    return;
  }

  document.getElementById('edit-input-id').value = id;
  document.getElementById('edit-input-kasiguranin').value = item.kasiguranin || '';
  document.getElementById('edit-input-tagalog').value = item.tagalog || '';
  document.getElementById('edit-input-english').value = item.english || '';
  document.getElementById('edit-input-category').value = item.category || 'Greetings & Essentials';
  document.getElementById('edit-input-part-of-speech').value = item.partOfSpeech || '';
  document.getElementById('edit-input-ipa').value = item.ipaNotation || '';
  document.getElementById('edit-input-neutral').value = item.neutralForm || '';
  document.getElementById('edit-input-perfective').value = item.perfectiveForm || '';
  document.getElementById('edit-input-imperfective').value = item.imperfectiveForm || '';
  document.getElementById('edit-input-contemplative').value = item.contemplativeForm || '';
  document.getElementById('edit-input-example1').value = item.exampleSentence || '';
  document.getElementById('edit-input-example1-translation').value = item.exampleTranslation || '';
  document.getElementById('edit-input-example2').value = item.exampleSentence2 || '';
  document.getElementById('edit-input-example2-translation').value = item.exampleTranslation2 || '';

  window.openModal('edit-vocab-modal');
};

// ── SQL Importer ────────────────────────────────────────────────────────────
function initSqlImporter() {
  const dropzone = document.getElementById('sql-dropzone');
  const fileInput = document.getElementById('sql-file-input');

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener('click', () => fileInput.click());
  dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) handleSqlFile(e.dataTransfer.files[0]);
  });
  fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) handleSqlFile(e.target.files[0]);
  });
}

function handleSqlFile(file) {
  const reader = new FileReader();
  reader.onload = async (e) => {
    try {
      const text = e.target.result;
      // 8 required columns + up to 4 optional trailing aspect columns
      // (neutral, imperfective, perfective, contemplative).
      const insertRegex = /INSERT INTO vocabulary[^(]*\([^)]+\)\s*VALUES\s*\(\s*'([^']+)'\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')\s*,\s*(NULL|'[^']*')(?:\s*,\s*(NULL|'[^']*'))*(?:\s*,\s*\d+)?\s*\);/g;

      const entries = [];
      let match;

      while ((match = insertRegex.exec(text)) !== null) {
        const clean = (val) => {
          if (!val || val === 'nan' || val === 'NULL' || val === "''") return null;
          if (val.startsWith("'") && val.endsWith("'")) return val.slice(1, -1).trim() || null;
          return val.trim();
        };

        entries.push({
          kasiguranin: match[1].trim(),
          tagalog: clean(match[2]),
          english: clean(match[3]),
          rootWord: clean(match[4]),
          partOfSpeech: clean(match[5]),
          category: clean(match[6]) || 'General',
          audioFile: clean(match[7]),
          sampleSentence: clean(match[8]),
          neutralForm: clean(match[9]),
          imperfectiveForm: clean(match[10]),
          perfectiveForm: clean(match[11]),
          contemplativeForm: clean(match[12]),
          createdAt: Date.now()
        });
      }

      if (entries.length === 0) {
        notify(`No valid INSERT INTO vocabulary statements found in "${file.name}"!`, 'error');
        return;
      }

      if (!(await confirmDialog({
        title: `Import ${entries.length} records?`,
        body: `Parsed from <strong>${escapeHtml(file.name)}</strong>. Existing entries with the same id are overwritten.`,
        confirmLabel: 'Import'
      }))) return;

      let count = 0;
      for (const entry of entries) {
        const newDoc = doc(collection(db, "vocabulary"));
        // Bulk-imported rows are stamped like any other write, or a spreadsheet import
        // would land in Firestore invisible to the app's incremental sync.
        await setDoc(newDoc, withUpdatedAt(entry));
        count++;
      }

      notify(`Successfully imported ${count} entries from SQL migration script into Firestore!`, 'success');
    } catch (err) {
      console.error("SQL Parsing Error:", err);
      notify("Failed to parse SQL file: " + err.message, 'error');
    }
  };
  reader.readAsText(file);
}

// ── Excel Importer ──────────────────────────────────────────────────────────
function initExcelImporter() {
  const dropzone = document.getElementById('excel-dropzone');
  const fileInput = document.getElementById('excel-file-input');

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener('click', () => fileInput.click());
  dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) handleExcelFile(e.dataTransfer.files[0]);
  });
  fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) handleExcelFile(e.target.files[0]);
  });
}

function handleExcelFile(file) {
  const reader = new FileReader();
  reader.onload = async (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]];

      // --- Auto-detect the header row ---
      // The spreadsheet may have blank rows above the actual headers.
      // We scan all rows (as raw arrays) to find the first row that contains
      // a known column name, then re-parse using that row as the header.
      const knownHeaders = ['kasiguranin', 'kasiguranin word', 'word', 'entry', 'english', 'tagalog'];
      const allRows = XLSX.utils.sheet_to_json(firstSheet, { header: 1 }); // raw arrays
      let headerRowIndex = 0;
      for (let i = 0; i < allRows.length; i++) {
        const cellValues = (allRows[i] || []).map(c => String(c).trim().toLowerCase());
        if (cellValues.some(v => knownHeaders.includes(v))) {
          headerRowIndex = i;
          break;
        }
      }

      // Re-parse with the correct header row
      const rawRows = XLSX.utils.sheet_to_json(firstSheet, { range: headerRowIndex });

      if (rawRows.length === 0) {
        notify("The selected Excel file contains no data rows! Make sure your header row includes a column named KASIGURANIN, ENGLISH, or TAGALOG.", 'error');
        return;
      }

      if (!(await confirmDialog({
        title: `Import ${rawRows.length} rows?`,
        body: `Parsed from <strong>${escapeHtml(file.name)}</strong> into the vocabulary collection.`,
        confirmLabel: 'Import'
      }))) return;

      let count = 0;
      let skipped = 0;
      let batches = [];
      let currentBatch = writeBatch(db);
      let operationsInCurrentBatch = 0;
      
      const existingWords = new Set(vocabulary.map(v => (v.kasiguranin || '').toLowerCase()));
      const wordsInThisImport = new Set(); 

      for (const rawRow of rawRows) {
        const row = {};
        Object.keys(rawRow).forEach(key => {
          row[key.trim().toLowerCase()] = rawRow[key];
        });

        const kasiguranin = row.kasiguranin || row['kasiguranin word'] || row.word || row.entry;
        if (!kasiguranin) continue;

        const wordClean = String(kasiguranin).trim();
        if (!wordClean) continue;
        
        const wordLower = wordClean.toLowerCase();
        
        if (existingWords.has(wordLower) || wordsInThisImport.has(wordLower)) {
          skipped++;
          continue;
        }
        
        wordsInThisImport.add(wordLower);

        const newDoc = doc(collection(db, "vocabulary"));
        currentBatch.set(newDoc, {
          kasiguranin: wordClean,
          tagalog: String(row.tagalog || "").trim() || null,
          english: String(row.english || "").trim() || null,
          category: String(row.category || "General").trim(),
          ipaNotation: String(row.ipa || row.ipanotation || "").trim() || null,
          importedFromExcel: file.name,
          createdAt: Date.now()
        });
        
        count++;
        operationsInCurrentBatch++;
        
        if (operationsInCurrentBatch === 490) {
          batches.push(currentBatch);
          currentBatch = writeBatch(db);
          operationsInCurrentBatch = 0;
        }
      }

      if (operationsInCurrentBatch > 0) {
        batches.push(currentBatch);
      }

      if (batches.length > 0) {
        notify(`Importing ${count} new entries (skipping ${skipped} duplicates)...`, 'success');
        for (const batch of batches) {
          await batch.commit();
        }
        notify(`Successfully imported ${count} new Kasiguranin entries! Skipped ${skipped} duplicates.`, 'success');
      } else {
        notify(`No new entries to import. Skipped ${skipped} duplicates.`, 'success');
      }
    } catch (err) {
      console.error("Excel Parsing Error:", err);
      notify("Failed to parse Excel file: " + err.message, 'error');
    }
  };
  reader.readAsArrayBuffer(file);
}

// ── Render Releases List ────────────────────────────────────────────────────


// ── Form Listeners ──────────────────────────────────────────────────────────
function initFormListeners() {
  const announcementForm = document.getElementById('announcement-form');
  if (announcementForm) {
    announcementForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const title = document.getElementById('input-announcement-title').value.trim();
      const message = document.getElementById('input-announcement-message').value.trim();
      if (!message) { notify("Please enter a message.", 'error'); return; }

      try {
        const ref = doc(collection(db, "announcements"));
        await setDoc(ref, {
          id: ref.id,
          title,
          message,
          active: true,
          createdAt: Date.now()
        });
        await logAudit("announcement.post", { announcementId: ref.id, title });
        notify("Announcement posted. It's live in the app now.", 'success');
        announcementForm.reset();
        closeModal('announcement-modal');
      } catch (error) {
        console.error("Error posting announcement:", error);
        notify("Failed to post announcement: " + error.message, 'error');
      }
    });
  }

  const addVocabForm = document.getElementById('add-vocab-form');
  if (addVocabForm) {
    addVocabForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const word = document.getElementById('input-kasiguranin').value.trim();
      const tagalog = document.getElementById('input-tagalog').value.trim();
      const english = document.getElementById('input-english').value.trim();
      const category = document.getElementById('input-category').value;
      const partOfSpeech = document.getElementById('input-part-of-speech').value;
      const ipa = document.getElementById('input-ipa').value.trim();
      const neutral = document.getElementById('input-neutral').value.trim();
      const perfective = document.getElementById('input-perfective').value.trim();
      const imperfective = document.getElementById('input-imperfective').value.trim();
      const contemplative = document.getElementById('input-contemplative').value.trim();
      const example1 = document.getElementById('input-example1').value.trim();
      const example1Translation = document.getElementById('input-example1-translation').value.trim();
      const example2 = document.getElementById('input-example2').value.trim();
      const example2Translation = document.getElementById('input-example2-translation').value.trim();

      if (!word) { notify("Please enter the Kasiguranin word.", 'error'); return; }

      const isDuplicate = vocabulary.some(v => (v.kasiguranin || '').toLowerCase() === word.toLowerCase());
      if (isDuplicate) {
        if (!(await confirmDialog({
          title: `"${word}" already exists`,
          body: 'The master dictionary already has this Kasiguranin word. Adding it again creates a duplicate entry.',
          confirmLabel: 'Add anyway'
        }))) return;
      }

      try {
        await addDoc(collection(db, "vocabulary"), {
          kasiguranin: word,
          tagalog: tagalog || null,
          english: english || null,
          category: category,
          partOfSpeech: partOfSpeech || null,
          ipaNotation: ipa || null,
          neutralForm: neutral || null,
          perfectiveForm: perfective || null,
          imperfectiveForm: imperfective || null,
          contemplativeForm: contemplative || null,
          exampleSentence: example1 || null,
          exampleTranslation: example1Translation || null,
          exampleSentence2: example2 || null,
          exampleTranslation2: example2Translation || null,
          createdAt: Date.now(),
          // A new word needs updatedAt too, not just createdAt — the app's incremental
          // sync filters on updatedAt, so without it a freshly added word would not
          // reach anyone until the next weekly full reconcile.
          updatedAt: Date.now()
        });
        await logAudit("vocabulary.create", { word });
        addVocabForm.reset();
        closeModal('add-vocab-modal');
        notify(`Successfully added "${word}" to dictionary!`, 'success');
      } catch (error) {
        notify("Error adding word: " + error.message, 'error');
      }
    });
  }

  const editVocabForm = document.getElementById('edit-vocab-form');
  if (editVocabForm) {
    editVocabForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('edit-input-id').value;
      const word = document.getElementById('edit-input-kasiguranin').value.trim();
      const tagalog = document.getElementById('edit-input-tagalog').value.trim();
      const english = document.getElementById('edit-input-english').value.trim();
      const category = document.getElementById('edit-input-category').value;
      const partOfSpeech = document.getElementById('edit-input-part-of-speech').value;
      const ipa = document.getElementById('edit-input-ipa').value.trim();
      const neutral = document.getElementById('edit-input-neutral').value.trim();
      const perfective = document.getElementById('edit-input-perfective').value.trim();
      const imperfective = document.getElementById('edit-input-imperfective').value.trim();
      const contemplative = document.getElementById('edit-input-contemplative').value.trim();
      const example1 = document.getElementById('edit-input-example1').value.trim();
      const example1Translation = document.getElementById('edit-input-example1-translation').value.trim();
      const example2 = document.getElementById('edit-input-example2').value.trim();
      const example2Translation = document.getElementById('edit-input-example2-translation').value.trim();

      if (!word) { notify("Please enter the Kasiguranin word.", 'error'); return; }

      try {
        await updateDoc(doc(db, "vocabulary", id), {
          kasiguranin: word,
          tagalog: tagalog || null,
          english: english || null,
          category: category,
          partOfSpeech: partOfSpeech || null,
          ipaNotation: ipa || null,
          neutralForm: neutral || null,
          perfectiveForm: perfective || null,
          imperfectiveForm: imperfective || null,
          contemplativeForm: contemplative || null,
          exampleSentence: example1 || null,
          exampleTranslation: example1Translation || null,
          exampleSentence2: example2 || null,
          exampleTranslation2: example2Translation || null,
          updatedAt: Date.now()
        });
        await logAudit("vocabulary.update", { id, word });
        editVocabForm.reset();
        closeModal('edit-vocab-modal');
        notify(`Successfully updated "${word}"!`, 'success');
      } catch (error) {
        notify("Error updating word: " + error.message, 'error');
      }
    });
  }

  const releaseForm = document.getElementById('publish-release-form');
  if (releaseForm) {
    releaseForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const code = parseInt(document.getElementById('rel-code').value);
      const name = document.getElementById('rel-name').value.trim();
      const url = document.getElementById('rel-url').value.trim();
      const notes = document.getElementById('rel-notes').value.trim();
      const forceUpdate = document.getElementById('rel-force')?.checked || false;

      if (isNaN(code) || code <= 0) { notify("Please enter a valid positive integer version code (e.g. 1, 2, 3).", 'error'); return; }
      if (!name) { notify("Please enter a version name (e.g. 1.0.0).", 'error'); return; }
      if (!url.startsWith('http://') && !url.startsWith('https://')) { notify("Direct APK Download link must start with http:// or https://", 'error'); return; }
      if (forceUpdate && !(await confirmDialog({
        title: `Publish v${name} as a required update?`,
        body: 'Every user sees a banner they cannot dismiss until they update.',
        confirmLabel: 'Publish required update', danger: true
      }))) return;

      try {
        // Deterministic doc id (vX.Y.Z), matching what CI's publish_release.js writes for the
        // same version — setDoc + merge means republishing a version CI already wrote only
        // overwrites these six known fields instead of creating a second, duplicate doc via
        // addDoc's random id.
        await setDoc(doc(db, "app_releases", `v${name}`), {
          versionCode: code,
          versionName: name,
          apkUrl: url,
          releaseNotes: notes,
          forceUpdate: forceUpdate,
          releasedAt: Date.now()
        }, { merge: true });
        await logAudit("release.publish", { versionCode: code, versionName: name, apkUrl: url, forceUpdate: forceUpdate });
        releaseForm.reset();
        notify(`Successfully published KasiGuru v${name} APK release!`, 'success');
      } catch (err) {
        notify("Failed to publish release: " + err.message, 'error');
      }
    });
  }

  const searchInput = document.getElementById('search-vocab-input');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(() => {
        vocabPage = 1;
        vocabLetter = '';
        renderVocabularyTable();
      }, 300);
    });
  }

  const categoryFilter = document.getElementById('filter-vocab-category');
  if (categoryFilter) {
    categoryFilter.addEventListener('change', () => {
      vocabPage = 1;
      vocabLetter = '';
      renderVocabularyTable();
    });
  }

  const inputEnglish = document.getElementById('input-english');
  const inputPos = document.getElementById('input-part-of-speech');
  if (inputEnglish && inputPos) {
    inputEnglish.addEventListener('input', () => {
      if (!inputPos.value) {
        const guessed = guessPOS(inputEnglish.value);
        if (guessed) inputPos.value = guessed;
      }
    });
  }

  const editInputEnglish = document.getElementById('edit-input-english');
  const editInputPos = document.getElementById('edit-input-part-of-speech');
  if (editInputEnglish && editInputPos) {
    editInputEnglish.addEventListener('input', () => {
      if (!editInputPos.value) {
        const guessed = guessPOS(editInputEnglish.value);
        if (guessed) editInputPos.value = guessed;
      }
    });
  }
}

// Nothing depends on this animation having run: the row is replaced by the next Firestore
// snapshot regardless, and under reduced motion the class simply expires immediately.
function markRowLeaving(btn) {
  const row = btn.closest('tr');
  if (!row) return;
  row.classList.add('row-leaving');
  row.querySelectorAll('button').forEach(b => { b.disabled = true; });
}

// ── Dictionary controls ─────────────────────────────────────────────────────
// The entry list has no column headers to click, so sorting is an explicit control. Ascending is
// the only direction that makes sense for an alphabetical dictionary, so the toggle is gone.
function initDictionaryControls() {
  const sortSelect = document.getElementById('sort-vocab');
  if (sortSelect) {
    sortSelect.addEventListener('change', () => {
      vocabSort = { key: sortSelect.value, dir: 'asc' };
      vocabPage = 1;
      renderVocabularyTable();
    });
  }
}

// ── Topbar ──────────────────────────────────────────────────────────────────
// The search field is not a second search: it hands its term to the dictionary's own filter and
// opens that tab, so there is one place a lookup can be, and the URL still says where you are.
function initTopbar() {
  const email = auth.currentUser ? auth.currentUser.email : '';
  const mail = document.getElementById('topbar-email');
  if (mail) {
    mail.textContent = email || 'Signed in';
    mail.title = email || '';
  }
  const avatar = document.getElementById('topbar-avatar');
  if (avatar) avatar.textContent = (email.charAt(0) || 'A').toUpperCase();

  const global = document.getElementById('global-search');
  const target = document.getElementById('search-vocab-input');
  if (!global || !target) return;

  global.addEventListener('input', () => {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => {
      target.value = global.value;
      vocabPage = 1;
      if (!document.getElementById('tab-vocabulary')?.classList.contains('active')) {
        window.switchTab('tab-vocabulary');
      }
      renderVocabularyTable();
    }, 300);
  });

  // "/" is the convention for jumping to search, and it must not fire while you are typing
  // somewhere else.
  document.addEventListener('keydown', (e) => {
    if (e.key !== '/' || e.metaKey || e.ctrlKey || e.altKey) return;
    const tag = (e.target.tagName || '').toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target.isContentEditable) return;
    e.preventDefault();
    global.focus();
    global.select();
  });
}

// ── Helpers ─────────────────────────────────────────────────────────────────
function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}




// ── Admin Audit Log ─────────────────────────────────────────────────────────
// Append-only record of admin actions (rules: admins create/read, never update/delete).
async function logAudit(action, details = {}) {
  try {
    const actor = (auth.currentUser && auth.currentUser.email) || "unknown";
    await addDoc(collection(db, "admin_audit_log"), {
      actor,
      action,
      details,
      timestamp: Date.now()
    });
  } catch (e) {
    console.warn("Audit log write failed:", e);
  }
}

window.checkPOS = function() {
  if (vocabulary.length === 0) {
    notify("Dictionary is empty, nothing to check.", 'error');
    return;
  }
  let posCount = 0;
  vocabulary.forEach(v => {
    if (v.partOfSpeech && v.partOfSpeech.trim() !== '') posCount++;
  });
  
  const percentage = Math.round((posCount / vocabulary.length) * 100);
  notify(`Parts of Speech Check: ${posCount} out of ${vocabulary.length} words (${percentage}%) have a Part of Speech set.`, 'success');
};

function guessPOS(eng) {
  if (!eng) return null;
  eng = eng.trim().toLowerCase();
  
  if (eng.startsWith('to ')) return 'Verb';
  if (eng.startsWith('a ') || eng.startsWith('an ') || eng.startsWith('the ')) return 'Noun';
  if (eng.endsWith('ly')) return 'Adverb';
  
  if (['i', 'you', 'he', 'she', 'it', 'we', 'they', 'me', 'him', 'her', 'us', 'them', 'this', 'that', 'these', 'those'].includes(eng)) return 'Pronoun';
  if (['and', 'but', 'or', 'so', 'because', 'although', 'if', 'when', 'while'].includes(eng)) return 'Conjunction / Connector';
  if (['in', 'on', 'at', 'by', 'for', 'with', 'about', 'against', 'between', 'into', 'through', 'during', 'before', 'after', 'above', 'below', 'to', 'from', 'up', 'down', 'of', 'over', 'under', 'again', 'without'].includes(eng)) return 'Preposition';
  if (['oh', 'ah', 'wow', 'ouch', 'hey', 'alas', 'yes', 'no'].includes(eng) || eng.endsWith('!')) return 'Interjection';
  if (eng.match(/^(very|really|quite|too|so|enough|just|almost|only)$/)) return 'Adverb';
  if (eng.match(/^(what|who|where|when|why|how)$/)) return 'Pronoun';

  // Common Adjective Suffixes
  if (eng.endsWith('ful') || eng.endsWith('less') || eng.endsWith('ous') || eng.endsWith('ish') || eng.endsWith('ive') || eng.endsWith('able') || eng.endsWith('ible')) {
    return 'Adjective';
  }

  // Default to Noun for anything that isn't caught by the above rules
  return 'Noun';
}

window.autoAssignPOS = async function() {
  if (vocabulary.length === 0) {
    notify("Dictionary is empty, nothing to process.", 'error');
    return;
  }
  
  if (!(await confirmDialog({
    title: `Auto-Assign Parts of Speech?`,
    body: `This will scan all ${vocabulary.length} words and automatically assign a Part of Speech based on their English translation if they don't have one. Proceed?`,
    confirmLabel: 'Auto-Assign POS'
  }))) return;

  let updatedCount = 0;
  let batches = [];
  let currentBatch = writeBatch(db);
  let operations = 0;

  for (const v of vocabulary) {
    if (v.partOfSpeech && v.partOfSpeech.trim() !== '') continue;
    if (!v.english) continue;

    const pos = guessPOS(v.english);
    if (pos) {
      currentBatch.update(doc(db, "vocabulary", v.id), { partOfSpeech: pos, updatedAt: Date.now() });
      updatedCount++;
      operations++;

      if (operations === 490) {
        batches.push(currentBatch);
        currentBatch = writeBatch(db);
        operations = 0;
      }
    }
  }

  if (operations > 0) {
    batches.push(currentBatch);
  }

  if (updatedCount === 0) {
    notify("No new Parts of Speech could be automatically assigned.", 'success');
    return;
  }

  notify(`Auto-Assigning Part of Speech for ${updatedCount} words...`, 'success');
  for (const batch of batches) {
    await batch.commit();
  }
  
  await logAudit("vocabulary.auto_assign_pos", { count: updatedCount });
  notify(`Successfully assigned Part of Speech to ${updatedCount} words!`, 'success');
};

window.autoCategorizeWords = async function() {
  if (vocabulary.length === 0) {
    notify("Dictionary is empty, nothing to categorize.", 'error');
    return;
  }
  
  if (!(await confirmDialog({
    title: `Auto-Categorize Dictionary?`,
    body: `This will scan all ${vocabulary.length} words and automatically assign them to a category based on their English translation. Existing categories may be overwritten. Proceed?`,
    confirmLabel: 'Auto-Categorize'
  }))) return;

  // ── Expanded keyword lists based on actual dictionary contents ──
  const bodyWords = [
    'arm', 'armpit', 'back', 'beard', 'belly', 'bile', 'blood', 'body', 'bone', 'brain', 'breast',
    'buttocks', 'cheek', 'chest', 'chin', 'ear', 'earwax', 'elbow', 'eye', 'eyebrow', 'eyelash',
    'face', 'finger', 'fingernail', 'foot', 'forehead', 'hair', 'hand', 'head', 'heart', 'heel',
    'intestines', 'jaw', 'kidney', 'knee', 'leg', 'lip', 'liver', 'lung', 'mouth', 'muscle',
    'nail', 'neck', 'nose', 'palm', 'rib', 'shoulder', 'skin', 'skull', 'sole', 'stomach',
    'temple', 'thigh', 'throat', 'thumb', 'toe', 'tongue', 'tooth', 'vein', 'waist', 'wrist',
    'ankle', 'nape', 'navel', 'gills', 'fur', 'mustache', 'saliva', 'pus', 'urine',
    'excrement', 'penis', 'vagina', 'erection', 'corpse', 'bald', 'curly', 'guts', 'swollen',
    'cough', 'sneeze', 'burp', 'yawn', 'breath', 'medicine', 'healthy', 'pain', 'itch', 'blind',
    'deaf', 'spit', 'flatulence', 'vommit', 'sweat', 'tear', 'disease', 'wound', 'scar', 'fever',
    'pregnant', 'birth', 'die', 'dead', 'skeleton', 'spine', 'pelvis', 'tendon', 'ligament',
    'artery', 'organ', 'bladder', 'colon', 'pancreas', 'spleen', 'tonsil', 'gland', 'nerve',
    'marrow', 'anus', 'rectum', 'womb', 'fetus', 'embryo', 'placenta', 'umbilical', 'molar',
    'canine', 'incisor', 'gum', 'palate', 'larynx', 'trachea', 'esophagus', 'diaphragm',
    'pupil', 'iris', 'retina', 'cornea', 'eardrum', 'nostril', 'dimple', 'wrinkle', 'pore',
    'freckle', 'mole', 'callus', 'blister', 'bruise', 'rash', 'boil', 'pimple', 'wart',
    'heal', 'cure', 'remedy', 'symptom', 'infection', 'inflammation', 'fracture', 'sprain',
    'diarrhea', 'constipation', 'nausea', 'headache', 'toothache', 'stomachache', 'backache',
    'vomit', 'bleed', 'faint', 'dizzy', 'numb', 'paralyzed', 'limp', 'lame', 'mute',
    'handicapped', 'disabled', 'physician', 'doctor', 'nurse', 'patient', 'surgery', 'injection',
    'bandage', 'ointment', 'tablet', 'capsule', 'syrup', 'herb', 'potion', 'antidote'
  ];

  const animalsWords = [
    'animal', 'ant', 'bird', 'butterfly', 'chick', 'chicken', 'cockroach', 'crab', 'crocodile',
    'crow', 'deer', 'dog', 'eel', 'fish', 'fishbone', 'frog', 'goat', 'hawk', 'insect', 'lizard',
    'monkey', 'mosquito', 'mouse', 'pig', 'rat', 'shark', 'shrimp', 'snake', 'spider', 'turtle',
    'worm', 'louse', 'octopus', 'squid', 'water buffalo', 'nest', 'whale', 'cow', 'cat',
    'horse', 'sheep', 'duck', 'goose', 'tiger', 'lion', 'bear', 'eagle', 'owl', 'bee', 'wasp',
    'beetle', 'elephant', 'giraffe', 'ape', 'gorilla', 'tortoise', 'clam', 'oyster', 'snail',
    'slug', 'leech', 'cricket', 'grasshopper', 'dragonfly', 'firefly', 'centipede', 'millipede',
    'scorpion', 'termite', 'tick', 'flea', 'maggot', 'larva', 'caterpillar', 'cocoon', 'chrysalis',
    'moth', 'hornet', 'locust', 'cicada', 'mantis', 'praying mantis', 'ladybug',
    'parrot', 'pigeon', 'dove', 'sparrow', 'swallow', 'heron', 'pelican', 'flamingo', 'penguin',
    'stork', 'crane', 'vulture', 'falcon', 'kite', 'robin', 'woodpecker', 'kingfisher',
    'rooster', 'hen', 'turkey', 'quail', 'pheasant', 'peacock', 'ostrich',
    'carabao', 'buffalo', 'ox', 'bull', 'calf', 'mare', 'stallion', 'foal', 'donkey', 'mule',
    'lamb', 'ram', 'ewe', 'kid', 'piglet', 'sow', 'boar', 'puppy', 'kitten',
    'rabbit', 'hare', 'squirrel', 'hedgehog', 'porcupine', 'skunk', 'fox', 'wolf',
    'hyena', 'leopard', 'panther', 'cheetah', 'jaguar', 'rhino', 'rhinoceros', 'hippo',
    'hippopotamus', 'zebra', 'antelope', 'gazelle', 'moose', 'elk', 'caribou', 'bison',
    'kangaroo', 'koala', 'platypus', 'armadillo', 'sloth', 'raccoon', 'badger', 'otter',
    'beaver', 'weasel', 'ferret', 'mink', 'seal', 'walrus', 'manatee', 'dolphin', 'porpoise',
    'stingray', 'jellyfish', 'starfish', 'sea urchin', 'sea cucumber', 'seahorse', 'coral',
    'lobster', 'crawfish', 'crayfish', 'prawn', 'mussel', 'scallop', 'conch', 'abalone',
    'salmon', 'tuna', 'trout', 'catfish', 'bass', 'carp', 'cod', 'herring', 'sardine',
    'mackerel', 'swordfish', 'barracuda', 'piranha', 'goldfish', 'guppy',
    'alligator', 'iguana', 'chameleon', 'gecko', 'salamander', 'newt', 'toad', 'tadpole',
    'cobra', 'viper', 'python', 'boa', 'anaconda', 'rattlesnake',
    'animal', 'creature', 'beast', 'wildlife', 'predator', 'prey', 'herbivore', 'carnivore',
    'omnivore', 'mammal', 'reptile', 'amphibian', 'crustacean', 'mollusk', 'arachnid',
    'livestock', 'poultry', 'cattle', 'flock', 'herd', 'swarm', 'colony',
    'horn', 'hoof', 'claw', 'talon', 'fang', 'tusk', 'antler', 'mane', 'feather',
    'wing', 'beak', 'bill', 'snout', 'muzzle', 'paw', 'whisker', 'shell', 'scale',
    'cockatoo', 'toucan', 'seagull', 'albatross', 'raven', 'magpie', 'jay',
    'chimpanzee', 'orangutan', 'baboon', 'lemur', 'gibbon',
    'bat', 'flying fox', 'pangolin', 'mongoose', 'civet', 'tapir',
    'pig', 'hog', 'swine'
  ];

  const foodWords = [
    'bitter', 'coconut', 'egg', 'eggplant', 'food', 'fruit', 'honey', 'meat', 'milk', 'salt',
    'salty', 'sugar', 'sugarcane', 'sweet', 'sour', 'water', 'rice', 'banana', 'cassava', 'taro',
    'yam', 'drink', 'eat', 'cook', 'grater', 'soup', 'ginger', 'hungry', 'thirsty',
    'full after eating', 'boil', 'pork', 'beef', 'spicy', 'bland', 'tasty', 'delicious',
    'meal', 'snack', 'breakfast', 'lunch', 'dinner', 'vegetable', 'vegetables', 'garlic', 'onion',
    'pepper', 'chili', 'tomato', 'potato', 'corn', 'maize', 'wheat', 'flour', 'bread', 'noodle',
    'pasta', 'bean', 'pea', 'lentil', 'nut', 'peanut', 'almond', 'cashew', 'walnut',
    'mango', 'papaya', 'pineapple', 'guava', 'melon', 'watermelon', 'grape', 'apple',
    'orange', 'lemon', 'lime', 'cherry', 'peach', 'pear', 'plum', 'berry', 'strawberry',
    'blueberry', 'raspberry', 'avocado', 'olive', 'fig', 'date', 'pomegranate', 'kiwi',
    'jackfruit', 'durian', 'lychee', 'rambutan', 'mangosteen', 'starfruit', 'dragonfruit',
    'breadfruit', 'plantain', 'persimmon', 'tamarind', 'passion fruit', 'calamansi',
    'cabbage', 'lettuce', 'spinach', 'kale', 'celery', 'carrot', 'radish', 'turnip', 'beet',
    'squash', 'pumpkin', 'cucumber', 'zucchini', 'okra', 'asparagus', 'broccoli', 'cauliflower',
    'mushroom', 'bamboo shoot', 'sprout', 'watercress', 'parsley', 'basil', 'cilantro',
    'mint', 'oregano', 'thyme', 'rosemary', 'bay leaf', 'cinnamon', 'clove', 'nutmeg',
    'turmeric', 'cumin', 'coriander', 'cardamom', 'anise', 'fennel', 'dill', 'saffron',
    'vinegar', 'soy sauce', 'fish sauce', 'oyster sauce', 'ketchup', 'mustard', 'mayonnaise',
    'oil', 'butter', 'cheese', 'cream', 'yogurt', 'tofu', 'tempeh',
    'stew', 'broth', 'porridge', 'congee', 'gruel', 'cereal', 'oatmeal',
    'roast', 'grill', 'fry', 'bake', 'steam', 'stir-fry', 'braise', 'simmer', 'sauté',
    'slice', 'dice', 'chop', 'mince', 'peel', 'grate', 'blend', 'knead', 'ferment',
    'pickle', 'preserve', 'smoke', 'dry', 'marinate', 'season', 'spice',
    'feast', 'banquet', 'recipe', 'ingredient', 'dish', 'course', 'serving', 'portion',
    'appetite', 'flavor', 'taste', 'aroma', 'savory', 'umami', 'ripe', 'unripe', 'raw',
    'cooked', 'overcooked', 'undercooked', 'burnt', 'stale', 'fresh', 'rotten', 'spoiled',
    'juice', 'tea', 'coffee', 'wine', 'beer', 'liquor', 'alcohol', 'vinegar',
    'fish', 'shrimp', 'crab', 'lobster', 'squid', 'prawn', 'seafood',
    'chicken', 'duck', 'goat', 'lamb', 'venison',
    'supper', 'brunch', 'refreshment', 'beverage', 'morsel', 'crumb', 'leftovers',
    'pantry', 'kitchen', 'stove', 'oven', 'pan', 'pot', 'wok', 'skillet', 'cauldron',
    'feed', 'nourish', 'starve', 'famish', 'gobble', 'devour', 'nibble', 'chew', 'bite',
    'sip', 'gulp', 'swallow', 'digest', 'burp', 'vomit'
  ];

  const numbersWords = [
    'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
    'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen',
    'eighteen', 'nineteen', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy',
    'eighty', 'ninety', 'hundred', 'thousand', 'million', 'billion', 'zero', 'half',
    'quarter', 'third', 'fourth', 'fifth', 'sixth', 'seventh', 'eighth', 'ninth', 'tenth',
    'dozen', 'pair', 'double', 'triple', 'single',
    'day', 'daytime', 'night', 'year', 'morning', 'afternoon', 'evening', 'month', 'week',
    'today', 'tomorrow', 'yesterday', 'first', 'last', 'once', 'often', 'now', 'count',
    'many', 'few', 'some', 'second', 'minute', 'hour', 'always', 'never', 'sometimes',
    'dawn', 'dusk', 'twilight', 'midnight', 'noon', 'midday', 'sunrise', 'sunset',
    'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday',
    'january', 'february', 'march', 'april', 'may', 'june', 'july', 'august',
    'september', 'october', 'november', 'december',
    'season', 'century', 'decade', 'fortnight', 'era', 'epoch', 'age',
    'early', 'late', 'soon', 'recently', 'formerly', 'meanwhile', 'eventually',
    'daily', 'weekly', 'monthly', 'yearly', 'annual', 'biweekly',
    'clock', 'calendar', 'schedule', 'date', 'time', 'duration', 'period', 'interval'
  ];

  const weatherWords = [
    'cloud', 'cold', 'dew', 'dry', 'hot', 'lightning', 'moon', 'rain', 'sky', 'star', 'sun',
    'thunder', 'weather', 'wind', 'fog', 'flood', 'monsoon', 'shower', 'storm', 'typhoon',
    'hurricane', 'breeze', 'snow', 'ice', 'freeze', 'hail', 'sleet', 'frost', 'mist', 'drizzle',
    'downpour', 'tornado', 'cyclone', 'blizzard', 'drought', 'humidity', 'humid',
    'overcast', 'cloudy', 'sunny', 'rainy', 'windy', 'stormy', 'foggy', 'misty',
    'rainbow', 'haze', 'smog', 'eclipse', 'meteor', 'comet', 'constellation',
    'temperature', 'climate', 'warm', 'cool', 'chilly', 'freezing', 'scorching',
    'tide', 'current', 'whirlpool', 'tsunami', 'earthquake', 'tremor', 'eruption', 'volcano',
    'landslide', 'avalanche'
  ];

  const natureWords = [
    'bamboo', 'bark', 'branch', 'earth', 'forest', 'leaf', 'mountain', 'ocean', 'river', 'sea',
    'soil', 'stone', 'tree', 'wave', 'flower', 'grass', 'root', 'seed', 'plant', 'island',
    'sand', 'mud', 'dust', 'moss', 'ashes', 'ember', 'fire', 'smoke', 'foam', 'torch', 'gold',
    'silver', 'copper', 'iron', 'wood', 'hill', 'valley', 'cave', 'lake', 'pond', 'waterfall',
    'swamp', 'jungle', 'creek', 'stream', 'brook', 'spring', 'well', 'canal', 'dam', 'reef',
    'cliff', 'canyon', 'gorge', 'ravine', 'plateau', 'prairie', 'meadow', 'pasture', 'field',
    'desert', 'oasis', 'tundra', 'glacier', 'iceberg', 'volcano', 'crater', 'geyser',
    'pebble', 'gravel', 'boulder', 'rock', 'mineral', 'crystal', 'gem', 'jewel', 'diamond',
    'ruby', 'emerald', 'sapphire', 'pearl', 'jade', 'amber', 'coral', 'ivory', 'marble',
    'granite', 'limestone', 'sandstone', 'clay', 'chalk', 'slate', 'quartz',
    'tin', 'zinc', 'lead', 'steel', 'bronze', 'brass', 'aluminum', 'platinum',
    'vine', 'shrub', 'bush', 'hedge', 'thicket', 'grove', 'orchard', 'plantation',
    'stump', 'trunk', 'twig', 'bud', 'blossom', 'petal', 'pollen', 'nectar', 'thorn', 'spine',
    'sap', 'resin', 'latex', 'timber', 'lumber', 'log', 'plank', 'firewood', 'kindling',
    'fern', 'palm', 'pine', 'oak', 'maple', 'willow', 'cedar', 'cypress', 'elm', 'birch',
    'eucalyptus', 'acacia', 'mahogany', 'teak', 'narra', 'mangrove', 'coconut tree',
    'mushroom', 'fungus', 'lichen', 'algae', 'seaweed', 'kelp',
    'wilderness', 'habitat', 'ecosystem', 'wetland', 'marsh', 'bog', 'delta', 'estuary',
    'peninsula', 'cape', 'bay', 'gulf', 'strait', 'channel', 'lagoon', 'cove', 'harbor',
    'shore', 'beach', 'coast', 'bank', 'bed', 'rapids', 'tributary', 'confluence',
    'horizon', 'landscape', 'terrain', 'topography', 'altitude', 'elevation',
    'continent', 'archipelago', 'atoll', 'islet'
  ];

  const familyWords = [
    'boy', 'brother-in-law', 'chief', 'child', 'cousin', 'father', 'girl', 'man', 'mother',
    'person', 'relative', 'sister', 'uncle', 'woman', 'daughter', 'son', 'parent', 'husband',
    'wife', 'sibling', 'orphan', 'lastborn', 'female', 'male', 'slave', 'companion', 'god',
    'aunt', 'nephew', 'niece', 'grandfather', 'grandmother', 'grandchild', 'ancestor', 'friend',
    'enemy', 'guest', 'stranger', 'neighbor', 'brother', 'baby', 'infant', 'toddler',
    'teenager', 'adolescent', 'adult', 'elder', 'elders', 'youth', 'maiden', 'widow', 'widower',
    'bride', 'groom', 'fiancé', 'fiancee', 'spouse', 'couple', 'family', 'clan', 'tribe',
    'kinship', 'lineage', 'dynasty', 'generation', 'descendant', 'heir', 'offspring',
    'godfather', 'godmother', 'godchild', 'stepfather', 'stepmother', 'stepson', 'stepdaughter',
    'father-in-law', 'mother-in-law', 'sister-in-law', 'son-in-law', 'daughter-in-law',
    'firstborn', 'twin', 'triplet', 'adopted', 'foster',
    'people', 'human', 'folk', 'community', 'society', 'citizen', 'villager', 'townsman',
    'king', 'queen', 'prince', 'princess', 'knight', 'warrior', 'soldier', 'guard',
    'priest', 'priestess', 'shaman', 'healer', 'midwife', 'teacher', 'student', 'pupil',
    'master', 'servant', 'lord', 'noble', 'peasant', 'commoner', 'beggar', 'thief',
    'leader', 'ruler', 'governor', 'mayor', 'judge', 'counselor', 'adviser',
    'merchant', 'trader', 'farmer', 'fisherman', 'hunter', 'gatherer', 'craftsman',
    'blacksmith', 'carpenter', 'weaver', 'potter', 'tailor', 'baker', 'butcher',
    'sailor', 'navigator', 'captain', 'crew'
  ];

  const emotionsWords = [
    'anger', 'angry', 'bad', 'beautiful', 'bright', 'clean', 'happy', 'sad', 'afraid', 'love',
    'tired', 'sick', 'fear', 'joy', 'good', 'scared', 'ugly', 'sleepy', 'awake', 'dream',
    'soul', 'alive', 'foul-smelling', 'fragrant', 'wrong', 'true', 'false', 'lie', 'truth',
    'shame', 'proud', 'brave', 'coward', 'smart', 'stupid', 'crazy', 'calm', 'nervous',
    'excited', 'bored', 'lonely', 'jealous', 'envy', 'greed', 'greedy', 'selfish', 'generous',
    'kind', 'cruel', 'gentle', 'fierce', 'humble', 'arrogant', 'patient', 'impatient',
    'grateful', 'ungrateful', 'hopeful', 'hopeless', 'confident', 'shy', 'timid', 'bold',
    'anxious', 'worried', 'relieved', 'surprised', 'shocked', 'amazed', 'astonished',
    'delighted', 'thrilled', 'ecstatic', 'content', 'satisfied', 'pleased', 'glad',
    'miserable', 'depressed', 'gloomy', 'melancholy', 'sorrowful', 'grieving', 'mourning',
    'furious', 'enraged', 'irritated', 'annoyed', 'frustrated', 'resentful', 'bitter',
    'disgusted', 'horrified', 'terrified', 'petrified', 'panic', 'dread',
    'compassion', 'empathy', 'sympathy', 'pity', 'mercy', 'forgiveness', 'gratitude',
    'hatred', 'contempt', 'scorn', 'spite', 'malice', 'revenge', 'vengeance',
    'desire', 'longing', 'yearning', 'craving', 'passion', 'lust', 'devotion', 'affection',
    'admiration', 'respect', 'worship', 'praise', 'blessing', 'curse',
    'peace', 'harmony', 'chaos', 'conflict', 'tension', 'stress', 'relief',
    'courage', 'determination', 'perseverance', 'willpower', 'spirit', 'morale',
    'wisdom', 'knowledge', 'ignorance', 'curiosity', 'wonder', 'awe',
    'guilt', 'regret', 'remorse', 'apology', 'forgive', 'pardon',
    'doubt', 'suspicion', 'trust', 'faith', 'belief', 'hope',
    'laugh', 'cry', 'weep', 'sob', 'mourn', 'grieve', 'celebrate', 'rejoice',
    'smile', 'frown', 'scowl', 'grimace', 'grin', 'smirk', 'blush',
    'sigh', 'groan', 'moan', 'scream', 'shout', 'whisper', 'murmur'
  ];

  const colorsWords = [
    'black', 'blue', 'green', 'red', 'white', 'yellow', 'gray', 'grey', 'brown', 'purple',
    'violet', 'indigo', 'orange', 'pink', 'magenta', 'cyan', 'turquoise', 'maroon', 'navy',
    'teal', 'olive', 'beige', 'tan', 'cream', 'ivory', 'scarlet', 'crimson', 'burgundy',
    'lavender', 'lilac', 'mauve', 'coral', 'salmon', 'peach', 'gold', 'golden', 'silver',
    'bronze', 'copper', 'khaki', 'charcoal', 'ebony', 'rust',
    'round', 'sharp', 'flat', 'long', 'short', 'big', 'small', 'tall', 'wide', 'narrow',
    'thick', 'thin', 'deep', 'shallow', 'high', 'dark', 'light', 'rough', 'smooth', 'soft',
    'hard', 'dull', 'fragile', 'tight', 'loose', 'straight', 'fast', 'slow', 'strong', 'fat',
    'full', 'new', 'old', 'wet', 'dirty', 'near', 'far', 'heavy', 'empty', 'hollow',
    'solid', 'liquid', 'rigid', 'flexible', 'elastic', 'stiff', 'limp', 'firm', 'tender',
    'coarse', 'fine', 'gross', 'net', 'broad', 'slim', 'lean', 'plump', 'obese',
    'huge', 'tiny', 'massive', 'miniature', 'enormous', 'gigantic', 'microscopic',
    'square', 'rectangular', 'triangular', 'circular', 'oval', 'spherical', 'cylindrical',
    'curved', 'bent', 'twisted', 'spiral', 'zigzag', 'wavy', 'crooked',
    'pointed', 'blunt', 'jagged', 'serrated', 'smooth', 'polished', 'glossy', 'matte',
    'transparent', 'translucent', 'opaque', 'shiny', 'glowing', 'luminous', 'dim', 'bright',
    'colorful', 'drab', 'vivid', 'pale', 'faded'
  ];

  const toolsWords = [
    'adze', 'arrow', 'axe', 'basket', 'blade', 'boat', 'charcoal', 'net', 'paddle', 'spear',
    'trap', 'needle', 'necklace', 'outrigger', 'wheel', 'bundle', 'knife', 'sword', 'shield',
    'bow', 'hammer', 'saw', 'rope', 'string', 'thread', 'cloth', 'weaving', 'loom', 'pot',
    'pan', 'bowl', 'plate', 'cup', 'spoon', 'fork', 'chisel', 'drill', 'file', 'pliers',
    'wrench', 'screwdriver', 'nail', 'screw', 'bolt', 'hook', 'chain', 'wire', 'cable',
    'shovel', 'hoe', 'rake', 'sickle', 'scythe', 'plow', 'harrow', 'trowel', 'pickaxe',
    'machete', 'bolo', 'cleaver', 'scissors', 'shears', 'razor', 'grindstone', 'whetstone',
    'anvil', 'forge', 'bellows', 'tongs', 'crucible', 'mold',
    'canoe', 'raft', 'ship', 'vessel', 'sail', 'mast', 'anchor', 'rudder', 'oar',
    'cart', 'wagon', 'sled', 'stretcher', 'yoke', 'harness', 'bridle', 'saddle',
    'bucket', 'barrel', 'crate', 'jar', 'jug', 'pitcher', 'vase', 'urn', 'gourd',
    'mortar', 'pestle', 'sieve', 'strainer', 'funnel', 'ladle', 'spatula', 'whisk',
    'broom', 'mop', 'dustpan', 'brush', 'comb', 'mirror',
    'lamp', 'lantern', 'candle', 'torch', 'match', 'lighter', 'flint',
    'bag', 'sack', 'pouch', 'wallet', 'purse', 'box', 'chest', 'trunk', 'cabinet',
    'shelf', 'drawer', 'rack', 'hanger', 'peg', 'clip', 'pin', 'ring', 'bracelet',
    'earring', 'brooch', 'pendant', 'crown', 'tiara', 'headdress',
    'weapon', 'dagger', 'lance', 'pike', 'mace', 'club', 'slingshot', 'blowgun',
    'crossbow', 'quiver', 'sheath', 'scabbard', 'armor', 'helmet',
    'fishing rod', 'fishhook', 'fishing line', 'bait', 'lure', 'tackle',
    'tool', 'instrument', 'implement', 'utensil', 'gadget', 'device', 'apparatus',
    'equipment', 'gear', 'machinery', 'mechanism'
  ];

  const houseWords = [
    'house', 'door', 'roof', 'stairs', 'fence', 'garment', 'mat', 'pillow', 'storehouse',
    'garden', 'road', 'image', 'war', 'debt', 'name', 'called', 'side', 'same', 'home',
    'room', 'floor', 'wall', 'window', 'bed', 'blanket', 'table', 'chair', 'village', 'town',
    'city', 'path', 'trail', 'bridge', 'hut', 'cabin', 'cottage', 'tent', 'shelter', 'dwelling',
    'building', 'structure', 'tower', 'castle', 'palace', 'temple', 'church', 'mosque',
    'shrine', 'altar', 'cemetery', 'grave', 'tomb',
    'gate', 'entrance', 'exit', 'hallway', 'corridor', 'porch', 'veranda', 'balcony',
    'terrace', 'courtyard', 'yard', 'lawn', 'driveway', 'garage', 'shed', 'barn',
    'attic', 'basement', 'cellar', 'closet', 'pantry', 'bathroom', 'bedroom', 'kitchen',
    'ceiling', 'beam', 'pillar', 'column', 'post', 'rafter', 'joist', 'foundation',
    'brick', 'mortar', 'concrete', 'cement', 'plaster', 'paint', 'tile', 'shingle', 'thatch',
    'carpet', 'rug', 'curtain', 'drape', 'blinds', 'shade',
    'furniture', 'sofa', 'couch', 'bench', 'stool', 'desk', 'wardrobe', 'dresser',
    'shelf', 'bookcase', 'cabinet', 'cupboard', 'counter',
    'mattress', 'sheet', 'quilt', 'comforter', 'duvet', 'cushion',
    'shirt', 'pants', 'trousers', 'skirt', 'dress', 'blouse', 'jacket', 'coat',
    'hat', 'cap', 'scarf', 'glove', 'sock', 'shoe', 'boot', 'sandal', 'slipper',
    'belt', 'tie', 'apron', 'uniform', 'robe', 'cloak', 'veil', 'underwear',
    'clothing', 'outfit', 'costume', 'fabric', 'textile', 'silk', 'cotton', 'wool',
    'linen', 'leather', 'denim', 'satin', 'velvet', 'nylon', 'polyester',
    'sew', 'stitch', 'hem', 'patch', 'button', 'zipper', 'buckle', 'lace',
    'market', 'shop', 'store', 'school', 'hospital', 'office', 'factory', 'warehouse',
    'prison', 'jail', 'court', 'library', 'museum', 'theater', 'stadium', 'arena',
    'street', 'avenue', 'highway', 'alley', 'lane', 'intersection', 'crossroads'
  ];

  const greetingsWords = [
    'goodbye', 'how much', 'what', 'when', 'where', 'who', 'why', 'not',
    'i don\'t know', 'in', 'out', 'up', 'down', 'downward', 'to', 'left', 'right',
    'over there', 'middle', 'other', 'shadow', 'hello', 'hi', 'welcome', 'thank you', 'thanks',
    'please', 'sorry', 'excuse me', 'yes', 'no', 'okay', 'farewell', 'greetings',
    'good morning', 'good afternoon', 'good evening', 'good night',
    'here', 'there', 'everywhere', 'nowhere', 'somewhere', 'anywhere',
    'above', 'below', 'behind', 'beside', 'between', 'inside', 'outside',
    'front', 'back', 'top', 'bottom', 'center', 'edge', 'corner',
    'north', 'south', 'east', 'west', 'direction', 'toward', 'away',
    'also', 'too', 'already', 'still', 'yet', 'again', 'perhaps', 'maybe',
    'certainly', 'definitely', 'probably', 'possibly', 'truly', 'really',
    'very', 'quite', 'rather', 'somewhat', 'enough', 'almost', 'barely',
    'only', 'just', 'even', 'except', 'instead', 'however', 'therefore',
    'all', 'every', 'each', 'both', 'either', 'neither', 'none', 'any',
    'much', 'more', 'most', 'less', 'least', 'than',
    'this', 'that', 'these', 'those', 'which', 'whose',
    'if', 'then', 'else', 'unless', 'until', 'while', 'because', 'since',
    'although', 'though', 'whether', 'so', 'and', 'but', 'or', 'nor',
    'with', 'without', 'about', 'through', 'during', 'before', 'after'
  ];



  function escapeRegex(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  function getCategory(eng) {
      const e = (eng || '').toLowerCase();
      
      const matches = (words) => words.some(w => new RegExp('\\b' + escapeRegex(w) + '\\b', 'i').test(e));

      // Check specific categories first, broader ones later
      if (matches(bodyWords)) return 'Body Parts & Health';
      if (matches(animalsWords)) return 'Animals & Wildlife';
      if (matches(foodWords)) return 'Food & Dining';

      if (matches(houseWords)) return 'House & Daily Life';
      if (matches(numbersWords)) return 'Numbers & Time';
      if (matches(weatherWords)) return 'Weather & Climate';
      if (matches(natureWords)) return 'Nature & Environment';
      if (matches(familyWords)) return 'Family & People';
      if (matches(emotionsWords)) return 'Emotions & Feelings';
      if (matches(toolsWords)) return 'Occupations & Tools';
      if (matches(colorsWords)) return 'Colors & Shapes';
      if (matches(greetingsWords)) return 'Greetings & Essentials';
      
      return 'General';
  }

  let updates = [];
  vocabulary.forEach(v => {
    const suggestedCat = getCategory(v.english);
    
    // Force update if it's the deprecated 'Actions (Verbs)' or 'Actions & Verbs' category
    if (v.category === 'Actions (Verbs)' || v.category === 'Actions & Verbs') {
      updates.push({ id: v.id, category: suggestedCat });
    } 
    // Update if it's currently General, or if we want to aggressively categorize everything
    // We will only update if the suggested category is different from current.
    // Also, if the current is a specific category (not General) and the suggestion is General, don't downgrade it.
    else if (suggestedCat !== 'General' && v.category !== suggestedCat) {
      updates.push({ id: v.id, category: suggestedCat });
    } else if ((!v.category || v.category === 'General' || v.category === 'Greetings & Essentials') && suggestedCat !== 'General') {
      updates.push({ id: v.id, category: suggestedCat });
    } else if (!v.category) {
      updates.push({ id: v.id, category: 'General' });
    }
  });

  if (updates.length === 0) {
    notify("All words are already accurately categorized!", 'success');
    return;
  }

  if (!(await confirmDialog({
    title: `Update ${updates.length} categories?`,
    body: `Found ${updates.length} words that can be automatically categorized. Proceed?`,
    confirmLabel: 'Update'
  }))) return;

  try {
    let count = 0;
    let batches = [];
    let currentBatch = writeBatch(db);
    let operations = 0;

    for (const update of updates) {
      currentBatch.update(doc(db, "vocabulary", update.id), { category: update.category });
      count++;
      operations++;

      if (operations === 490) {
        batches.push(currentBatch);
        currentBatch = writeBatch(db);
        operations = 0;
      }
    }

    if (operations > 0) {
      batches.push(currentBatch);
    }

    notify(`Updating ${count} categories...`, 'success');
    for (const batch of batches) {
      await batch.commit();
    }
    
    await logAudit("vocabulary.auto_categorize", { count });
    notify(`Successfully categorized ${count} words!`, 'success');
  } catch (e) {
    console.error("Error updating categories:", e);
    notify("Failed to auto-categorize: " + e.message, 'error');
  }
};

window.removeDuplicateWords = async function() {
  if (!(await confirmDialog({
    title: `Remove Duplicate Words?`,
    body: `Are you sure you want to scan and remove all duplicate words from the dictionary? This action cannot be undone.`,
    confirmLabel: 'Remove Duplicates'
  }))) return;

  const duplicatesToDelete = [];
  const wordMap = new Map();

  vocabulary.forEach(v => {
    const wordClean = (v.kasiguranin || '').trim().toLowerCase();
    if (!wordClean) return;
    
    if (!wordMap.has(wordClean)) {
      wordMap.set(wordClean, [v]);
    } else {
      wordMap.get(wordClean).push(v);
    }
  });

  wordMap.forEach((docs, word) => {
    if (docs.length > 1) {
      docs.sort((a, b) => {
        const timeA = a.createdAt || 0;
        const timeB = b.createdAt || 0;
        return timeB - timeA;
      });
      for (let i = 1; i < docs.length; i++) {
        duplicatesToDelete.push(docs[i].id);
      }
    }
  });

  if (duplicatesToDelete.length === 0) {
    notify("No duplicates found in the dictionary.", 'success');
    return;
  }

  if (!(await confirmDialog({
    title: `Delete ${duplicatesToDelete.length} duplicates?`,
    body: `Found ${duplicatesToDelete.length} duplicates. Proceed with deletion?`,
    confirmLabel: 'Delete'
  }))) return;

  try {
    let count = 0;
    let batches = [];
    let currentBatch = writeBatch(db);
    let operations = 0;

    for (const id of duplicatesToDelete) {
      currentBatch.delete(doc(db, "vocabulary", id));
      count++;
      operations++;

      if (operations === 490) {
        batches.push(currentBatch);
        currentBatch = writeBatch(db);
        operations = 0;
      }
    }

    if (operations > 0) {
      batches.push(currentBatch);
    }

    notify(`Deleting ${count} duplicate entries...`, 'success');
    for (const batch of batches) {
      await batch.commit();
    }
    
    await logAudit("vocabulary.remove_duplicates", { count });
    notify(`Successfully removed ${count} duplicate entries!`, 'success');
  } catch (e) {
    console.error("Error removing duplicates:", e);
    notify("Failed to remove duplicates: " + e.message, 'error');
  }
};

