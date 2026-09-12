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
import { normaliseWord, findExistingWord } from './word-normalize.js';

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
let reports = [];
let reportsLoaded = false;
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
let auditLogsLoaded = false;

let auditLogs = [];
let logsCurrentPage = 1;
const LOGS_PER_PAGE = 50;

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
  'tab-reports': 'reports',
  'tab-vocabulary': 'dictionary',
  'tab-stages': 'stages',
  'tab-stories': 'stories',
  'tab-releases': 'releases',
  'tab-users': 'users',
  'tab-logs': 'logs',
  'tab-backup': 'backup'
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
  initStageReview();
  initLogsControls();
  initUsersListener();
  initBansListener();
  initBackupRestore();

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

  // 1c. User Issue & Word Reports Listener (bugs, wrong words, photo evidence)
  try {
    const reportsQuery = query(collection(db, "issue_reports"), orderBy("submittedAt", "desc"));
    const unsubReports = onSnapshot(reportsQuery, (snapshot) => {
      reports = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      reportsLoaded = true;
      renderReportsTable();
      updateDashboardMetrics();
    }, (error) => {
      console.warn("Primary reports query failed, attempting plain fallback query:", error);
      try {
        const reportsFallback = query(collection(db, "issue_reports"));
        const unsubFallback = onSnapshot(reportsFallback, (snapshot) => {
          reports = snapshot.docs
            .map(doc => ({ id: doc.id, ...doc.data() }))
            .sort((a, b) => (b.submittedAt || b.createdAt || 0) - (a.submittedAt || a.createdAt || 0));
          reportsLoaded = true;
          renderReportsTable();
          updateDashboardMetrics();
        }, (fallbackErr) => {
          console.error("Reports fallback listener error:", fallbackErr);
          renderReportsError("Unable to connect to live issue reports queue.");
        });
        unsubscribeFns.push(unsubFallback);
      } catch (e) {
        renderReportsError("Unable to connect to live issue reports queue.");
      }
    });
    unsubscribeFns.push(unsubReports);
  } catch (e) {
    console.error("Firestore reports query error:", e);
  }

  // 1d. Announcements Listener - the admin's own view of what AnnouncementRepository serves live.
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
      renderStageReview();
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

  // 4. Admin Audit Logs Listener
  try {
    const logsQuery = query(collection(db, "admin_audit_log"), orderBy("timestamp", "desc"));
    const unsubLogs = onSnapshot(logsQuery, (snapshot) => {
      auditLogs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      auditLogsLoaded = true;
      renderAuditLogs();
    }, (error) => {
      console.warn("Audit logs listener error:", error);
      let msg = "Unable to connect to live Firestore audit log.";
      if (error.code === 'permission-denied') {
        msg = "Permission denied. Ensure Firestore Rules allow read access.";
      }
      renderAuditLogsError(msg);
    });
    unsubscribeFns.push(unsubLogs);
  } catch (e) {
    console.error("Firestore audit log query error:", e);
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
  // The two gaps worth working through in bulk. A sentence unlocks the lesson system's
  // build-the-sentence exercise for that word; a theme decides which section of the learning path
  // teaches it. Both are filled in the edit form below, so this filter is how you find the queue.
  const gap = document.getElementById('filter-vocab-gap')?.value || '';

  return vocabulary.filter(item => {
    const matchesSearch = !term ||
      (item.kasiguranin || '').toLowerCase().includes(term) ||
      (item.tagalog || '').toLowerCase().includes(term) ||
      (item.english || '').toLowerCase().includes(term);
    const matchesCat = !cat || item.category === cat;
    const matchesLetter = !vocabLetter ||
      (item.kasiguranin || '').trim().charAt(0).toUpperCase() === vocabLetter;
    const matchesGap =
      !gap ||
      (gap === 'sentence' && !(item.exampleSentence || '').trim()) ||
      (gap === 'theme' && !(item.theme || '').trim()) ||
      // A sentence whose English translation is missing. The lesson's sentence-building exercise
      // reads exampleTranslation and shows it to the learner as what the sentence means, so a row
      // with a Kasiguranin sentence and no English gloss either shows nothing or, worse, shows
      // whatever language happened to be typed into that box. The field used to be labelled
      // "Its Tagalog or English translation", and the five sentences recorded so far all hold
      // Tagalog, so this filter is how they get found and repaired.
      (gap === 'sentence-english' &&
        (item.exampleSentence || '').trim() && !(item.exampleTranslation || '').trim());
    return matchesSearch && matchesCat && matchesLetter && matchesGap;
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

  // Example sentences were editable in this portal long before they were ever shown back here, so
  // a moderator had no way to see what a word already carried without opening the edit form.
  const examples = [
    [item.exampleSentence, item.exampleTranslation],
    [item.exampleSentence2, item.exampleTranslation2]
  ].filter(([sentence]) => (sentence || '').trim());

  body.innerHTML = `
    <div class="entry-detail-head">
      <span class="headword">${escapeHtml(item.kasiguranin || '—')}</span>
      ${item.ipaNotation ? `<span class="ipa">/${escapeHtml(item.ipaNotation)}/</span>` : ''}
      ${pos ? `<span class="pos">${escapeHtml(pos)}</span>` : ''}
    </div>
    <dl class="deflist">
      ${row('Tagalog', item.tagalog)}
      ${row('English', item.english)}
      ${row('Meaning (English)', item.meaningEnglish)}
      ${row('Meaning (Tagalog)', item.meaningTagalog)}
      <dt>Category</dt><dd><span class="badge badge-category">${escapeHtml(item.category || 'General')}</span></dd>
    </dl>
    ${aspects.length ? `
      <div style="margin-top:var(--s-5);">
        <dt style="font-size:var(--t-xs); font-weight:700; color:var(--muted);">Verb aspects</dt>
        <div class="aspect-grid">
          ${aspects.map(([label, value]) => `
            <div class="aspect"><span>${label}</span><b>${escapeHtml(value)}</b></div>`).join('')}
        </div>
      </div>` : ''}
    ${examples.length ? `
      <div style="margin-top:var(--s-5);">
        <dt style="font-size:var(--t-xs); font-weight:700; color:var(--muted);">Example sentences</dt>
        ${examples.map(([sentence, translation]) => `
          <p style="margin:var(--s-2) 0 0;"><i>${escapeHtml(sentence)}</i>${
            (translation || '').trim()
              ? `<br><span style="color:var(--muted);">${escapeHtml(translation)}</span>`
              : ''
          }</p>`).join('')}
      </div>` : ''}`;

  const editBtn = document.getElementById('entry-modal-edit');
  if (editBtn) {
    editBtn.onclick = () => {
      window.closeModal('entry-modal');
      window.openEditVocabModal(id);
    };
  }
  
  const deleteBtn = document.getElementById('entry-modal-delete');
  if (deleteBtn) {
    deleteBtn.onclick = async () => {
      if (!(await confirmDialog({
        title: 'Delete Dictionary Entry?',
        body: `Are you sure you want to delete the entry for "${item.kasiguranin}"? This action cannot be undone.`,
        confirmLabel: 'Delete'
      }))) return;

      try {
        await deleteDoc(doc(db, "vocabulary", id));
        await logAudit("vocabulary.delete", { id, kasiguranin: item.kasiguranin });
        window.closeModal('entry-modal');
        notify(`Deleted ${item.kasiguranin}`, 'success');
        vocabulary = vocabulary.filter(v => v.id !== id);
        renderVocabularyTable();
      } catch (error) {
        console.error("Error deleting entry:", error);
        notify("Failed to delete entry: " + error.message, 'error');
      }
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

  // "Live" is the newest release the app and the download page will actually offer: the highest
  // versionCode that has not been yanked. releases is already ordered versionCode-desc.
  const live = releases.find(rel => !rel.yanked) || null;

  container.innerHTML = releases.map((rel) => {
    const ms = toMillis(rel.releasedAt);
    const when = ms
      ? new Date(ms).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
      : 'date not recorded';
    const notes = (rel.releaseNotes || '').trim();
    const isLive = live && rel.id === live.id;
    const id = escapeHtml(rel.id);

    return `
      <div class="release-row${isLive ? ' is-live' : ''}${rel.yanked ? ' is-yanked' : ''}"${rel.yanked ? ' style="opacity:.55;"' : ''}>
        <div class="release-node"><span class="release-dot" aria-hidden="true"></span></div>
        <div class="release-main">
          <div class="release-title">
            <b>v${escapeHtml(String(rel.versionName || '?'))}</b>
            <small>Build ${escapeHtml(String(rel.versionCode ?? '—'))} · ${escapeHtml(when)}</small>
            ${isLive ? '<span class="badge badge-approved">Live</span>' : ''}
            ${rel.yanked ? '<span class="badge badge-rejected">Yanked</span>' : ''}
            ${rel.forceUpdate ? '<span class="badge badge-pending">Required</span>' : ''}
          </div>
          <p class="release-notes${notes ? '' : ' is-empty'}">${notes ? escapeHtml(notes) : 'No release notes were recorded for this build.'}</p>
        </div>
        <div class="release-side" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
          ${rel.apkUrl
            ? `<a href="${escapeHtml(rel.apkUrl)}" target="_blank" rel="noopener" class="btn btn-outline btn-sm">
                 <iconsax-icon name="document-download" type="bulk" size="15" color="currentColor"></iconsax-icon> APK
               </a>`
            : '<span class="result-count">No link</span>'}
          <button type="button" class="btn btn-outline btn-sm" onclick="editRelease('${id}')">Edit</button>
          <button type="button" class="btn btn-outline btn-sm" onclick="toggleReleaseYank('${id}')">${rel.yanked ? 'Restore' : 'Yank'}</button>
        </div>
      </div>`;
  }).join('');
}

// Roll installs back to the previous good build by pulling a bad release. The app and the download
// page skip yanked releases when they pick "the latest one"; someone already on the yanked build is
// not downgraded — Android will not install an older APK over a newer one — so a real fix still
// needs a fresh release.
window.toggleReleaseYank = async function(id) {
  const rel = releases.find(r => r.id === id);
  if (!rel) return;
  const yank = !rel.yanked;

  const proceed = await confirmDialog(yank ? {
    title: `Yank v${rel.versionName}?`,
    body: 'The app and the download page stop offering this build and fall back to the previous ' +
          'release. Anyone already on it stays on it until you publish a fixed version.',
    confirmLabel: 'Yank this build', danger: true
  } : {
    title: `Restore v${rel.versionName}?`,
    body: 'It becomes available again. If it is the highest version code, it goes back to being the ' +
          'one the app and download page offer.',
    confirmLabel: 'Restore'
  });
  if (!proceed) return;

  try {
    await updateDoc(doc(db, 'app_releases', id), { yanked: yank });
    await logAudit(yank ? 'release.yank' : 'release.restore', {
      versionName: rel.versionName, versionCode: rel.versionCode
    });
    notify(yank ? `v${rel.versionName} yanked.` : `v${rel.versionName} restored.`, 'success');
  } catch (err) {
    notify('Could not update that release: ' + err.message, 'error');
  }
};

// Opens the publish modal in edit mode for an existing release. versionName is the doc id, so it is
// shown read-only; everything else is editable.
window.editRelease = function(id) {
  const rel = releases.find(r => r.id === id);
  if (!rel) return;
  // openModal resets the form to "publish" state; populate for editing straight after.
  window.openModal('publish-release-modal');
  document.getElementById('rel-editing-id').value = id;
  document.getElementById('rel-code').value = rel.versionCode ?? '';
  document.getElementById('rel-name').value = rel.versionName || '';
  document.getElementById('rel-name').disabled = true;
  document.getElementById('rel-url').value = rel.apkUrl || '';
  document.getElementById('rel-notes').value = rel.releaseNotes || '';
  const force = document.getElementById('rel-force');
  if (force) force.checked = !!rel.forceUpdate;
  const title = document.getElementById('rel-modal-title');
  if (title) title.textContent = `Edit v${rel.versionName}`;
  const btn = document.getElementById('rel-submit-btn');
  if (btn) btn.textContent = 'Save changes';
};

// Back to a blank "publish" state. Called when the modal opens for a new release and after a save.
function resetReleaseForm() {
  const form = document.getElementById('publish-release-form');
  if (form) form.reset();
  const editId = document.getElementById('rel-editing-id');
  if (editId) editId.value = '';
  const name = document.getElementById('rel-name');
  if (name) name.disabled = false;
  const title = document.getElementById('rel-modal-title');
  if (title) title.textContent = 'Publish release';
  const btn = document.getElementById('rel-submit-btn');
  if (btn) btn.textContent = 'Publish release';
}


// ── Admin Logs Tab ──────────────────────────────────────────────────────────

function initLogsControls() {
  const searchLogs = document.getElementById('search-logs-input');
  if (searchLogs) {
    searchLogs.addEventListener('input', () => {
      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(() => {
        logsCurrentPage = 1;
        renderAuditLogs();
      }, 250);
    });
  }
  const filterLogs = document.getElementById('filter-logs-action');
  if (filterLogs) {
    filterLogs.addEventListener('change', () => {
      logsCurrentPage = 1;
      renderAuditLogs();
    });
  }
}

function filteredAuditLogs() {
  const searchTerm = (document.getElementById('search-logs-input')?.value || '').trim().toLowerCase();
  const filterAction = document.getElementById('filter-logs-action')?.value || '';

  return auditLogs.filter(log => {
    if (filterAction) {
      if (filterAction === 'user') {
        if (!log.action.startsWith('user.') && !log.action.startsWith('user_') && !log.action.startsWith('appeal.')) return false;
      } else if (filterAction === 'appeal') {
        if (!log.action.startsWith('appeal.')) return false;
      } else if (!log.action.startsWith(filterAction)) {
        return false;
      }
    }

    if (searchTerm) {
      if (log.actor && log.actor.toLowerCase().includes(searchTerm)) return true;
      if (log.action && log.action.toLowerCase().includes(searchTerm)) return true;
      if (log.details) {
        const detailsStr = JSON.stringify(log.details).toLowerCase();
        if (detailsStr.includes(searchTerm)) return true;
      }
      return false;
    }
    return true;
  });
}

window.setLogsPage = function(n) {
  logsCurrentPage = n;
  renderAuditLogs();
};

function renderLogsPager(total) {
  const pager = document.getElementById('logs-pager');
  if (!pager) return;
  const totalPages = Math.ceil(total / LOGS_PER_PAGE) || 1;
  if (logsCurrentPage > totalPages) logsCurrentPage = totalPages;

  if (totalPages <= 1) {
    pager.innerHTML = '';
    return;
  }

  let h = '';
  h += `<button class="btn btn-outline btn-sm" ${logsCurrentPage === 1 ? 'disabled' : ''} onclick="window.setLogsPage(${logsCurrentPage - 1})">Prev</button>`;
  h += `<span class="pager-text">Page ${logsCurrentPage} of ${totalPages}</span>`;
  h += `<button class="btn btn-outline btn-sm" ${logsCurrentPage === totalPages ? 'disabled' : ''} onclick="window.setLogsPage(${logsCurrentPage + 1})">Next</button>`;
  pager.innerHTML = h;
}

function renderAuditLogsError(message) {
  const container = document.getElementById('audit-log-list');
  if (!container) return;
  container.innerHTML = `<div class="empty">
    <iconsax-icon name="shield-cross" type="bulk" size="30" color="var(--status-rejected)"></iconsax-icon>
    <b style="color:var(--status-rejected);">Access Denied</b>
    ${escapeHtml(message)}
  </div>`;
  const count = document.getElementById('logs-result-count');
  if (count) count.textContent = '';
  renderLogsPager(0);
}

function renderAuditLogs() {
  const container = document.getElementById('audit-log-list');
  if (!container || !auditLogsLoaded) return;

  const fLogs = filteredAuditLogs();
  
  const countSpan = document.getElementById('logs-result-count');
  if (countSpan) countSpan.textContent = `${fLogs.length.toLocaleString()} log${fLogs.length === 1 ? '' : 's'}`;
  
  renderLogsPager(fLogs.length);

  if (fLogs.length === 0) {
    container.innerHTML = `
      <div class="empty">
        <iconsax-icon name="shield-tick" type="bulk" size="30" color="currentColor"></iconsax-icon>
        <b>No logs found</b>
        The audit log is empty or no entries match your search.
      </div>`;
    return;
  }

  const start = (logsCurrentPage - 1) * LOGS_PER_PAGE;
  const pageItems = fLogs.slice(start, start + LOGS_PER_PAGE);

  container.innerHTML = pageItems.map((log, i) => {
    const ms = log.timestamp;
    const when = ms ? new Date(ms).toLocaleString(undefined, { 
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }) : 'unknown time';
    
    // Extract a readable summary from details
    let summary = '';
    const d = log.details || {};
    if (d.displayName || d.uid) {
      const userDisplay = d.displayName ? d.displayName : `UID: ${d.uid}`;
      const reasonPart = d.reason ? ` — Reason: "${d.reason}"` : (d.reviewNotes ? ` — Note: "${d.reviewNotes}"` : (d.note ? ` — Note: "${d.note}"` : ''));
      summary = `User: ${userDisplay}${reasonPart}`;
    } else if (d.word) summary = `Word: ${d.word}`;
    else if (d.title) summary = `Title: ${d.title}`;
    else if (d.kasiguranin) summary = `Word: ${d.kasiguranin}`;
    else if (d.versionName) summary = `v${d.versionName}`;

    let badgeClass = 'badge-category';
    if (log.action.includes('block') || log.action.includes('delete') || log.action.includes('reject')) {
      badgeClass = 'badge-rejected';
    } else if (log.action.includes('unblock') || log.action.includes('approve') || log.action.includes('create')) {
      badgeClass = 'badge-approved';
    }

    return `
      <div class="release-row" style="grid-template-columns: auto 1fr; border-bottom: 1px solid var(--hair); padding: var(--s-4) 0;">
        <div class="release-node"><span class="release-dot" aria-hidden="true" style="background:var(--violet-soft);"></span></div>
        <div class="release-main">
          <div class="release-title">
            <span class="badge ${badgeClass}" style="margin-left:0; margin-right:var(--s-2); font-family:var(--sans); font-size:var(--t-xs); font-weight:700;">${escapeHtml(log.action)}</span>
            <b style="font-size: var(--t-sm); font-family:var(--sans);">${escapeHtml(log.actor)}</b>
            <small style="font-size: var(--t-xs); color: var(--muted);">${escapeHtml(when)}</small>
            ${summary ? `<small style="margin-left: var(--s-2); color: var(--ink);"><b>${escapeHtml(summary)}</b></small>` : ''}
          </div>
        </div>
      </div>`;
  }).join('');
}

window.exportAuditLogs = function() {
  const rangeEl = document.getElementById('export-logs-range');
  const rangeVal = rangeEl ? rangeEl.value : '7';

  let targetLogs = auditLogs;
  let label = 'all';

  if (rangeVal !== 'all') {
    const days = parseInt(rangeVal, 10) || 7;
    const cutoff = Date.now() - (days * 24 * 60 * 60 * 1000);
    targetLogs = auditLogs.filter(log => (log.timestamp || 0) >= cutoff);
    label = `past-${days}-days`;
  }

  if (!targetLogs || targetLogs.length === 0) {
    notify(`No audit logs found for the selected time range (${label.replace(/-/g, ' ')}).`, "info");
    return;
  }

  const dateStr = new Date().toISOString().split('T')[0];
  const exportedAt = new Date().toLocaleString();
  const rangeLabel = label.replace(/-/g, ' ');

  // Helper to pick a badge colour based on the action string
  function actionColor(action) {
    const a = (action || '').toLowerCase();
    if (a.includes('delete') || a.includes('block') || a.includes('reject') || a.includes('ban')) {
      return { bg: '#fee2e2', color: '#b91c1c', border: '#fca5a5' };
    }
    if (a.includes('create') || a.includes('approve') || a.includes('unblock') || a.includes('unban')) {
      return { bg: '#dcfce7', color: '#15803d', border: '#86efac' };
    }
    if (a.includes('update') || a.includes('edit') || a.includes('modify')) {
      return { bg: '#fef9c3', color: '#a16207', border: '#fde047' };
    }
    return { bg: '#e0e7ff', color: '#3730a3', border: '#a5b4fc' };
  }

  function esc(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function formatTs(ts) {
    if (!ts) return '—';
    try { return new Date(ts).toLocaleString(); } catch { return ts; }
  }

  function detailsHtml(d) {
    if (!d || typeof d !== 'object') return esc(d);
    return Object.entries(d)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `<span class="detail-pill"><b>${esc(k)}:</b> ${esc(typeof v === 'object' ? JSON.stringify(v) : v)}</span>`)
      .join('');
  }

  const rows = targetLogs.map((log, i) => {
    const c = actionColor(log.action);
    const badge = `<span class="badge" style="background:${c.bg};color:${c.color};border:1px solid ${c.border}">${esc(log.action)}</span>`;
    return `
      <tr class="${i % 2 === 0 ? 'even' : 'odd'}">
        <td class="num">${i + 1}</td>
        <td>${formatTs(log.timestamp)}</td>
        <td>${badge}</td>
        <td><b>${esc(log.actor || log.adminEmail || '—')}</b></td>
        <td class="details">${detailsHtml(log.details || log.data || {})}</td>
      </tr>`;
  }).join('');

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>KasiGuru Audit Logs — ${rangeLabel} — ${dateStr}</title>
<style>
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: #f8fafc; color: #1e293b; padding: 32px 24px; font-size: 14px; }
  .report-header { background: linear-gradient(135deg, #6c3aff 0%, #a855f7 100%);
    color: #fff; border-radius: 16px; padding: 32px 36px; margin-bottom: 28px; }
  .report-header h1 { font-size: 24px; font-weight: 700; letter-spacing: -0.5px; margin-bottom: 4px; }
  .report-header p  { font-size: 13px; opacity: 0.8; margin-top: 4px; }
  .stats { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 24px; }
  .stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
               padding: 16px 24px; flex: 1; min-width: 150px; }
  .stat-card .num { font-size: 28px; font-weight: 700; color: #6c3aff; }
  .stat-card .lbl { font-size: 12px; color: #64748b; margin-top: 2px; }
  .table-wrap { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px;
                overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
  table { width: 100%; border-collapse: collapse; }
  thead th { background: #f1f5f9; padding: 12px 14px; text-align: left;
             font-size: 11px; text-transform: uppercase; letter-spacing: .6px;
             color: #64748b; font-weight: 600; border-bottom: 1px solid #e2e8f0; }
  tbody tr.even { background: #fff; }
  tbody tr.odd  { background: #fafafa; }
  tbody tr:hover { background: #f0f4ff; }
  td { padding: 10px 14px; vertical-align: top; border-bottom: 1px solid #f1f5f9; }
  td.num { color: #94a3b8; font-size: 12px; width: 48px; text-align: center; }
  td.details { max-width: 420px; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 999px;
           font-size: 11px; font-weight: 700; white-space: nowrap; }
  .detail-pill { display: inline-block; background: #f1f5f9; border-radius: 6px;
                 padding: 2px 7px; margin: 2px 3px 2px 0; font-size: 12px; word-break: break-all; }
  .detail-pill b { color: #475569; margin-right: 2px; }
  .footer { margin-top: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  @media(max-width:600px) {
    body { padding: 16px 8px; }
    table { font-size: 12px; }
  }
</style>
</head>
<body>
<div class="report-header">
  <h1>📋 KasiGuru Audit Logs</h1>
  <p>Range: <b>${rangeLabel}</b> &nbsp;•&nbsp; Exported on <b>${exportedAt}</b></p>
</div>
<div class="stats">
  <div class="stat-card"><div class="num">${targetLogs.length}</div><div class="lbl">Total Entries</div></div>
  <div class="stat-card"><div class="num">${[...new Set(targetLogs.map(l => l.actor || l.adminEmail).filter(Boolean))].length}</div><div class="lbl">Unique Admins</div></div>
  <div class="stat-card"><div class="num">${[...new Set(targetLogs.map(l => l.action).filter(Boolean))].length}</div><div class="lbl">Unique Actions</div></div>
</div>
<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th>#</th>
        <th>Timestamp</th>
        <th>Action</th>
        <th>Admin</th>
        <th>Details</th>
      </tr>
    </thead>
    <tbody>${rows}</tbody>
  </table>
</div>
<div class="footer">KasiGuru Admin &mdash; Audit Log Report &mdash; ${dateStr}</div>
</body>
</html>`;

  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);

  const a = document.createElement('a');
  a.href = url;
  a.download = `kasiguru-audit-logs-${label}-${dateStr}.html`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  notify(`Exported ${targetLogs.length} audit logs (${rangeLabel}) as a readable report!`, "success");
};



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
  // The build actually being offered: newest that has not been yanked.
  const latest = releases.find(r => !r.yanked) || releases[0];
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
  const pendingReports = reports.filter(r => (r.status || 'pending') === 'pending');
  const navReportsCount = document.getElementById('nav-reports-count');
  if (navReportsCount) {
    navReportsCount.textContent = pendingReports.length;
    navReportsCount.hidden = pendingReports.length === 0;
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
      <div class="item" onclick="window.openSubmissionModal('${sub.id}')" style="cursor:pointer;" title="Click to review submission details for ${escapeHtml(word)}">
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
    tr.style.cursor = 'pointer';
    const statusBadgeClass = sub.status === 'approved' ? 'badge-approved' : (sub.status === 'rejected' ? 'badge-rejected' : 'badge-pending');
    
    tr.innerHTML = `
      <td data-label="Kasiguranin">
        <button type="button" class="table-link-btn" onclick="window.openSubmissionModal('${sub.id}')" title="Click to view details for ${escapeHtml(sub.kasiguranin)}">
          <strong>${escapeHtml(sub.kasiguranin)}</strong>
        </button>
      </td>
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
          <div class="row-actions">
            <span style="color:var(--muted); font-size:0.85rem; margin-right:4px;">Processed</span>
            <button class="btn btn-outline btn-sm delete-sub-btn" data-id="${sub.id}" title="Delete submission record"><iconsax-icon name="trash" type="bulk" size="14" color="currentColor"></iconsax-icon></button>
          </div>
        `}
      </td>
    `;
    tr.addEventListener('click', (e) => {
      if (e.target.closest('button') || e.target.closest('a') || e.target.closest('input')) return;
      window.openSubmissionModal(sub.id);
    });
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
  tbody.querySelectorAll('.delete-sub-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      deleteSubmission(btn.getAttribute('data-id'));
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

// ── Word Submission Details Modal ───────────────────────────────────────────
window.openSubmissionModal = function(id) {
  const sub = submissions.find(s => s.id === id);
  const body = document.getElementById('submission-modal-body');
  const actions = document.getElementById('submission-modal-actions');
  if (!sub || !body || !actions) return;

  const pos = sub.partOfSpeech || '';
  const aspects = [
    ['Neutral', sub.neutralForm],
    ['Past', sub.pastTense || sub.perfectiveForm],
    ['Present', sub.presentTense || sub.imperfectiveForm],
    ['Future', sub.futureTense || sub.contemplativeForm]
  ].filter(([, v]) => (v || '').trim());

  const row = (label, value) => value
    ? `<dt>${label}</dt><dd>${escapeHtml(value)}</dd>`
    : `<dt>${label}</dt><dd style="color:var(--muted); font-style:italic;">Not recorded</dd>`;

  const statusBadgeClass = sub.status === 'approved' ? 'badge-approved' : (sub.status === 'rejected' ? 'badge-rejected' : 'badge-pending');
  const submittedFormatted = sub.submittedAt
    ? new Date(sub.submittedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : 'Not recorded';

  // Check if existing in master dictionary
  const existing = findExistingWord(sub.kasiguranin, vocabulary);
  let conflictBanner = '';
  if (existing.length > 0) {
    const senses = existing
      .map(e => `<strong>"${escapeHtml(e.kasiguranin)}"</strong> &mdash; ${escapeHtml(e.english || e.tagalog || 'no gloss')}`)
      .join('<br>');
    conflictBanner = `
      <div style="margin-top:var(--s-4); padding:var(--s-3); background:var(--status-pending-tint); border:1px solid var(--status-pending); border-radius:var(--r-ctl);">
        <div style="font-size:var(--t-xs); font-weight:700; color:var(--status-pending); margin-bottom:4px;">
          Already in Master Dictionary (${existing.length} sense${existing.length === 1 ? '' : 's'})
        </div>
        <div style="font-size:var(--t-xs); color:var(--ink); line-height:1.4;">${senses}</div>
      </div>
    `;
  }

  body.innerHTML = `
    <div class="entry-detail-head">
      <span class="headword">${escapeHtml(sub.kasiguranin || '—')}</span>
      ${sub.ipaNotation ? `<span class="ipa">/${escapeHtml(sub.ipaNotation)}/</span>` : ''}
      ${pos ? `<span class="pos">${escapeHtml(pos)}</span>` : ''}
    </div>
    <dl class="deflist">
      ${row('Tagalog', sub.tagalog)}
      ${row('English', sub.english)}
      ${row('Meaning (English)', sub.meaningEnglish)}
      ${row('Meaning (Tagalog)', sub.meaningTagalog)}
      <dt>Category</dt><dd><span class="badge badge-category">${escapeHtml(sub.category || 'General')}</span></dd>
      ${(sub.rootForm && sub.rootForm !== sub.kasiguranin) ? row('Root form', sub.rootForm) : ''}
      <dt>Contributor</dt><dd><strong>${escapeHtml(sub.contributorName || 'Anonymous')}</strong></dd>
      <dt>Submitted</dt><dd>${submittedFormatted}</dd>
      <dt>Status</dt><dd><span class="badge ${statusBadgeClass}">${(sub.status || 'pending').toUpperCase()}</span></dd>
    </dl>
    ${conflictBanner}
    ${aspects.length ? `
      <div style="margin-top:var(--s-5);">
        <dt style="font-size:var(--t-xs); font-weight:700; color:var(--muted);">Verb aspects</dt>
        <div class="aspect-grid">
          ${aspects.map(([label, value]) => `
            <div class="aspect"><span>${label}</span><b>${escapeHtml(value)}</b></div>`).join('')}
        </div>
      </div>` : ''}
    ${(sub.exampleSentence || '').trim() ? `
      <div style="margin-top:var(--s-5);">
        <dt style="font-size:var(--t-xs); font-weight:700; color:var(--muted);">Example sentence</dt>
        <p style="margin:var(--s-2) 0 0;"><i>${escapeHtml(sub.exampleSentence)}</i></p>
      </div>` : ''}`;

  const isPending = (sub.status || 'pending') === 'pending';
  actions.innerHTML = `
    <button type="button" class="btn btn-outline" onclick="closeModal('submission-modal')">Close</button>
    <button type="button" class="btn btn-danger" id="submission-modal-delete">
      <iconsax-icon name="trash" type="bulk" size="17" color="currentColor"></iconsax-icon> Delete entry
    </button>
    ${isPending ? `
      <button type="button" class="btn btn-danger" id="submission-modal-reject" style="background:#b43a3a; border-color:#b43a3a;">
        <iconsax-icon name="close-circle" type="bulk" size="17" color="currentColor"></iconsax-icon> Reject
      </button>
    ` : ''}
    <button type="button" class="btn btn-primary" id="submission-modal-edit">
      <iconsax-icon name="edit" type="bulk" size="17" color="currentColor"></iconsax-icon> Edit entry
    </button>
    ${isPending ? `
      <button type="button" class="btn btn-success" id="submission-modal-approve">
        <iconsax-icon name="tick-circle" type="bulk" size="17" color="currentColor"></iconsax-icon> Approve entry
      </button>
    ` : ''}
  `;

  const editBtn = document.getElementById('submission-modal-edit');
  if (editBtn) {
    editBtn.onclick = () => {
      window.closeModal('submission-modal');
      window.openEditSubmissionModal(id);
    };
  }

  const deleteBtn = document.getElementById('submission-modal-delete');
  if (deleteBtn) {
    deleteBtn.onclick = () => {
      window.deleteSubmission(id);
    };
  }

  const rejectBtn = document.getElementById('submission-modal-reject');
  if (rejectBtn) {
    rejectBtn.onclick = async () => {
      window.closeModal('submission-modal');
      await rejectSubmission(id);
    };
  }

  const approveBtn = document.getElementById('submission-modal-approve');
  if (approveBtn) {
    approveBtn.onclick = async () => {
      window.closeModal('submission-modal');
      await approveSubmission(id);
    };
  }

  window.openModal('submission-modal');
};

// ── Edit Submission Modal ───────────────────────────────────────────────────
window.openEditSubmissionModal = function(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) {
    console.error('Edit submission: item not found for id', id);
    return;
  }

  document.getElementById('edit-sub-id').value = id;
  document.getElementById('edit-sub-kasiguranin').value = sub.kasiguranin || '';
  document.getElementById('edit-sub-tagalog').value = sub.tagalog || '';
  document.getElementById('edit-sub-english').value = sub.english || '';
  document.getElementById('edit-sub-category').value = sub.category || 'General';
  document.getElementById('edit-sub-part-of-speech').value = sub.partOfSpeech || '';
  document.getElementById('edit-sub-meaning-en').value = sub.meaningEnglish || '';
  document.getElementById('edit-sub-meaning-tl').value = sub.meaningTagalog || '';
  document.getElementById('edit-sub-ipa').value = sub.ipaNotation || '';
  document.getElementById('edit-sub-root').value = sub.rootForm || '';
  document.getElementById('edit-sub-past').value = sub.pastTense || sub.perfectiveForm || '';
  document.getElementById('edit-sub-present').value = sub.presentTense || sub.imperfectiveForm || '';
  document.getElementById('edit-sub-future').value = sub.futureTense || sub.contemplativeForm || '';
  document.getElementById('edit-sub-example').value = sub.exampleSentence || '';
  document.getElementById('edit-sub-contributor').value = sub.contributorName || 'Anonymous';

  window.openModal('edit-submission-modal');
};

// ── Delete Submission ───────────────────────────────────────────────────────
window.deleteSubmission = async function(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) return;

  if (!(await confirmDialog({
    title: 'Delete Word Submission?',
    body: `Are you sure you want to permanently delete the submission for "${sub.kasiguranin}"? This action cannot be undone.`,
    confirmLabel: 'Delete',
    danger: true
  }))) return;

  try {
    await deleteDoc(doc(db, "word_submissions", id));
    await logAudit("submission.delete", { id, word: sub.kasiguranin });
    window.closeModal('submission-modal');
    notify(`Deleted submission "${sub.kasiguranin}"`, 'success');
    submissions = submissions.filter(s => s.id !== id);
    renderSubmissionsTable();
    renderOverview();
  } catch (error) {
    console.error("Error deleting submission:", error);
    notify("Failed to delete submission: " + error.message, 'error');
  }
};

// ── Approve Submission ──────────────────────────────────────────────────────
async function approveSubmission(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) return;

  // The last gate before a word enters the dictionary, and until now the only path into `vocabulary`
  // without one. The in-app form warns the contributor while they type, but a warning they can tap
  // past is advisory - this is where a duplicate is actually stopped or knowingly allowed.
  //
  // Compared with the app's own normalisation, not toLowerCase(): "singët" and "singet" are the same
  // word, and a moderator should not have to spot that by eye.
  const existing = findExistingWord(sub.kasiguranin, vocabulary);
  if (existing.length > 0) {
    const senses = existing
      .map(e => `"${escapeHtml(e.kasiguranin)}" — ${escapeHtml(e.english || e.tagalog || 'no gloss')}`)
      .join('<br>');
    const proceed = await confirmDialog({
      title: `"${escapeHtml(sub.kasiguranin)}" is already in the dictionary`,
      body:
        `<p>The master dictionary already has:</p><p style="margin-top:6px;">${senses}</p>` +
        `<p style="margin-top:10px;">Approving adds a second entry. Do that only if this is a genuine ` +
        `homonym — a different word that happens to be spelled the same. If it is the same word, reject ` +
        `the submission instead.</p>`,
      confirmLabel: 'Approve as a separate sense',
      danger: true
    });
    if (!proceed) return;
  }

  try {
    const newVocabRef = doc(collection(db, "vocabulary"));
    await setDoc(newVocabRef, withUpdatedAt({
      kasiguranin: sub.kasiguranin.trim(),
      tagalog: (sub.tagalog || "").trim(),
      english: (sub.english || "").trim(),
      rootForm: (sub.rootForm || sub.kasiguranin).trim(),
      category: sub.category || "General",
      partOfSpeech: sub.partOfSpeech || null,
      meaningEnglish: (sub.meaningEnglish || "").trim() || null,
      meaningTagalog: (sub.meaningTagalog || "").trim() || null,
      ipaNotation: (sub.ipaNotation || "").trim(),
      perfectiveForm: (sub.pastTense || sub.perfectiveForm || "").trim(),
      imperfectiveForm: (sub.presentTense || sub.imperfectiveForm || "").trim(),
      contemplativeForm: (sub.futureTense || sub.contemplativeForm || "").trim(),
      neutralForm: (sub.neutralForm || "").trim() || null,
      exampleSentence: (sub.exampleSentence || "").trim(),
      verifiedByAdmin: true,
      approvedAt: Date.now()
    }));

    await updateDoc(doc(db, "word_submissions", id), {
      status: "approved",
      reviewedAt: Date.now()
    });

    sub.status = 'approved';
    sub.reviewedAt = Date.now();

    await logAudit("submission.approve", { submissionId: id, word: sub.kasiguranin });
    notify(`Successfully approved "${sub.kasiguranin}" and migrated to master dictionary!`, 'success');
    renderSubmissionsTable();
    renderOverview();
  } catch (error) {
    console.error("Error approving submission:", error);
    notify("Failed to approve submission: " + error.message, 'error');
  }
}

// ── Reject Submission ───────────────────────────────────────────────────────
async function rejectSubmission(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) return;

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
    sub.status = 'rejected';
    sub.reviewedAt = Date.now();
    await logAudit("submission.reject", { submissionId: id, word: sub ? sub.kasiguranin : "" });
    notify(`Rejected "${sub.kasiguranin}"`, 'info');
    renderSubmissionsTable();
    renderOverview();
  } catch (error) {
    console.error("Error rejecting submission:", error);
    notify("Error rejecting submission: " + error.message, 'error');
  }
}

// ── User Issue & Word Reports ────────────────────────────────────────────────
function renderReportsTable() {
  const tbody = document.getElementById('reports-tbody');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (reports.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align:center; padding:2.5rem; color:var(--muted);">
          <iconsax-icon name="tick-circle" type="bulk" size="32" color="var(--status-approved)"></iconsax-icon>
          <div style="margin-top:8px;">No issue reports submitted. All clear!</div>
        </td>
      </tr>`;
    return;
  }

  const reportsCountElem = document.getElementById('reports-result-count');
  if (reportsCountElem) {
    const pending = reports.filter(r => (r.status || 'pending') === 'pending').length;
    reportsCountElem.textContent = pending === 0
      ? `${reports.length} total, none pending`
      : `${pending} pending of ${reports.length}`;
  }

  reports.forEach(rep => {
    const tr = document.createElement('tr');
    const status = rep.status || 'pending';
    const statusBadgeClass = status === 'resolved' ? 'badge-approved' : (status === 'dismissed' ? 'badge-rejected' : 'badge-pending');
    const dateFormatted = rep.submittedAt ? new Date(rep.submittedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-';

    const hasPhoto = (rep.photoBase64 && rep.photoBase64.length > 50) || (rep.photoUrl && rep.photoUrl.length > 0);
    const photoSrc = rep.photoBase64 || rep.photoUrl;

    tr.innerHTML = `
      <td data-label="Date" style="white-space:nowrap; font-size:0.85rem; color:var(--muted);">${dateFormatted}</td>
      <td data-label="Category"><span class="badge badge-category">${escapeHtml(rep.category || 'Bug / Issue')}</span></td>
      <td data-label="Title & Target">
        <strong>${escapeHtml(rep.title || 'Report')}</strong>
        ${rep.targetWord ? `<div style="font-size:0.85rem; color:var(--primary); font-weight:600; margin-top:2px;">Word: ${escapeHtml(rep.targetWord)}</div>` : ''}
        ${rep.targetScreen ? `<div style="font-size:0.8rem; color:var(--muted);">Screen: ${escapeHtml(rep.targetScreen)}</div>` : ''}
      </td>
      <td data-label="Description" style="max-width:260px; font-size:0.88rem; line-height:1.4;">
        ${escapeHtml(rep.description || '-')}
      </td>
      <td data-label="Evidence">
        ${hasPhoto ? `
          <div style="cursor:pointer; display:inline-block;" onclick="window.viewReportEvidence('${photoSrc}', '${escapeHtml(rep.title || 'Evidence')}')" title="Click to enlarge">
            <img src="${photoSrc}" alt="Evidence Thumbnail" style="width:48px; height:48px; object-fit:cover; border-radius:6px; border:1px solid var(--border); box-shadow:var(--shadow-sm);" />
            <div style="font-size:0.75rem; color:var(--primary); font-weight:600; text-align:center;">Enlarge</div>
          </div>
        ` : `<span style="color:var(--muted); font-size:0.82rem;">None</span>`}
      </td>
      <td data-label="Reporter & Device" style="font-size:0.82rem;">
        <div><strong>${escapeHtml(rep.reporterName || 'Anonymous')}</strong></div>
        ${rep.reporterEmail ? `<div style="color:var(--muted);">${escapeHtml(rep.reporterEmail)}</div>` : ''}
        ${rep.deviceInfo || rep.appVersion ? `<div style="color:var(--muted); margin-top:4px; font-size:0.78rem;">v${escapeHtml(rep.appVersion || '')} • ${escapeHtml(rep.deviceInfo || '')}</div>` : ''}
      </td>
      <td data-label="Status"><span class="badge ${statusBadgeClass}">${status.toUpperCase()}</span></td>
      <td data-label="Actions">
        <div class="row-actions">
          ${status === 'pending' ? `
            <button class="btn btn-success btn-sm resolve-report-btn" data-id="${rep.id}"><iconsax-icon name="tick-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Resolve</button>
            <button class="btn btn-danger btn-sm dismiss-report-btn" data-id="${rep.id}"><iconsax-icon name="close-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Dismiss</button>
          ` : `
            <button class="btn btn-outline btn-sm delete-report-btn" data-id="${rep.id}"><iconsax-icon name="trash" type="bulk" size="14" color="currentColor"></iconsax-icon></button>
          `}
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('.resolve-report-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      resolveReport(btn.getAttribute('data-id'));
    });
  });
  tbody.querySelectorAll('.dismiss-report-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      dismissReport(btn.getAttribute('data-id'));
    });
  });
  tbody.querySelectorAll('.delete-report-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      deleteReport(btn.getAttribute('data-id'));
    });
  });

  applyTableSemantics();
}

function renderReportsError(message) {
  const tbody = document.getElementById('reports-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--status-rejected); padding:2rem;">${escapeHtml(message)}</td></tr>`;
  }
}

window.viewReportEvidence = function(imgSrc, title) {
  const modalImg = document.getElementById('evidence-modal-img');
  const modalTitle = document.getElementById('evidence-modal-title');
  if (modalImg) modalImg.src = imgSrc;
  if (modalTitle) modalTitle.textContent = title || "Photo Evidence";
  const modal = document.getElementById('evidence-modal');
  if (modal) modal.classList.add('active');
};

async function resolveReport(id) {
  try {
    await updateDoc(doc(db, "issue_reports", id), {
      status: "resolved",
      resolvedAt: Date.now()
    });
    const rep = reports.find(r => r.id === id);
    await logAudit("report.resolve", { reportId: id, category: rep?.category, title: rep?.title });
    notify("Report marked as resolved.", "success");
  } catch (error) {
    console.error("Error resolving report:", error);
    notify("Failed to resolve report: " + error.message, "error");
  }
}

async function dismissReport(id) {
  try {
    await updateDoc(doc(db, "issue_reports", id), {
      status: "dismissed",
      dismissedAt: Date.now()
    });
    const rep = reports.find(r => r.id === id);
    await logAudit("report.dismiss", { reportId: id, category: rep?.category, title: rep?.title });
    notify("Report dismissed.", "info");
  } catch (error) {
    console.error("Error dismissing report:", error);
    notify("Failed to dismiss report: " + error.message, "error");
  }
}

async function deleteReport(id) {
  if (!(await confirmDialog({
    title: 'Delete this report record?',
    body: 'This will permanently remove the report and attached photo evidence from the database.',
    confirmLabel: 'Delete', danger: true
  }))) return;

  try {
    await deleteDoc(doc(db, "issue_reports", id));
    await logAudit("report.delete", { reportId: id });
    notify("Report deleted.", "info");
  } catch (error) {
    console.error("Error deleting report:", error);
    notify("Failed to delete report: " + error.message, "error");
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
  document.getElementById('edit-input-theme').value = item.theme || '';
  document.getElementById('edit-input-meaning-en').value = item.meaningEnglish || '';
  document.getElementById('edit-input-meaning-tl').value = item.meaningTagalog || '';
  document.getElementById('edit-input-ipa').value = item.ipaNotation || '';
  document.getElementById('edit-input-neutral').value = item.neutralForm || '';
  document.getElementById('edit-input-perfective').value = item.perfectiveForm || '';
  document.getElementById('edit-input-imperfective').value = item.imperfectiveForm || '';
  document.getElementById('edit-input-contemplative').value = item.contemplativeForm || '';
  document.getElementById('edit-input-example1').value = item.exampleSentence || '';
  document.getElementById('edit-input-example1-translation').value = item.exampleTranslation || '';
  document.getElementById('edit-input-example2').value = item.exampleSentence2 || '';
  document.getElementById('edit-input-example2-translation').value = item.exampleTranslation2 || '';
  document.getElementById('edit-input-example1-translation-tl').value = item.exampleTranslationTagalog || '';
  document.getElementById('edit-input-example2-translation-tl').value = item.exampleTranslation2Tagalog || '';
  document.getElementById('edit-input-example-source').value = item.exampleSource || '';
  loadAudioEditorForWord('edit-input', item);

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

      // Deduplicated on the shared normalisation, like every other way a word enters the
      // dictionary. This path had no check at all: each row was written to a freshly generated
      // document id, so importing the same file twice simply doubled the corpus. The dialog said
      // "existing entries with the same id are overwritten", which was never true of a random id.
      const existingWords = new Set(
        vocabulary.map(v => normaliseWord(v.kasiguranin || '')).filter(Boolean)
      );
      const seenInThisImport = new Set();
      const fresh = [];
      let skipped = 0;
      for (const entry of entries) {
        const key = normaliseWord(entry.kasiguranin || '');
        if (!key || existingWords.has(key) || seenInThisImport.has(key)) { skipped++; continue; }
        seenInThisImport.add(key);
        fresh.push(entry);
      }

      if (fresh.length === 0) {
        notify(`Nothing to import from "${file.name}" — all ${entries.length} entries are already in the dictionary.`, 'error');
        return;
      }

      if (!(await confirmDialog({
        title: `Import ${fresh.length} new record${fresh.length === 1 ? '' : 's'}?`,
        body:
          `Parsed from <strong>${escapeHtml(file.name)}</strong>.` +
          (skipped > 0
            ? ` ${skipped} of ${entries.length} are already in the dictionary and will be skipped.`
            : ''),
        confirmLabel: 'Import'
      }))) return;

      let count = 0;
      for (const entry of fresh) {
        const newDoc = doc(collection(db, "vocabulary"));
        // Bulk-imported rows are stamped like any other write, or a spreadsheet import
        // would land in Firestore invisible to the app's incremental sync.
        await setDoc(newDoc, withUpdatedAt(entry));
        count++;
      }

      await logAudit("vocabulary.import_sql", { file: file.name, imported: count, skipped });
      notify(`Imported ${count} new entr${count === 1 ? 'y' : 'ies'} from ${escapeHtml(file.name)}. Skipped ${skipped} already in the dictionary.`, 'success');
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
      
      // Compared on the shared normalisation, not a bare toLowerCase().
      //
      // This importer was the one path into `vocabulary` that did not use it, and it is the path
      // every word in the corpus arrived through. The cost is measurable: of the seven entries the
      // dictionary carries twice with the same meaning, all seven are pairs this rule sees as one
      // word and toLowerCase() saw as two -- "tëllën"/"tël-lën", "uló"/"ulo", "laya"/"layâ",
      // "kulapnet"/"kulapnët". word-normalize.js says the contributor-facing and moderator-facing
      // checks must agree on what "the same word" means; an importer that disagrees undoes both.
      const existingWords = new Set(
        vocabulary.map(v => normaliseWord(v.kasiguranin || '')).filter(Boolean)
      );
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
        
        // A row whose headword folds to nothing (punctuation only) is not comparable, so it is
        // skipped rather than imported under an empty key that would then match every other such row.
        const wordKey = normaliseWord(wordClean);
        if (!wordKey) { skipped++; continue; }

        if (existingWords.has(wordKey) || wordsInThisImport.has(wordKey)) {
          skipped++;
          continue;
        }

        wordsInThisImport.add(wordKey);

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


// ── Word pronunciation audio ───────────────────────────────────────────────
// Clips live in Firestore at word_audio/{key} as raw bytes, one document per word sense — the same
// pattern story_page_images uses, and for the same reason: no Firebase Storage on the Spark plan.
// The vocabulary doc carries only a pointer (audioResName = key) plus audioUpdatedAt as a
// cache-buster; the Android app fetches the bytes on first play and caches them to disk, falling
// back to text-to-speech when a word has none.

const AUDIO_MAX_BYTES = 400 * 1024;               // must match the firestore.rules word_audio cap
const AUDIO_MIME_OK = new Set([
  'audio/mpeg', 'audio/mp3', 'audio/mp4', 'audio/aac', 'audio/x-m4a',
  'audio/ogg', 'audio/opus', 'audio/webm'
]);
const AUDIO_DEFAULT_STATUS =
  'Optional. A short m4a / mp3 / ogg clip, under 400 KB. Without it the app speaks the word with text-to-speech.';

// key = slug(kasiguranin)__slug(english). Kept in exact step with the app's WordAudioRepository /
// AudioPlayerManager: both sides lowercase, collapse every run of non-[a-z0-9] to "_", trim "_".
function audioKey(kasiguranin, english) {
  const slug = s => (s || '').trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
  return `${slug(kasiguranin)}__${slug(english)}`;
}

// prefix ('input' | 'edit-input') -> { blob, mimeType, name, remove, url }
const audioEditors = new Map();

function audioEls(prefix) {
  return {
    file:    document.getElementById(`${prefix}-audio-file`),
    pick:    document.getElementById(`${prefix}-audio-pick-btn`),
    preview: document.getElementById(`${prefix}-audio-preview`),
    remove:  document.getElementById(`${prefix}-audio-remove-btn`),
    status:  document.getElementById(`${prefix}-audio-status`)
  };
}

function resetAudioEditor(prefix) {
  const st = audioEditors.get(prefix);
  if (st?.url) URL.revokeObjectURL(st.url);
  audioEditors.delete(prefix);
  const el = audioEls(prefix);
  if (!el.file) return;
  el.file.value = '';
  if (el.preview) { el.preview.removeAttribute('src'); el.preview.style.display = 'none'; }
  if (el.remove)  el.remove.style.display = 'none';
  if (el.status)  { el.status.textContent = AUDIO_DEFAULT_STATUS; el.status.style.color = 'var(--muted)'; }
}

// Populate the edit editor from a word's stored state.
function loadAudioEditorForWord(prefix, item) {
  resetAudioEditor(prefix);
  const el = audioEls(prefix);
  if (!el.status) return;
  if (item && item.audioResName) {
    const when = item.audioUpdatedAt ? new Date(item.audioUpdatedAt).toLocaleDateString() : 'earlier';
    el.status.textContent = `This word has a recording (uploaded ${when}). Choose a file to replace it, or remove it.`;
    if (el.remove) el.remove.style.display = '';
  }
}

function initAudioEditor(prefix) {
  const el = audioEls(prefix);
  if (!el.file || !el.pick) return;

  el.pick.addEventListener('click', () => el.file.click());

  el.file.addEventListener('change', () => {
    const f = el.file.files && el.file.files[0];
    if (!f) return;
    const okType = AUDIO_MIME_OK.has(f.type) || /\.(m4a|mp3|ogg|oga|aac|opus)$/i.test(f.name);
    if (!okType) {
      notify('That is not an audio file the app can play. Use m4a, mp3 or ogg.', 'error');
      el.file.value = '';
      return;
    }
    if (f.size > AUDIO_MAX_BYTES) {
      notify(`That clip is ${Math.round(f.size / 1024)} KB, over the ${Math.round(AUDIO_MAX_BYTES / 1024)} KB limit. Trim it or re-export it smaller and try again.`, 'error');
      el.file.value = '';
      return;
    }
    const prev = audioEditors.get(prefix);
    if (prev?.url) URL.revokeObjectURL(prev.url);
    const url = URL.createObjectURL(f);
    audioEditors.set(prefix, { blob: f, mimeType: f.type || 'audio/mp4', name: f.name, remove: false, url });
    if (el.preview) { el.preview.src = url; el.preview.style.display = ''; }
    if (el.remove)  el.remove.style.display = '';
    if (el.status)  {
      el.status.textContent = `New clip: ${f.name} (${Math.round(f.size / 1024)} KB). It uploads when you save.`;
      el.status.style.color = 'var(--violet)';
    }
  });

  if (el.remove) {
    el.remove.addEventListener('click', () => {
      const prev = audioEditors.get(prefix);
      if (prev?.url) URL.revokeObjectURL(prev.url);
      audioEditors.set(prefix, { blob: null, mimeType: '', name: '', remove: true, url: null });
      el.file.value = '';
      if (el.preview) { el.preview.removeAttribute('src'); el.preview.style.display = 'none'; }
      if (el.status)  {
        el.status.textContent = 'The recording will be removed on save — the app goes back to text-to-speech for this word.';
        el.status.style.color = 'var(--violet)';
      }
    });
  }
}

// { kind: 'set', blob, mimeType } | { kind: 'remove' } | null
function pendingAudio(prefix) {
  const st = audioEditors.get(prefix);
  if (!st) return null;
  if (st.remove) return { kind: 'remove' };
  if (st.blob)   return { kind: 'set', blob: st.blob, mimeType: st.mimeType };
  return null;
}

// Writes / removes word_audio/{key} for a word and returns the vocab-doc fields to merge into the
// write ({} when there is nothing to do, or when the audio write failed).
//
// The audio write is best-effort: it must never block the word save. If word_audio is denied
// (the security rule not yet deployed) or the upload fails for any other reason, the word still
// saves — just without an audio pointer — and the admin is told the clip did not go through.
async function commitWordAudio(prefix, kasiguranin, english) {
  const pending = pendingAudio(prefix);
  if (!pending) return {};
  const key = audioKey(kasiguranin, english);

  if (pending.kind === 'set') {
    try {
      const buf = new Uint8Array(await pending.blob.arrayBuffer());
      await setDoc(doc(db, 'word_audio', key), {
        data: Bytes.fromUint8Array(buf),
        mimeType: pending.mimeType || 'audio/mp4',
        byteSize: buf.length,
        kasiguranin,
        english: english || '',
        updatedAt: new Date().toISOString()
      });
      await logAudit('vocabulary.audio', { word: kasiguranin, action: 'set', bytes: buf.length });
      return { audioResName: key, audioUpdatedAt: Date.now() };
    } catch (err) {
      console.warn('word_audio upload failed', key, err);
      notify('The word was saved, but the audio clip could not be uploaded: ' + (err.message || err), 'error');
      return {};
    }
  }

  // remove
  try {
    await deleteDoc(doc(db, 'word_audio', key));
    await logAudit('vocabulary.audio', { word: kasiguranin, action: 'remove' });
    return { audioResName: '', audioUpdatedAt: Date.now() };
  } catch (err) {
    console.warn('word_audio delete failed', key, err);
    notify('The word was saved, but the audio clip could not be removed: ' + (err.message || err), 'error');
    return {};
  }
}

function initAudioEditors() {
  initAudioEditor('input');
  initAudioEditor('edit-input');
  // The add modal has no JS open handler (inline onclick), so clear stale pending state on open.
  const openModalInner = window.openModal;
  window.openModal = function(id) {
    if (id === 'add-vocab-modal') resetAudioEditor('input');
    // Always open the release modal in "publish" state; editRelease() populates it right after.
    if (id === 'publish-release-modal') resetReleaseForm();
    return openModalInner.call(window, id);
  };
}

// ── Form Listeners ──────────────────────────────────────────────────────────
function initFormListeners() {
  initAudioEditors();

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
      const theme = document.getElementById('input-theme').value;
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
      const example1TranslationTl = document.getElementById('input-example1-translation-tl').value.trim();
      const example2TranslationTl = document.getElementById('input-example2-translation-tl').value.trim();
      const exampleSource = document.getElementById('input-example-source').value.trim();
      const meaningEnglish = document.getElementById('input-meaning-en').value.trim();
      const meaningTagalog = document.getElementById('input-meaning-tl').value.trim();

      if (!word) { notify("Please enter the Kasiguranin word.", 'error'); return; }

      const isDuplicate = findExistingWord(word, vocabulary).length > 0;
      if (isDuplicate) {
        if (!(await confirmDialog({
          title: `"${word}" already exists`,
          body: 'The master dictionary already has this Kasiguranin word. Adding it again creates a duplicate entry.',
          confirmLabel: 'Add anyway'
        }))) return;
      }

      try {
        const audioFields = await commitWordAudio('input', word, english);
        await addDoc(collection(db, "vocabulary"), {
          kasiguranin: word,
          tagalog: tagalog || null,
          english: english || null,
          category: category,
          theme: theme || "",
          partOfSpeech: partOfSpeech || null,
          meaningEnglish: meaningEnglish || null,
          meaningTagalog: meaningTagalog || null,
          ipaNotation: ipa || null,
          neutralForm: neutral || null,
          perfectiveForm: perfective || null,
          imperfectiveForm: imperfective || null,
          contemplativeForm: contemplative || null,
          exampleSentence: example1 || null,
          exampleTranslation: example1Translation || null,
          exampleSentence2: example2 || null,
          exampleTranslation2: example2Translation || null,
          // Firestore-only: the app reads neither, so no Room migration. The Tagalog gloss is for
          // the written record, and the source is what lets a sentence be cited in the thesis.
          exampleTranslationTagalog: example1TranslationTl || null,
          exampleTranslation2Tagalog: example2TranslationTl || null,
          exampleSource: exampleSource || null,
          createdAt: Date.now(),
          // A new word needs updatedAt too, not just createdAt — the app's incremental
          // sync filters on updatedAt, so without it a freshly added word would not
          // reach anyone until the next weekly full reconcile.
          updatedAt: Date.now(),
          ...audioFields
        });
        await logAudit("vocabulary.create", { word });
        addVocabForm.reset();
        resetAudioEditor('input');
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
      const theme = document.getElementById('edit-input-theme').value;
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
      const example1TranslationTl = document.getElementById('edit-input-example1-translation-tl').value.trim();
      const example2TranslationTl = document.getElementById('edit-input-example2-translation-tl').value.trim();
      const exampleSource = document.getElementById('edit-input-example-source').value.trim();
      const meaningEnglish = document.getElementById('edit-input-meaning-en').value.trim();
      const meaningTagalog = document.getElementById('edit-input-meaning-tl').value.trim();

      if (!word) { notify("Please enter the Kasiguranin word.", 'error'); return; }

      try {
        const audioFields = await commitWordAudio('edit-input', word, english);
        await updateDoc(doc(db, "vocabulary", id), {
          kasiguranin: word,
          tagalog: tagalog || null,
          english: english || null,
          category: category,
          theme: theme || "",
          partOfSpeech: partOfSpeech || null,
          meaningEnglish: meaningEnglish || null,
          meaningTagalog: meaningTagalog || null,
          ipaNotation: ipa || null,
          neutralForm: neutral || null,
          perfectiveForm: perfective || null,
          imperfectiveForm: imperfective || null,
          contemplativeForm: contemplative || null,
          exampleSentence: example1 || null,
          exampleTranslation: example1Translation || null,
          exampleSentence2: example2 || null,
          exampleTranslation2: example2Translation || null,
          // Firestore-only: the app reads neither, so no Room migration. The Tagalog gloss is for
          // the written record, and the source is what lets a sentence be cited in the thesis.
          exampleTranslationTagalog: example1TranslationTl || null,
          exampleTranslation2Tagalog: example2TranslationTl || null,
          exampleSource: exampleSource || null,
          updatedAt: Date.now(),
          ...audioFields
        });
        await logAudit("vocabulary.update", { id, word });
        editVocabForm.reset();
        resetAudioEditor('edit-input');
        closeModal('edit-vocab-modal');
        notify(`Successfully updated "${word}"!`, 'success');
      } catch (error) {
        notify("Error updating word: " + error.message, 'error');
      }
    });
  }

  const editSubForm = document.getElementById('edit-submission-form');
  if (editSubForm) {
    editSubForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('edit-sub-id').value;
      const word = document.getElementById('edit-sub-kasiguranin').value.trim();
      const tagalog = document.getElementById('edit-sub-tagalog').value.trim();
      const english = document.getElementById('edit-sub-english').value.trim();
      const category = document.getElementById('edit-sub-category').value;
      const partOfSpeech = document.getElementById('edit-sub-part-of-speech').value;
      const meaningEn = document.getElementById('edit-sub-meaning-en').value.trim();
      const meaningTl = document.getElementById('edit-sub-meaning-tl').value.trim();
      const ipa = document.getElementById('edit-sub-ipa').value.trim();
      const root = document.getElementById('edit-sub-root').value.trim();
      const past = document.getElementById('edit-sub-past').value.trim();
      const present = document.getElementById('edit-sub-present').value.trim();
      const future = document.getElementById('edit-sub-future').value.trim();
      const example = document.getElementById('edit-sub-example').value.trim();
      const contributor = document.getElementById('edit-sub-contributor').value.trim();

      if (!word) {
        notify("Please enter the Kasiguranin word.", 'error');
        return;
      }

      try {
        const updated = {
          kasiguranin: word,
          tagalog: tagalog || null,
          english: english || null,
          category: category,
          partOfSpeech: partOfSpeech || null,
          meaningEnglish: meaningEn || null,
          meaningTagalog: meaningTl || null,
          ipaNotation: ipa || null,
          rootForm: root || word,
          pastTense: past || null,
          presentTense: present || null,
          futureTense: future || null,
          exampleSentence: example || null,
          contributorName: contributor || 'Anonymous'
        };

        await updateDoc(doc(db, "word_submissions", id), updated);
        await logAudit("submission.update", { id, word });

        const sub = submissions.find(s => s.id === id);
        if (sub) {
          Object.assign(sub, updated);
        }

        closeModal('edit-submission-modal');
        notify(`Successfully updated submission "${word}"!`, 'success');
        renderSubmissionsTable();
        window.openSubmissionModal(id);
      } catch (error) {
        console.error("Error updating submission:", error);
        notify("Error updating submission: " + error.message, 'error');
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

      const editingId = document.getElementById('rel-editing-id')?.value || '';

      if (isNaN(code) || code <= 0) { notify("Please enter a valid positive integer version code (e.g. 1, 2, 3).", 'error'); return; }
      if (!name) { notify("Please enter a version name (e.g. 1.0.0).", 'error'); return; }
      if (!url.startsWith('http://') && !url.startsWith('https://')) { notify("Direct APK Download link must start with http:// or https://", 'error'); return; }
      if (forceUpdate && !(await confirmDialog({
        title: `Mark v${name} as a required update?`,
        body: 'Every user sees a banner they cannot dismiss until they update.',
        confirmLabel: 'Confirm required update', danger: true
      }))) return;

      try {
        if (editingId) {
          // Editing an existing release: leave versionName (the doc id) and releasedAt alone, and
          // never touch `yanked` here — that is the Yank/Restore control's job.
          await updateDoc(doc(db, 'app_releases', editingId), {
            versionCode: code,
            apkUrl: url,
            releaseNotes: notes,
            forceUpdate: forceUpdate
          });
          await logAudit('release.edit', { versionName: name, versionCode: code, apkUrl: url, forceUpdate });
          notify(`Saved changes to v${name}.`, 'success');
        } else {
          // Deterministic doc id (vX.Y.Z), matching what CI's publish_release.js writes for the
          // same version — setDoc + merge means republishing a version CI already wrote only
          // overwrites these known fields instead of creating a second, duplicate doc via
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
          notify(`Successfully published KasiGuru v${name} APK release!`, 'success');
        }
        resetReleaseForm();
        closeModal('publish-release-modal');
      } catch (err) {
        notify("Failed to save release: " + err.message, 'error');
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

  const gapFilter = document.getElementById('filter-vocab-gap');
  if (gapFilter) {
    gapFilter.addEventListener('change', () => {
      vocabPage = 1;
      renderVocabularyTable();
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




// ── Stage review ────────────────────────────────────────────────────────────
// Where a proposal from functions/tag_themes.js becomes a fact, and the only place it can.
//
// The tagger reads a word's English gloss and proposes which learning-tree stage should teach it and
// what part of speech it is, because both stored fields are demonstrably wrong: `category` files
// "flight" and "fragile" under Colors & Shapes, and `partOfSpeech` marks 1,010 of 1,246 words Noun,
// including *angay* (go) and *saneg* (hear). But a script reading glosses is a guess, and this corpus
// is the primary record of an endangered language, so nothing it proposes reaches a learner until a
// person here agrees with it. The tagger writes only to `themeProposed` / `partOfSpeechProposed`;
// this screen is what copies a proposal into the live `theme` and `partOfSpeech`.
//
// Confidence is shown rather than hidden because the two passes are not equally trustworthy: a gloss
// match (0.9) *is* the word's meaning, while a definition match (0.6) merely mentions it. Accepting a
// hundred gloss matches at once is reasonable; accepting definition matches unread is not, which is
// why the bulk button only ever takes the former.

let stageFilter = '';
let stageConfidenceFilter = '';

/** A proposal worth showing: one that exists and would actually change something. */
function stageProposals() {
  return vocabulary.filter(w => {
    const themeChanges = (w.themeProposed || '') && (w.themeProposed !== (w.theme || ''));
    const posChanges = (w.partOfSpeechProposed || '') && (w.partOfSpeechProposed !== (w.partOfSpeech || ''));
    return themeChanges || posChanges;
  });
}

/**
 * How far a word's proposals can be trusted: the strongest evidence behind any of them.
 *
 * Taken across both proposals rather than from the stage alone, because a word can be proposed a
 * part of speech and no stage — *kagi* (word) is a noun by its definition but belongs to no stage in
 * the map. Reading only the stage's confidence left those rows at `undefined`, which filtered them
 * out of the gloss-match view and out of bulk accept, so they could only ever be found by clearing
 * every filter. A proposal you cannot see is a proposal that never gets reviewed.
 */
function proposalConfidence(word) {
  return Math.max(
    Number(word.themeProposedConfidence) || 0,
    Number(word.partOfSpeechProposedConfidence) || 0
  );
}

function stageProposalsFiltered() {
  return stageProposals().filter(w => {
    if (stageFilter && w.themeProposed !== stageFilter) return false;
    const confidence = proposalConfidence(w);
    if (stageConfidenceFilter === 'high' && confidence < 0.9) return false;
    if (stageConfidenceFilter === 'low' && confidence >= 0.9) return false;
    return true;
  });
}

function initStageReview() {
  const filter = document.getElementById('stages-filter');
  const confidence = document.getElementById('stages-confidence');
  const acceptAll = document.getElementById('stages-accept-confident');

  if (filter) filter.addEventListener('change', () => { stageFilter = filter.value; renderStageReview(); });
  if (confidence) confidence.addEventListener('change', () => { stageConfidenceFilter = confidence.value; renderStageReview(); });
  if (acceptAll) acceptAll.addEventListener('click', acceptConfidentStageProposals);
}

function renderStageReview() {
  const tbody = document.getElementById('stages-tbody');
  if (!tbody) return;

  const all = stageProposals();
  const rows = stageProposalsFiltered();

  // Keep the stage filter's options in step with whatever the tagger actually proposed, so a stage
  // that was renamed or dropped from the map never lingers as a dead option.
  const filter = document.getElementById('stages-filter');
  if (filter) {
    const stages = [...new Set(all.map(w => w.themeProposed).filter(Boolean))].sort();
    const current = filter.value;
    filter.innerHTML = '<option value="">All stages</option>' +
      stages.map(s => `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`).join('');
    filter.value = current;
  }

  const navCount = document.getElementById('nav-stages-count');
  if (navCount) {
    navCount.textContent = all.length;
    navCount.hidden = all.length === 0;
  }

  const count = document.getElementById('stages-result-count');
  if (count) {
    const confident = all.filter(w => proposalConfidence(w) >= 0.9).length;
    count.textContent = all.length === 0
      ? 'No proposals waiting'
      : `${rows.length} shown of ${all.length} waiting · ${confident} from the gloss`;
  }

  tbody.innerHTML = '';
  if (rows.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align:center; padding:2.5rem; color:var(--muted);">
          <iconsax-icon name="tick-circle" type="bulk" size="32" color="var(--gold-ink)"></iconsax-icon>
          <div style="margin-top:8px;">${all.length === 0
            ? 'Nothing to review. Run <code>node functions/tag_themes.js &lt;key.json&gt; --apply</code> to propose stages.'
            : 'No proposals match this filter.'}</div>
        </td>
      </tr>`;
    applyTableSemantics();
    return;
  }

  rows.forEach(w => {
    const tr = document.createElement('tr');
    const confident = proposalConfidence(w) >= 0.9;
    const themeCell = w.themeProposed && w.themeProposed !== (w.theme || '')
      ? `<span style="color:var(--muted);">${escapeHtml(w.theme || 'none')}</span> &rarr;
         <strong>${escapeHtml(w.themeProposed)}</strong>`
      : `<span style="color:var(--muted);">${escapeHtml(w.theme || 'none')}</span>`;
    const posCell = w.partOfSpeechProposed && w.partOfSpeechProposed !== (w.partOfSpeech || '')
      ? `<span style="color:var(--muted);">${escapeHtml(w.partOfSpeech || 'none')}</span> &rarr;
         <strong>${escapeHtml(w.partOfSpeechProposed)}</strong>`
      : `<span style="color:var(--muted);">${escapeHtml(w.partOfSpeech || 'none')}</span>`;

    tr.innerHTML = `
      <td data-label="Word"><strong>${escapeHtml(w.kasiguranin || '')}</strong></td>
      <td data-label="English">${escapeHtml(w.english || '-')}</td>
      <td data-label="Stage">${themeCell}</td>
      <td data-label="Part of speech">${posCell}</td>
      <td data-label="Why">
        <span class="badge ${confident ? 'badge-approved' : 'badge-pending'}">
          ${confident ? 'gloss' : 'definition'}
        </span>
        <div style="font-size:0.8rem; color:var(--muted); margin-top:4px;">
          ${escapeHtml(w.themeProposedEvidence || w.partOfSpeechProposedEvidence || '')}
        </div>
      </td>
      <td data-label="Actions">
        <div class="row-actions">
          <button class="btn btn-success btn-sm stage-accept-btn" data-id="${w.id}">
            <iconsax-icon name="tick-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Accept
          </button>
          <button class="btn btn-outline btn-sm stage-reject-btn" data-id="${w.id}">
            <iconsax-icon name="close-circle" type="bulk" size="16" color="currentColor"></iconsax-icon> Reject
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('.stage-accept-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      acceptStageProposal(btn.getAttribute('data-id'));
    });
  });
  tbody.querySelectorAll('.stage-reject-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      markRowLeaving(btn);
      rejectStageProposal(btn.getAttribute('data-id'));
    });
  });

  applyTableSemantics();
}

/**
 * The patch that turns one word's proposal into its live value.
 *
 * Proposal fields are blanked rather than left in place, so a word never sits in the queue twice and
 * a re-run of the tagger can tell an unreviewed word from a settled one. Blanked, not deleted, to
 * match how every other optional field in this corpus is cleared.
 */
function acceptedStagePatch(word) {
  const patch = {
    themeProposed: '',
    themeProposedConfidence: '',
    themeProposedEvidence: '',
    partOfSpeechProposed: '',
    partOfSpeechProposedConfidence: '',
    partOfSpeechProposedEvidence: ''
  };
  if (word.themeProposed) patch.theme = word.themeProposed;
  if (word.partOfSpeechProposed) patch.partOfSpeech = word.partOfSpeechProposed;
  return withUpdatedAt(patch);
}

async function acceptStageProposal(id) {
  const word = vocabulary.find(w => w.id === id);
  if (!word) return;
  try {
    await updateDoc(doc(db, 'vocabulary', id), acceptedStagePatch(word));
    await logAudit('stage.accept', {
      word: word.kasiguranin,
      theme: word.themeProposed || null,
      partOfSpeech: word.partOfSpeechProposed || null,
      confidence: word.themeProposedConfidence || null
    });
    notify(`"${word.kasiguranin}" moved to ${word.themeProposed || word.partOfSpeech}.`, 'success');
  } catch (e) {
    console.error('Stage accept failed:', e);
    notify('Could not save that change. Check your connection and try again.', 'error');
    renderStageReview();
  }
}

async function rejectStageProposal(id) {
  const word = vocabulary.find(w => w.id === id);
  if (!word) return;
  try {
    await updateDoc(doc(db, 'vocabulary', id), withUpdatedAt({
      themeProposed: '',
      themeProposedConfidence: '',
      themeProposedEvidence: '',
      partOfSpeechProposed: '',
      partOfSpeechProposedConfidence: '',
      partOfSpeechProposedEvidence: ''
    }));
    await logAudit('stage.reject', { word: word.kasiguranin, rejected: word.themeProposed || null });
    notify(`Proposal for "${word.kasiguranin}" discarded. The word keeps its current stage.`, 'success');
  } catch (e) {
    console.error('Stage reject failed:', e);
    notify('Could not discard that proposal. Check your connection and try again.', 'error');
    renderStageReview();
  }
}

/**
 * Accepts every gloss-matched proposal currently in view, in batches.
 *
 * Only the 0.9 pass, and only what the filter is already showing, so "accept all" can never reach
 * further than what the reviewer is looking at. Definition matches are excluded by design — they are
 * the ones worth reading one at a time.
 */
async function acceptConfidentStageProposals() {
  const rows = stageProposalsFiltered().filter(w => proposalConfidence(w) >= 0.9);
  if (rows.length === 0) {
    notify('No gloss-matched proposals in view to accept.', 'error');
    return;
  }

  const proceed = await confirmDialog({
    title: `Accept ${rows.length} proposal${rows.length === 1 ? '' : 's'}?`,
    body:
      `<p>This sets the learning-tree stage and part of speech for ${rows.length} word` +
      `${rows.length === 1 ? '' : 's'} from the tagger's gloss match.</p>` +
      `<p style="margin-top:10px;">Words matched from their written definition are not included — ` +
      `those are worth reading one at a time.</p>`,
    confirmLabel: 'Accept them'
  });
  if (!proceed) return;

  try {
    for (let i = 0; i < rows.length; i += 400) {
      const chunk = rows.slice(i, i + 400);
      const batch = writeBatch(db);
      chunk.forEach(w => batch.update(doc(db, 'vocabulary', w.id), acceptedStagePatch(w)));
      await batch.commit();
    }
    await logAudit('stage.accept_bulk', { count: rows.length, stage: stageFilter || 'all' });
    notify(`Accepted ${rows.length} proposal${rows.length === 1 ? '' : 's'}.`, 'success');
  } catch (e) {
    console.error('Bulk stage accept failed:', e);
    notify('Could not save those changes. Some may have been applied — reload to see the current state.', 'error');
  }
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

// ── Users Listener ──────────────────────────────────────────────────────────
// ── Users Listener & Helpers ────────────────────────────────────────────────
let usersList = [];
let bansMap = new Map(); // uid → ban doc

const GENERIC_NAMES = new Set([
  'learner', 'guest', 'anonymous user', 'registered user',
  'kasiguranin learner', 'kasiguru learner',
  'google account', 'google'
]);

function isGenericName(name) {
  if (!name || typeof name !== 'string') return true;
  const clean = name.trim().toLowerCase();
  return !clean || GENERIC_NAMES.has(clean);
}

function resolveUserDisplayName(user, progressData = null) {
  const p = progressData || {};
  const u = user || {};

  // Try real non-generic names first in priority order:
  // 1. progressData.fullName
  // 2. progressData.userName
  // 3. user.fullName
  // 4. user.userName
  // 5. user.displayName
  const candidates = [
    p.fullName,
    p.userName,
    u.fullName,
    u.userName,
    u.displayName
  ];

  for (const c of candidates) {
    if (c && typeof c === 'string') {
      const trimmed = c.trim();
      if (trimmed && !isGenericName(trimmed)) {
        return trimmed;
      }
    }
  }

  // If all explicit names are generic or missing, derive from email prefix
  const resolvedEmail = (p.email || u.email || '').trim();
  if (resolvedEmail && resolvedEmail.includes('@')) {
    const emailPrefix = resolvedEmail.split('@')[0].trim();
    if (emailPrefix && !isGenericName(emailPrefix)) {
      return emailPrefix;
    }
  }

  // Fallback to any non-empty name or 'Registered User'
  for (const c of candidates) {
    if (c && typeof c === 'string' && c.trim()) {
      return c.trim();
    }
  }

  return 'Registered User';
}

function initUsersListener() {
  const usersQuery = query(collection(db, "leaderboard_public"), orderBy("totalXp", "desc"));
  const unsubUsers = onSnapshot(usersQuery, (snapshot) => {
    const prevMap = new Map(usersList.map(u => [u.id, u]));
    usersList = snapshot.docs.map(doc => {
      const prev = prevMap.get(doc.id) || {};
      const raw = { id: doc.id, ...doc.data() };
      const merged = { ...prev, ...raw };
      if (prev.fullName && !raw.fullName) merged.fullName = prev.fullName;
      if (prev.userName && !raw.userName) merged.userName = prev.userName;
      if (prev.progressLoaded) merged.progressLoaded = true;
      if (!isGenericName(prev.displayName) && isGenericName(raw.displayName)) {
        merged.displayName = prev.displayName;
      }
      return merged;
    });
    renderUsersTable();
    enrichUsersWithProgress();
  }, (error) => {
    console.error("Users listener error:", error);
    const tbody = document.getElementById('users-tbody');
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:2.5rem; color:var(--status-rejected);">Failed to load users. Check permissions or indexes.</td></tr>`;
    }
  });
  unsubscribeFns.push(unsubUsers);

  // Search + filter controls
  const searchInput = document.getElementById('search-users-input');
  const filterSelect = document.getElementById('filter-users-status');
  if (searchInput) searchInput.addEventListener('input', renderUsersTable);
  if (filterSelect) filterSelect.addEventListener('change', renderUsersTable);

  const usersTableBody = document.getElementById('users-tbody');
  if (usersTableBody) {
    usersTableBody.addEventListener('click', (e) => {
      const reviewAppealBtn = e.target.closest('.btn-review-appeal');
      if (reviewAppealBtn) {
        const uid = reviewAppealBtn.getAttribute('data-uid');
        const name = reviewAppealBtn.getAttribute('data-name');
        window.openAppealReview(uid, name);
        return;
      }
      const blockBtn = e.target.closest('.btn-block-user');
      if (blockBtn) {
        const uid = blockBtn.getAttribute('data-uid');
        const name = blockBtn.getAttribute('data-name');
        window.blockUser(uid, name);
        return;
      }
      const unblockBtn = e.target.closest('.btn-unblock-user');
      if (unblockBtn) {
        const uid = unblockBtn.getAttribute('data-uid');
        const name = unblockBtn.getAttribute('data-name');
        window.unblockUser(uid, name);
        return;
      }
      const viewBtn = e.target.closest('.btn-view-user');
      if (viewBtn) {
        const uid = viewBtn.getAttribute('data-uid');
        window.openUserDetails(uid);
        return;
      }
      const row = e.target.closest('.user-row-clickable');
      if (row && !e.target.closest('button') && !e.target.closest('a')) {
        const uid = row.getAttribute('data-uid');
        if (uid) window.openUserDetails(uid);
      }
    });
  }
}

async function enrichUsersWithProgress() {
  let hasUpdates = false;
  const enriched = await Promise.all(usersList.map(async (u) => {
    const currentResolved = resolveUserDisplayName(u);
    if (u.progressLoaded && !isGenericName(currentResolved)) {
      return u;
    }
    try {
      const pDoc = await getDoc(doc(db, "users", u.id, "progress", "main"));
      if (pDoc.exists()) {
        const pData = pDoc.data() || {};
        const pEmail = (pData.email || '').trim();
        const pDate = pData.registeredAt || pData.createdAt || pData.updatedAt || 0;
        const pFullName = (pData.fullName || '').trim();
        const pUserName = (pData.userName || '').trim();
        
        let changed = false;
        const updatedUser = { ...u, progressLoaded: true };
        
        if (!updatedUser.email && pEmail) {
          updatedUser.email = pEmail;
          changed = true;
        }
        if (!updatedUser.registeredAt && !updatedUser.createdAt && pDate) {
          updatedUser.registeredAt = pDate;
          changed = true;
        }
        if (pFullName && updatedUser.fullName !== pFullName) {
          updatedUser.fullName = pFullName;
          changed = true;
        }
        if (pUserName && updatedUser.userName !== pUserName) {
          updatedUser.userName = pUserName;
          changed = true;
        }

        const newResolved = resolveUserDisplayName(updatedUser, pData);
        if (newResolved && (isGenericName(updatedUser.displayName) || updatedUser.displayName !== newResolved)) {
          updatedUser.displayName = newResolved;
          changed = true;
        }

        if (changed) {
          hasUpdates = true;
          return updatedUser;
        }
        return updatedUser;
      } else {
        return { ...u, progressLoaded: true };
      }
    } catch (e) {
      // Ignore if user progress doc is not accessible
      return u;
    }
  }));

  if (hasUpdates) {
    usersList = enriched;
    renderUsersTable();
  }
}

function renderUsersTable() {
  const tbody = document.getElementById('users-tbody');
  const countEl = document.getElementById('users-result-count');
  if (!tbody) return;

  // ── Step 1: Filter anonymous / generic accounts ───────────────────
  const validUsers = usersList.filter(user => {
    if (user.isAnonymous === true) return false;
    const name = resolveUserDisplayName(user).toLowerCase();
    const hasRealEmail = Boolean(user.email && user.email.includes('@'));
    const hasRealName  = !isGenericName(name);
    return hasRealEmail || hasRealName;
  });

  // ── Step 2: Deduplicate by email / display name ───────────────────
  const uniqueUserMap = new Map();
  for (const user of validUsers) {
    const name  = resolveUserDisplayName(user).toLowerCase();
    const email = (user.email || '').trim().toLowerCase();
    const key = email || (!isGenericName(name) ? name : '') || user.id;
    if (!uniqueUserMap.has(key)) {
      uniqueUserMap.set(key, { ...user });
    } else {
      const existing = uniqueUserMap.get(key);
      const merged = { ...existing };
      if (!existing.email && user.email) merged.email = user.email;
      if ((user.totalXp || 0) > (existing.totalXp || 0)) merged.totalXp = user.totalXp;
      if (!existing.registeredAt && user.registeredAt) merged.registeredAt = user.registeredAt;
      if (!existing.fullName && user.fullName) merged.fullName = user.fullName;
      if (!existing.userName && user.userName) merged.userName = user.userName;
      uniqueUserMap.set(key, merged);
    }
  }
  const byNameIndex = new Map();
  for (const [key, user] of uniqueUserMap) {
    const name = resolveUserDisplayName(user).toLowerCase();
    if (!name || isGenericName(name)) continue;
    if (!byNameIndex.has(name)) {
      byNameIndex.set(name, key);
    } else {
      const otherKey = byNameIndex.get(name);
      const other = uniqueUserMap.get(otherKey);
      if (!other) continue;
      const current = user;
      const currentHasEmail = current.email && current.email.includes('@');
      const otherHasEmail   = other.email   && other.email.includes('@');
      if (currentHasEmail && otherHasEmail && current.email.toLowerCase() !== other.email.toLowerCase()) continue;
      let keepKey, dropKey;
      if (currentHasEmail && !otherHasEmail) { keepKey = key;      dropKey = otherKey; }
      else if (otherHasEmail && !currentHasEmail) { keepKey = otherKey; dropKey = key; }
      else { keepKey = (current.totalXp||0) >= (other.totalXp||0) ? key : otherKey;
             dropKey = keepKey === key ? otherKey : key; }
      const winner = uniqueUserMap.get(keepKey);
      const loser  = uniqueUserMap.get(dropKey);
      const merged = { ...winner };
      if (!winner.email && loser.email) merged.email = loser.email;
      merged.totalXp = Math.max(winner.totalXp||0, loser.totalXp||0);
      if (!winner.registeredAt && loser.registeredAt) merged.registeredAt = loser.registeredAt;
      if (!winner.fullName && loser.fullName) merged.fullName = loser.fullName;
      if (!winner.userName && loser.userName) merged.userName = loser.userName;
      uniqueUserMap.set(keepKey, merged);
      uniqueUserMap.delete(dropKey);
      byNameIndex.set(name, keepKey);
    }
  }

  let registeredUsers = Array.from(uniqueUserMap.values());

  // ── Step 3: Search + status filter ───────────────────────────────
  const searchQ = (document.getElementById('search-users-input')?.value || '').trim().toLowerCase();
  const statusF = document.getElementById('filter-users-status')?.value || '';

  if (searchQ) {
    registeredUsers = registeredUsers.filter(u => {
      const displayName = resolveUserDisplayName(u).toLowerCase();
      const rawName  = (u.displayName || u.fullName || u.userName || '').toLowerCase();
      const email = (u.email || '').toLowerCase();
      return displayName.includes(searchQ) || rawName.includes(searchQ) || email.includes(searchQ);
    });
  }
  if (statusF === 'banned') {
    registeredUsers = registeredUsers.filter(u => bansMap.has(u.id));
  } else if (statusF === 'active') {
    registeredUsers = registeredUsers.filter(u => !bansMap.has(u.id));
  } else if (statusF === 'appeals') {
    registeredUsers = registeredUsers.filter(u => {
      const ban = bansMap.get(u.id);
      return ban && ban.appealStatus === 'pending';
    });
  }

  if (countEl) {
    countEl.textContent = `${registeredUsers.length} user account${registeredUsers.length === 1 ? '' : 's'}`;
  }

  if (registeredUsers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:2.5rem; color:var(--muted);">No registered user accounts found yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = registeredUsers.map(user => {
    const xp     = user.totalXp || 0;
    const streak = user.currentStreak || 0;
    const isBanned = bansMap.has(user.id);
    const banDoc   = bansMap.get(user.id) || {};
    const hasPendingAppeal = isBanned && banDoc.appealStatus === 'pending';
    const hasRejectedAppeal = isBanned && banDoc.appealStatus === 'rejected';

    // Resolve email
    const resolvedEmail = (user.email && user.email.includes('@'))
      ? user.email.trim()
      : (user.displayName && user.displayName.includes('@'))
        ? user.displayName.trim()
        : '';

    // Resolve display name & username
    const displayName = resolveUserDisplayName(user);
    const initial = (displayName[0] || 'U').toUpperCase();

    // Show secondary username tag if different from display name (e.g. Full Name shown with @username)
    const rawUserName = (user.userName || '').trim();
    const userNameHint = (rawUserName && rawUserName !== displayName && !isGenericName(rawUserName))
      ? `<span style="color:var(--muted); font-size:0.75rem; font-weight:normal;">(@${escapeHtml(rawUserName)})</span>`
      : '';

    let appealBadge = '';
    if (hasPendingAppeal) {
      appealBadge = `<span class="badge" style="background:#fff3cd; color:#856404; border:1px solid #ffeeba; font-weight:700; font-size:0.75rem; padding:2px 7px; border-radius:999px; margin-left:6px; display:inline-flex; align-items:center; gap:4px; vertical-align:middle;" title="User has an appeal waiting for review"><iconsax-icon name="notification" type="bulk" size="12" color="#b45309"></iconsax-icon> Appeal Pending</span>`;
    }

    const userLabel = `
      <div style="display:flex; align-items:center; gap:10px;">
        <div class="user-avatar-sm">${escapeHtml(initial)}</div>
        <div>
          <div style="font-weight:700; display:flex; align-items:center; flex-wrap:wrap; gap:4px;">
            <span>${escapeHtml(displayName)}</span>
            ${userNameHint}
            ${appealBadge}
          </div>
          <div style="color:var(--muted); font-size:0.75rem; font-family:monospace;">${escapeHtml(user.id ? user.id.slice(0, 10) + '…' : '—')}</div>
        </div>
      </div>
    `;
    const emailDisplay = resolvedEmail ? escapeHtml(resolvedEmail) : `<span style="color:var(--muted);">—</span>`;

    // Format date
    const dateValue = user.registeredAt || user.createdAt || user.joinedAt || user.updatedAt;
    const dateMs = toMillis(dateValue);
    let registeredDate = '—';
    if (dateMs > 0) {
      registeredDate = new Date(dateMs).toLocaleDateString(undefined, { month:'short', day:'numeric', year:'numeric' });
    } else if (user.lastActiveDate) {
      const parsed = Date.parse(user.lastActiveDate);
      if (!Number.isNaN(parsed)) registeredDate = new Date(parsed).toLocaleDateString(undefined, { month:'short', day:'numeric', year:'numeric' });
    }

    const badge = escapeHtml(user.titleBadge || 'Kasiguranin Apprentice');

    // Status cell
    let statusCell = '';
    if (isBanned) {
      if (hasPendingAppeal) {
        statusCell = `<span class="badge" style="background:#fff3cd; color:#856404; border:1px solid #ffeeba; font-weight:700; display:inline-flex; align-items:center; gap:4px;" title="Appeal pending review"><iconsax-icon name="notification" type="bulk" size="12" color="#b45309"></iconsax-icon> Appeal Pending</span>`;
      } else if (hasRejectedAppeal) {
        statusCell = `<span class="badge badge-rejected" title="Appeal rejected: ${escapeHtml(banDoc.appealReviewNotes || '')}">Blocked (Appeal Declined)</span>`;
      } else {
        statusCell = `<span class="badge badge-rejected" title="${escapeHtml(banDoc.reason || '')}">Blocked</span>`;
      }
    } else {
      statusCell = `<span class="badge badge-approved">Active</span>`;
    }

    // Actions cell — only show for real accounts that have a uid
    let actionCell = '—';
    if (user.id) {
      let buttons = '';
      if (isBanned) {
        buttons += `<button type="button" class="btn btn-sm btn-outline btn-unblock-user" data-uid="${escapeHtml(user.id)}" data-name="${escapeHtml(displayName)}">Unblock</button>`;
      } else {
        buttons += `<button type="button" class="btn btn-sm btn-danger btn-block-user" data-uid="${escapeHtml(user.id)}" data-name="${escapeHtml(displayName)}">Block</button>`;
      }
      actionCell = buttons || '—';
    }

    return `
      <tr class="user-row-clickable" data-uid="${escapeHtml(user.id)}" title="Click to view details">
        <td>${userLabel}</td>
        <td style="color:var(--text); font-size:0.875rem;">${emailDisplay}</td>
        <td style="color:var(--muted); font-size:0.875rem; white-space:nowrap;">${escapeHtml(registeredDate)}</td>
        <td><span class="badge badge-outline" style="border:1px solid var(--border); color:var(--text); background:transparent;">${badge}</span></td>
        <td class="num">${xp.toLocaleString()} XP</td>
        <td class="num" style="color:var(--primary); font-weight:700;"><iconsax-icon name="fire" type="bulk" size="14" color="currentColor" style="vertical-align:text-bottom;"></iconsax-icon> ${streak}</td>
        <td>${statusCell}</td>
        <td>${actionCell}</td>
      </tr>
    `;
  }).join('');

  updateAppealsBadge();
}

// ── Bans Listener ────────────────────────────────────────────────────────────
function initBansListener() {
  try {
    const unsubBans = onSnapshot(collection(db, 'user_bans'), (snapshot) => {
      bansMap.clear();
      snapshot.docs.forEach(d => {
        if (d.data().isBanned) bansMap.set(d.id, { id: d.id, ...d.data() });
      });
      renderUsersTable();
      updateAppealsBadge();
    }, (err) => {
      console.warn('Bans listener error:', err);
    });
    unsubscribeFns.push(unsubBans);
  } catch (e) {
    console.error('Bans init error:', e);
  }
}

// ── Appeals Notification & Filter Helper ────────────────────────────────────
function updateAppealsBadge() {
  let pendingAppealsCount = 0;
  bansMap.forEach((ban) => {
    if (ban && ban.isBanned && ban.appealStatus === 'pending') {
      pendingAppealsCount++;
    }
  });

  // Sidebar badge next to "Users"
  const navUsersCount = document.getElementById('nav-users-count');
  if (navUsersCount) {
    navUsersCount.textContent = pendingAppealsCount;
    navUsersCount.hidden = pendingAppealsCount === 0;
  }

  // Users tab banner alert
  const appealsAlert = document.getElementById('users-appeals-alert');
  const appealsAlertCount = document.getElementById('users-appeals-alert-count');
  if (appealsAlert) {
    if (pendingAppealsCount > 0) {
      appealsAlert.style.display = 'flex';
      if (appealsAlertCount) {
        appealsAlertCount.textContent = `${pendingAppealsCount} user appeal${pendingAppealsCount === 1 ? '' : 's'}`;
      }
    } else {
      appealsAlert.style.display = 'none';
    }
  }
}

window.filterToAppeals = function() {
  const filterSelect = document.getElementById('filter-users-status');
  if (filterSelect) {
    filterSelect.value = 'appeals';
    renderUsersTable();
  }
};

// ── User Account Details Modal ──────────────────────────────────────────────
window.openUserDetails = async function(uid) {
  if (!uid) return;
  const modal = document.getElementById('user-details-modal');
  const body = document.getElementById('user-modal-body');
  const actionsBox = document.getElementById('user-modal-action-buttons');
  if (!modal || !body) return;

  // Show loading skeleton while loading user progress
  body.innerHTML = `
    <div style="text-align:center; padding:2.5rem; color:var(--muted);">
      <iconsax-icon name="user" type="bulk" size="36" color="var(--violet)"></iconsax-icon>
      <p style="margin-top:12px; font-weight:600;">Loading user details&hellip;</p>
    </div>
  `;
  if (actionsBox) actionsBox.innerHTML = '';
  openModal('user-details-modal');

  // Find cached user
  const user = usersList.find(u => u.id === uid) || { id: uid };
  let progressData = {};
  try {
    const pDoc = await getDoc(doc(db, 'users', uid, 'progress', 'main'));
    if (pDoc.exists()) {
      progressData = pDoc.data() || {};
    }
  } catch (e) {
    console.warn('Could not load user progress in details modal:', e);
  }

  // Display name resolution
  const displayName = resolveUserDisplayName(user, progressData);

  // Sync loaded progress data into cached user in usersList and update table
  const cachedIdx = usersList.findIndex(u => u.id === uid);
  if (cachedIdx !== -1) {
    let changed = false;
    const cu = usersList[cachedIdx];
    if (progressData.fullName && cu.fullName !== progressData.fullName) {
      cu.fullName = progressData.fullName;
      changed = true;
    }
    if (progressData.userName && cu.userName !== progressData.userName) {
      cu.userName = progressData.userName;
      changed = true;
    }
    if (progressData.email && !cu.email) {
      cu.email = progressData.email;
      changed = true;
    }
    if (!cu.progressLoaded) {
      cu.progressLoaded = true;
      changed = true;
    }
    if (cu.displayName !== displayName) {
      cu.displayName = displayName;
      changed = true;
    }
    if (changed) {
      renderUsersTable();
    }
  }

  const banDoc = bansMap.get(uid);
  const isBanned = Boolean(banDoc && banDoc.isBanned);
  const hasPendingAppeal = Boolean(isBanned && banDoc.appealStatus === 'pending');
  const hasRejectedAppeal = Boolean(isBanned && banDoc.appealStatus === 'rejected');

  // Email resolution
  const resolvedEmail = (progressData.email || user.email || '').trim();

  const xp = progressData.totalXp ?? user.totalXp ?? 0;
  const streak = progressData.currentStreak ?? user.currentStreak ?? 0;
  const badge = progressData.titleBadge || user.titleBadge || 'Kasiguranin Apprentice';

  const dateValue = progressData.registeredAt || user.registeredAt || progressData.createdAt || user.createdAt || user.joinedAt || user.updatedAt;
  const dateMs = toMillis(dateValue);
  let registeredDate = '—';
  if (dateMs > 0) {
    registeredDate = new Date(dateMs).toLocaleString(undefined, { dateStyle:'medium', timeStyle:'short' });
  }

  // Status tag
  let statusBadgeHtml = `<span class="badge badge-approved" style="font-size:0.8rem; padding:4px 10px;">Active Account</span>`;
  if (isBanned) {
    if (hasPendingAppeal) {
      statusBadgeHtml = `<span class="badge" style="background:#fff3cd; color:#856404; border:1px solid #ffeeba; font-weight:700; font-size:0.8rem; padding:4px 10px; display:inline-flex; align-items:center; gap:5px;"><iconsax-icon name="notification" type="bulk" size="14" color="#b45309"></iconsax-icon> Suspended (Appeal Pending)</span>`;
    } else if (hasRejectedAppeal) {
      statusBadgeHtml = `<span class="badge badge-rejected" style="font-size:0.8rem; padding:4px 10px;">Suspended (Appeal Declined)</span>`;
    } else {
      statusBadgeHtml = `<span class="badge badge-rejected" style="font-size:0.8rem; padding:4px 10px;">Suspended</span>`;
    }
  }

  // Suspension details card (if banned)
  let suspensionSectionHtml = '';
  if (isBanned) {
    const bannedDateStr = banDoc.bannedAt ? new Date(banDoc.bannedAt).toLocaleString() : 'Unknown';
    suspensionSectionHtml = `
      <div style="background:var(--sunken); border-left:4px solid var(--status-rejected); border-radius:var(--r-ctl); padding:14px 16px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
          <span style="font-weight:700; color:var(--status-rejected); font-size:0.875rem;">Account Suspension Reason</span>
          <small style="color:var(--muted); font-size:0.75rem;">Suspended on ${escapeHtml(bannedDateStr)} by ${escapeHtml(banDoc.bannedBy || 'Admin')}</small>
        </div>
        <p style="margin:0; font-size:0.875rem; color:var(--ink); line-height:1.5;">${escapeHtml(banDoc.reason || 'No reason specified.')}</p>
      </div>
    `;
  }

  // Appeal Section
  let appealSectionHtml = '';
  if (hasPendingAppeal) {
    const appealDateStr = banDoc.appealSubmittedAt ? new Date(banDoc.appealSubmittedAt).toLocaleString() : 'Unknown';
    appealSectionHtml = `
      <div class="appeal-banner-pending">
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px;">
          <div style="display:flex; align-items:center; gap:8px; color:#92400e; font-weight:700; font-size:0.9rem;">
            <iconsax-icon name="notification" type="bulk" size="20" color="#d97706"></iconsax-icon>
            <span>Appeal Awaiting Review</span>
          </div>
          <small style="color:#b45309; font-size:0.75rem;">Submitted ${escapeHtml(appealDateStr)}</small>
        </div>
        <div style="background:white; border:1px solid #fde68a; border-radius:var(--r-ctl); padding:12px; margin-top:8px;">
          <div style="font-size:0.75rem; font-weight:700; color:#92400e; text-transform:uppercase; margin-bottom:4px; letter-spacing:0.04em;">User Statement:</div>
          <blockquote style="margin:0; font-size:0.875rem; color:#1f2937; line-height:1.6; font-style:italic; white-space:pre-wrap;">${escapeHtml(banDoc.appealText || 'No statement provided.')}</blockquote>
        </div>
      </div>
    `;
  } else if (hasRejectedAppeal) {
    const reviewDateStr = banDoc.appealReviewedAt ? new Date(banDoc.appealReviewedAt).toLocaleString() : 'Unknown';
    appealSectionHtml = `
      <div class="appeal-banner-rejected">
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px;">
          <span style="font-weight:700; color:var(--status-rejected); font-size:0.875rem;">Appeal Declined</span>
          <small style="color:var(--muted); font-size:0.75rem;">Reviewed ${escapeHtml(reviewDateStr)} by ${escapeHtml(banDoc.appealReviewedBy || 'Admin')}</small>
        </div>
        ${banDoc.appealText ? `<div style="font-size:0.825rem; color:var(--muted); font-style:italic;">User statement: "${escapeHtml(banDoc.appealText)}"</div>` : ''}
        ${banDoc.appealReviewNotes ? `<div style="font-size:0.875rem; color:var(--ink); margin-top:4px;"><strong>Feedback given:</strong> ${escapeHtml(banDoc.appealReviewNotes)}</div>` : ''}
      </div>
    `;
  }

  const initial = (displayName[0] || 'U').toUpperCase();

  body.innerHTML = `
    <!-- Profile Header Card -->
    <div style="display:flex; align-items:center; justify-content:space-between; gap:16px; background:var(--sunken); border-radius:var(--r-ctl); padding:16px; flex-wrap:wrap;">
      <div style="display:flex; align-items:center; gap:14px;">
        <div style="width:52px; height:52px; border-radius:50%; background:var(--violet); color:white; display:flex; align-items:center; justify-content:center; font-size:1.35rem; font-weight:800; flex-shrink:0; box-shadow:0 4px 12px rgba(91,76,219,0.25);">
          ${escapeHtml(initial)}
        </div>
        <div>
          <div style="font-size:1.15rem; font-weight:800; color:var(--ink);">${escapeHtml(displayName)}</div>
          ${(progressData.userName && progressData.userName !== displayName && !isGenericName(progressData.userName)) ? `<div style="font-size:0.8rem; color:var(--primary); font-weight:600; margin-top:1px;">@${escapeHtml(progressData.userName)}</div>` : ''}
          <div style="font-size:0.875rem; color:var(--muted); margin-top:2px;">${escapeHtml(resolvedEmail || 'No email associated')}</div>
          <div style="display:flex; align-items:center; gap:6px; margin-top:6px;">
            <span style="font-size:0.75rem; font-family:monospace; background:var(--surface); border:1px solid var(--border); padding:2px 6px; border-radius:4px; color:var(--text);">UID: ${escapeHtml(uid)}</span>
            <button type="button" class="btn btn-xs btn-outline" style="font-size:0.7rem; padding:2px 6px;" onclick="navigator.clipboard.writeText('${escapeHtml(uid)}'); this.textContent='Copied!'; setTimeout(()=>this.textContent='Copy', 1500);">Copy</button>
          </div>
        </div>
      </div>
      <div>
        ${statusBadgeHtml}
      </div>
    </div>

    <!-- Stats Grid -->
    <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(130px, 1fr)); gap:12px;">
      <div class="user-details-stat-card">
        <span class="user-details-stat-label">Total XP</span>
        <span class="user-details-stat-val" style="color:var(--violet);">${xp.toLocaleString()} XP</span>
      </div>
      <div class="user-details-stat-card">
        <span class="user-details-stat-label">Current Streak</span>
        <span class="user-details-stat-val" style="color:#d97706; display:flex; align-items:center; gap:4px;">
          <iconsax-icon name="fire" type="bulk" size="18" color="#d97706"></iconsax-icon> ${streak} ${streak === 1 ? 'day' : 'days'}
        </span>
      </div>
      <div class="user-details-stat-card">
        <span class="user-details-stat-label">Level Badge</span>
        <span class="user-details-stat-val" style="font-size:0.9rem; font-weight:700;">${escapeHtml(badge)}</span>
      </div>
      <div class="user-details-stat-card">
        <span class="user-details-stat-label">Joined</span>
        <span class="user-details-stat-val" style="font-size:0.825rem; font-weight:600; color:var(--muted);">${escapeHtml(registeredDate)}</span>
      </div>
    </div>

    ${suspensionSectionHtml}
    ${appealSectionHtml}
  `;

  // Modal Actions
  if (actionsBox) {
    let actionButtonsHtml = '';
    if (hasPendingAppeal) {
      actionButtonsHtml = `
        <button type="button" class="btn btn-danger" onclick="closeModal('user-details-modal'); window.rejectAppeal('${escapeHtml(uid)}', '${escapeHtml(displayName)}');">Decline Appeal</button>
        <button type="button" class="btn btn-primary" onclick="closeModal('user-details-modal'); window.approveAppeal('${escapeHtml(uid)}', '${escapeHtml(displayName)}');">Approve Appeal &amp; Unblock</button>
      `;
    } else if (isBanned) {
      actionButtonsHtml = `
        <button type="button" class="btn btn-outline" onclick="closeModal('user-details-modal'); window.unblockUser('${escapeHtml(uid)}', '${escapeHtml(displayName)}');">Unblock User</button>
      `;
    } else {
      actionButtonsHtml = `
        <button type="button" class="btn btn-danger" onclick="closeModal('user-details-modal'); window.blockUser('${escapeHtml(uid)}', '${escapeHtml(displayName)}');">Block User</button>
      `;
    }
    actionsBox.innerHTML = actionButtonsHtml;
  }
};

// ── Block User ────────────────────────────────────────────────────────────────
window.blockUser = async function(uid, displayName) {
  if (!uid) {
    notify('Cannot block user: missing UID.', 'danger');
    return;
  }
  const nameToDisplay = displayName || 'User';

  // Step 1: Ask for a reason via a custom input dialog
  let reasonValue = '';
  const host = dialogHost();
  host.querySelector('#confirm-dialog-title').textContent = `Block ${nameToDisplay}?`;
  const bodyEl = host.querySelector('#confirm-dialog-body');
  bodyEl.innerHTML =
    `<p style="margin-bottom:10px;">This user will see an "Account Suspended" screen and cannot use KasiGuru until unblocked.</p>` +
    `<label for="ban-reason-input" style="font-weight:600; display:block; margin-bottom:6px;">Reason <span style="color:var(--status-rejected);">*</span></label>` +
    `<textarea id="ban-reason-input" class="form-control" rows="3" placeholder="e.g. Harassment, cheating, repeated abuse of the community submission system…" style="width:100%; resize:vertical;"></textarea>`;

  const okBtn = host.querySelector('[data-act="ok"]');
  okBtn.textContent = 'Block user';
  okBtn.className = 'btn btn-danger';

  const confirmed = await new Promise((resolve) => {
    function close(result) {
      reasonValue = (document.getElementById('ban-reason-input')?.value || '').trim();
      host.classList.remove('active');
      host.removeEventListener('click', onBackdrop);
      document.removeEventListener('keydown', onKey);
      okBtn.onclick = null;
      host.querySelector('[data-act="cancel"]').onclick = null;
      resolve(result);
    }
    function onBackdrop(e) { if (e.target === host) close(false); }
    function onKey(e) { if (e.key === 'Escape') close(false); }
    okBtn.onclick = () => {
      const reason = (document.getElementById('ban-reason-input')?.value || '').trim();
      if (!reason) {
        document.getElementById('ban-reason-input')?.classList.add('input-error');
        document.getElementById('ban-reason-input')?.focus();
        return;
      }
      close(true);
    };
    host.querySelector('[data-act="cancel"]').onclick = () => close(false);
    host.addEventListener('click', onBackdrop);
    document.addEventListener('keydown', onKey);
    host.classList.add('active');
    setTimeout(() => document.getElementById('ban-reason-input')?.focus(), 80);
  });

  if (!confirmed || !reasonValue) return;

  try {
    const actor = (auth.currentUser && auth.currentUser.email) || 'admin';
    await setDoc(doc(db, 'user_bans', uid), {
      isBanned: true,
      reason: reasonValue,
      bannedAt: Date.now(),
      bannedBy: actor
    });
    // Log with both format and details for audit trail
    await logAudit('user.block', { uid, displayName: nameToDisplay, reason: reasonValue });
    notify(`${nameToDisplay} has been blocked.`, 'success');
  } catch (e) {
    console.error('Block failed:', e);
    let errMsg = e.message || String(e);
    if (e.code === 'permission-denied') {
      errMsg = 'Permission denied. Make sure firestore.rules has been published to Firebase with user_bans permissions.';
    }
    notify('Failed to block user: ' + errMsg, 'danger');
  }
};

// ── Unblock User ──────────────────────────────────────────────────────────────
window.unblockUser = async function(uid, displayName) {
  if (!uid) {
    notify('Cannot unblock user: missing UID.', 'danger');
    return;
  }
  const nameToDisplay = displayName || 'User';

  const confirmed = await confirmDialog({
    title: `Unblock ${nameToDisplay}?`,
    body: `<p>This user will regain full access to KasiGuru immediately.</p>`,
    confirmLabel: 'Unblock',
    danger: false
  });
  if (!confirmed) return;

  try {
    await deleteDoc(doc(db, 'user_bans', uid));
    await logAudit('user.unblock', { uid, displayName: nameToDisplay });
    notify(`${nameToDisplay} has been unblocked.`, 'success');
  } catch (e) {
    console.error('Unblock failed:', e);
    let errMsg = e.message || String(e);
    if (e.code === 'permission-denied') {
      errMsg = 'Permission denied. Make sure firestore.rules has been published to Firebase with user_bans permissions.';
    }
    notify('Failed to unblock user: ' + errMsg, 'danger');
  }
};

// ── Review Appeal ─────────────────────────────────────────────────────────────
window.openAppealReview = function(uid, displayName) {
  const ban = bansMap.get(uid);
  if (!ban) {
    notify('Ban record not found for this user.', 'danger');
    return;
  }
  const nameToDisplay = displayName || 'User';
  const modal = document.getElementById('appeal-review-modal');
  const body = document.getElementById('appeal-modal-body');
  const approveBtn = document.getElementById('appeal-modal-approve-btn');
  const rejectBtn = document.getElementById('appeal-modal-reject-btn');
  if (!modal || !body) return;

  const banDateStr = ban.bannedAt ? new Date(ban.bannedAt).toLocaleString() : 'Unknown';
  const appealDateStr = ban.appealSubmittedAt ? new Date(ban.appealSubmittedAt).toLocaleString() : 'Unknown';

  body.innerHTML = `
    <div style="background:var(--bg-subtle, #f8f9fa); border:1px solid var(--border, #e9ecef); border-radius:8px; padding:12px 16px;">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
        <strong style="font-size:1rem;">${escapeHtml(nameToDisplay)}</strong>
        <span style="font-size:0.75rem; color:var(--muted); font-family:monospace;">${escapeHtml(uid)}</span>
      </div>
      <div style="font-size:0.85rem; color:var(--muted); margin-bottom:4px;">
        <strong>Suspension Reason:</strong> ${escapeHtml(ban.reason || 'None specified')}
      </div>
      <div style="font-size:0.8rem; color:var(--muted);">
        Suspended on: ${escapeHtml(banDateStr)} ${ban.bannedBy ? `by ${escapeHtml(ban.bannedBy)}` : ''}
      </div>
    </div>

    <div>
      <label style="font-weight:700; font-size:0.875rem; display:flex; align-items:center; gap:6px; margin-bottom:6px;">
        <iconsax-icon name="document-text" type="bulk" size="16" color="var(--violet)"></iconsax-icon>
        User Appeal Statement
      </label>
      <div style="background:#fff; border:1px solid var(--border, #ced4da); border-radius:8px; padding:14px; font-size:0.925rem; line-height:1.5; color:var(--text); white-space:pre-wrap; max-height:200px; overflow-y:auto;">
        ${escapeHtml(ban.appealText || 'No statement provided.')}
      </div>
      <small style="color:var(--muted); display:block; margin-top:4px;">Submitted: ${escapeHtml(appealDateStr)}</small>
    </div>
  `;

  approveBtn.onclick = async () => {
    closeModal('appeal-review-modal');
    await window.approveAppeal(uid, nameToDisplay);
  };

  rejectBtn.onclick = async () => {
    closeModal('appeal-review-modal');
    await window.rejectAppeal(uid, nameToDisplay);
  };

  openModal('appeal-review-modal');
};

// ── Approve Appeal & Unblock ──────────────────────────────────────────────────
window.approveAppeal = async function(uid, displayName) {
  const nameToDisplay = displayName || 'User';
  const confirmed = await confirmDialog({
    title: `Approve Appeal & Unblock ${nameToDisplay}?`,
    body: `<p>This will approve the appeal and restore the user's account immediately. The user will be automatically redirected to KasiGuru without having to sign out.</p>`,
    confirmLabel: 'Approve & Unblock',
    danger: false
  });
  if (!confirmed) return;

  try {
    await deleteDoc(doc(db, 'user_bans', uid));
    await logAudit('appeal.approve', { uid, displayName: nameToDisplay, note: 'Appeal approved; user unblocked' });
    notify(`Appeal approved! ${nameToDisplay} has been unblocked.`, 'success');
  } catch (e) {
    console.error('Approve appeal failed:', e);
    notify('Failed to approve appeal: ' + (e.message || String(e)), 'danger');
  }
};

// ── Reject Appeal ─────────────────────────────────────────────────────────────
window.rejectAppeal = async function(uid, displayName) {
  const nameToDisplay = displayName || 'User';

  let feedbackValue = '';
  const host = dialogHost();
  host.querySelector('#confirm-dialog-title').textContent = `Decline Appeal from ${nameToDisplay}?`;
  const bodyEl = host.querySelector('#confirm-dialog-body');
  bodyEl.innerHTML =
    `<p style="margin-bottom:10px;">The account will remain suspended. The user will see your feedback message on their Account Suspended screen.</p>` +
    `<label for="reject-feedback-input" style="font-weight:600; display:block; margin-bottom:6px;">Moderator Feedback <span style="color:var(--status-rejected);">*</span></label>` +
    `<textarea id="reject-feedback-input" class="form-control" rows="3" placeholder="e.g. Your appeal was reviewed, but the suspension stands due to repeated violations." style="width:100%; resize:vertical;"></textarea>`;

  const okBtn = host.querySelector('[data-act="ok"]');
  okBtn.textContent = 'Decline appeal';
  okBtn.className = 'btn btn-danger';

  const confirmed = await new Promise((resolve) => {
    function close(result) {
      feedbackValue = (document.getElementById('reject-feedback-input')?.value || '').trim();
      host.classList.remove('active');
      host.removeEventListener('click', onBackdrop);
      document.removeEventListener('keydown', onKey);
      okBtn.onclick = null;
      host.querySelector('[data-act="cancel"]').onclick = null;
      resolve(result);
    }
    function onBackdrop(e) { if (e.target === host) close(false); }
    function onKey(e) { if (e.key === 'Escape') close(false); }
    okBtn.onclick = () => {
      const fb = (document.getElementById('reject-feedback-input')?.value || '').trim();
      if (!fb) {
        document.getElementById('reject-feedback-input')?.classList.add('input-error');
        document.getElementById('reject-feedback-input')?.focus();
        return;
      }
      close(true);
    };
    host.querySelector('[data-act="cancel"]').onclick = () => close(false);
    host.addEventListener('click', onBackdrop);
    document.addEventListener('keydown', onKey);
    host.classList.add('active');
    setTimeout(() => document.getElementById('reject-feedback-input')?.focus(), 80);
  });

  if (!confirmed || !feedbackValue) return;

  try {
    const actor = (auth.currentUser && auth.currentUser.email) || 'admin';
    await updateDoc(doc(db, 'user_bans', uid), {
      appealStatus: 'rejected',
      appealReviewNotes: feedbackValue,
      appealReviewedAt: Date.now(),
      appealReviewedBy: actor
    });
    await logAudit('appeal.reject', { uid, displayName: nameToDisplay, reviewNotes: feedbackValue });
    notify(`Appeal declined for ${nameToDisplay}.`, 'info');
  } catch (e) {
    console.error('Reject appeal failed:', e);
    notify('Failed to decline appeal: ' + (e.message || String(e)), 'danger');
  }
};

// ── Backup & Restore ────────────────────────────────────────────────────────
window.exportBackup = async function() {
  const btn = document.getElementById('btn-export-backup');
  if (btn) btn.disabled = true;
  notify("Preparing database backup...", "info");

  try {
    // Collections this page can completely enumerate. Deliberately does NOT include learner data:
    // the browser SDK has no listDocuments(), so it cannot see a `users/{uid}` document that owns a
    // progress subcollection but has no fields of its own - and that is every user. A backup that
    // silently omitted learner progress is the exact failure this scope note exists to prevent.
    // Full-fidelity backup is functions/backup_firestore.js, which runs on the Admin SDK.
    const SCOPE = [
      "vocabulary",
      "stories",
      "story_page_images",
      // Was "system_announcements" - a name no collection has ever had. AnnouncementRepository.kt
      // and firestore.rules both say `announcements`, so every export before this quietly wrote an
      // empty array, and a restore would have been denied by the rules.
      "announcements",
      "app_releases",
      "word_submissions",
      "literature_submissions",
      "issue_reports",
      "admin_audit_log"
    ];

    const collections = {};
    for (const name of SCOPE) {
      const snap = await getDocs(collection(db, name));
      collections[name] = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    }

    const backupData = {
      version: 2,
      scope: "content-and-moderation",
      note: "Learner progress (users/*/progress) is not included; use functions/backup_firestore.js for a full backup.",
      exportedAt: new Date().toISOString(),
      timestamp: Date.now(),
      collections
    };

    const jsonStr = JSON.stringify(backupData, null, 2);
    const blob = new Blob([jsonStr], { type: "application/json" });
    const url = URL.createObjectURL(blob);

    const dateStr = new Date().toISOString().split('T')[0];
    const a = document.createElement('a');
    a.href = url;
    a.download = `kasiguru-content-backup-${dateStr}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    const summary = Object.entries(backupData.collections)
      .map(([k, v]) => `${v.length} ${k}`)
      .join(', ');
    notify(`Backup downloaded: ${summary}.`, "success");
    logAudit("backup_export", {
      scope: backupData.scope,
      counts: Object.fromEntries(Object.entries(backupData.collections).map(([k, v]) => [k, v.length]))
    });
  } catch (e) {
    console.error("Backup export failed:", e);
    notify("Backup export failed: " + e.message, "danger");
  } finally {
    if (btn) btn.disabled = false;
  }
};

/**
 * Clears the moderation queues: pending word and literature submissions, and issue reports.
 *
 * Scoped to exactly the collections `firestore.rules` lets an admin delete. Learner progress,
 * leaderboard rows, device tokens and security questions are owner-writable only by design, so a
 * full database reset is deliberately not a button here - it runs from functions/reset_firestore.js
 * with the service-account key. Weakening the rules to make this button do more would give every
 * admin session the power to rewrite any learner's data.
 */
window.resetModerationQueues = async function () {
  const btn = document.getElementById('btn-reset-queues');
  const QUEUES = ['word_submissions', 'literature_submissions', 'issue_reports'];

  try {
    if (btn) btn.disabled = true;

    const snaps = {};
    let total = 0;
    for (const name of QUEUES) {
      const snap = await getDocs(collection(db, name));
      snaps[name] = snap.docs;
      total += snap.docs.length;
    }

    if (total === 0) {
      notify('Moderation queues are already empty.', 'info');
      return;
    }

    const breakdown = QUEUES.map((n) => `${snaps[n].length} ${n.replace(/_/g, ' ')}`).join(', ');
    const confirmed = await confirmDialog({
      title: 'Clear moderation queues?',
      body:
        `<p>This permanently deletes <strong>${escapeHtml(breakdown)}</strong>.</p>` +
        `<p style="color:var(--status-rejected); margin-top:8px;">Approved words already merged into the dictionary are not affected. ` +
        `Pending and rejected items are gone for good. Export a backup first if you have not.</p>`,
      confirmLabel: `Delete ${total} items`,
      danger: true
    });
    if (!confirmed) return;

    notify('Clearing moderation queues...', 'info');

    let removed = 0;
    for (const name of QUEUES) {
      let batch = writeBatch(db);
      let n = 0;
      for (const d of snaps[name]) {
        batch.delete(doc(db, name, d.id));
        n++;
        removed++;
        if (n % 400 === 0) {
          await batch.commit();
          batch = writeBatch(db);
        }
      }
      if (n % 400 !== 0) await batch.commit();
    }

    await logAudit('reset_moderation_queues', {
      counts: Object.fromEntries(QUEUES.map((n) => [n, snaps[n].length]))
    });
    notify(`Cleared ${removed} items from the moderation queues.`, 'success');
  } catch (e) {
    console.error('Queue reset failed:', e);
    notify('Reset failed: ' + e.message, 'danger');
  } finally {
    if (btn) btn.disabled = false;
  }
};

function initBackupRestore() {
  const dropzone = document.getElementById('backup-dropzone');
  const fileInput = document.getElementById('backup-file-input');
  const statusEl = document.getElementById('backup-restore-status');

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener('click', () => fileInput.click());

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });

  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));

  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleBackupFile(files[0]);
    }
  });

  fileInput.addEventListener('change', () => {
    if (fileInput.files && fileInput.files.length > 0) {
      handleBackupFile(fileInput.files[0]);
      fileInput.value = '';
    }
  });

  async function handleBackupFile(file) {
    if (!file.name.endsWith('.json')) {
      notify("Please select a valid .json backup file.", "danger");
      return;
    }

    try {
      const text = await file.text();
      const backup = JSON.parse(text);

      // Support both full database backup files and audit-log-specific export files
      if (backup.exportType === "kasiguru_admin_audit_logs" && Array.isArray(backup.logs)) {
        backup.collections = {
          admin_audit_log: backup.logs
        };
      }

      if (!backup.collections) {
        notify("Invalid backup file structure.", "danger");
        return;
      }

      const counts = Object.entries(backup.collections)
        .map(([k, v]) => `${Array.isArray(v) ? v.length : 0} ${k}`)
        .join(', ');

      const confirmed = await confirmDialog({
        title: 'Restore Database Backup?',
        body: `<p>Found: <strong>${escapeHtml(counts)}</strong>.</p><p style="color:var(--status-rejected); margin-top:8px;">Warning: This will write/overwrite documents in Firestore.</p>`,
        confirmLabel: 'Restore Backup',
        danger: true
      });

      if (!confirmed) return;

      if (statusEl) statusEl.textContent = 'Restoring database records...';
      notify("Restoring backup...", "info");

      let totalRestored = 0;

      for (const [collName, docs] of Object.entries(backup.collections)) {
        if (!Array.isArray(docs)) continue;

        let batch = writeBatch(db);
        let count = 0;

        // Firestore security rules enforce append-only for admin_audit_log (update/delete denied).
        // Only insert audit logs that do not already exist in Firestore to prevent update rejections.
        if (collName === 'admin_audit_log') {
          const existingIds = new Set(auditLogs.map(l => l.id));
          const newDocs = docs.filter(d => d && d.id && !existingIds.has(d.id));

          for (const docData of newDocs) {
            const dataToSave = { ...docData };
            delete dataToSave.id;

            const docRef = doc(db, collName, String(docData.id));
            batch.set(docRef, dataToSave);
            count++;
            totalRestored++;

            if (count >= 450) {
              await batch.commit();
              batch = writeBatch(db);
              count = 0;
            }
          }
          if (count > 0) {
            await batch.commit();
          }
          continue;
        }

        for (const docData of docs) {
          const docId = docData.id;
          if (!docId) continue;

          const dataToSave = { ...docData };
          delete dataToSave.id;

          const docRef = doc(db, collName, String(docId));
          batch.set(docRef, dataToSave, { merge: true });
          count++;
          totalRestored++;

          if (count >= 450) {
            await batch.commit();
            batch = writeBatch(db);
            count = 0;
          }
        }

        if (count > 0) {
          await batch.commit();
        }
      }

      if (statusEl) statusEl.textContent = `Restore completed! Restored ${totalRestored} documents.`;
      notify(`Successfully restored ${totalRestored} documents from backup!`, "success");
      logAudit("backup_restore", { totalRestored });

    } catch (e) {
      console.error("Restore failed:", e);
      if (statusEl) statusEl.textContent = 'Restore failed: ' + e.message;
      notify("Restore failed: " + e.message, "danger");
    }
  }
}


