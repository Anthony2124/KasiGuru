// app.js — KasiGuru Admin Dashboard Logic (Auth-Guarded)
// This file is loaded ONLY by dashboard.html, after Firebase Auth confirms the user is logged in.

import { 
  db, auth,
  collection, doc, setDoc, addDoc, updateDoc, deleteDoc, query, orderBy, onSnapshot 
} from './firebase-config.js';
import { 
  onAuthStateChanged, signOut 
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// Global state
let submissions = [];
let vocabulary = [];
let releases = [];
let searchDebounceTimer = null;
let unsubscribeFns = [];

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
window.switchTab = function(targetTab) {
  const navBtns = document.querySelectorAll('nav button[data-tab]');
  const allTabs = document.querySelectorAll('.tab-content');

  navBtns.forEach(b => {
    if (b.getAttribute('data-tab') === targetTab) {
      b.classList.add('active');
    } else {
      b.classList.remove('active');
    }
  });

  allTabs.forEach(tc => {
    if (tc.id === targetTab) {
      tc.classList.add('active');
      tc.style.setProperty('display', 'block', 'important');
    } else {
      tc.classList.remove('active');
      tc.style.setProperty('display', 'none', 'important');
    }
  });
};

// ── Init ────────────────────────────────────────────────────────────────────
function init() {
  initNavigation();
  initRealtimeListeners();
  initExcelImporter();
  initSqlImporter();
  initFormListeners();

  const defaultBtn = document.querySelector('nav button[data-tab].active') || document.querySelector('nav button[data-tab]');
  if (defaultBtn) {
    const defaultTab = defaultBtn.getAttribute('data-tab');
    window.switchTab(defaultTab);
  }
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

  // 2. Vocabulary Listener
  try {
    const vocabQuery = query(collection(db, "vocabulary"));
    const unsubVocab = onSnapshot(vocabQuery, (snapshot) => {
      vocabulary = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
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
function updateDashboardMetrics() {
  const metricWords = document.getElementById('metric-total-words');
  if (metricWords) metricWords.textContent = vocabulary.length;

  const metricPending = document.getElementById('metric-pending-sub');
  if (metricPending) metricPending.textContent = submissions.filter(s => s.status === 'pending').length;

  const metricReleases = document.getElementById('metric-total-releases');
  if (metricReleases) metricReleases.textContent = releases.length;

  const metricVersion = document.getElementById('metric-latest-version');
  const latestRelease = releases[0];
  if (metricVersion) metricVersion.textContent = latestRelease ? `v${latestRelease.versionName}` : 'v1.0.0';
}

// ── Render Submissions Table ────────────────────────────────────────────────
function renderSubmissionsTable() {
  const tbody = document.getElementById('submissions-tbody');
  if (!tbody) return;
  
  tbody.innerHTML = '';
  
  if (submissions.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align:center; padding:2.5rem; color:var(--text-muted);">
          <iconsax-icon name="clock" type="bulk" size="32" color="var(--play-gold-start)"></iconsax-icon>
          <div style="margin-top:8px;">No pending word submissions in queue.</div>
        </td>
      </tr>`;
    return;
  }

  submissions.forEach(sub => {
    const tr = document.createElement('tr');
    const statusBadgeClass = sub.status === 'approved' ? 'badge-approved' : (sub.status === 'rejected' ? 'badge-rejected' : 'badge-pending');
    
    tr.innerHTML = `
      <td><strong>${escapeHtml(sub.kasiguranin)}</strong></td>
      <td>${escapeHtml(sub.tagalog || '-')}</td>
      <td>${escapeHtml(sub.english || '-')}</td>
      <td><span class="badge badge-category">${escapeHtml(sub.category || 'General')}</span></td>
      <td><span class="badge badge-category" style="background:rgba(255,255,255,0.1); color:#fff; border-color:var(--border-color);">${escapeHtml(sub.partOfSpeech || '-')}</span></td>
      <td><small>${escapeHtml(sub.contributorName || 'Anonymous')}</small></td>
      <td><span class="badge ${statusBadgeClass}">${(sub.status || 'pending').toUpperCase()}</span></td>
      <td>
        ${sub.status === 'pending' ? `
          <button class="btn btn-success btn-sm approve-btn" data-id="${sub.id}"><iconsax-icon name="tick-circle" type="bulk" size="16" color="#12161F"></iconsax-icon> Approve</button>
          <button class="btn btn-danger btn-sm reject-btn" data-id="${sub.id}"><iconsax-icon name="close-circle" type="bulk" size="16" color="#FFFFFF"></iconsax-icon> Reject</button>
        ` : `
          <span style="color:var(--text-muted); font-size:0.85rem;">Processed</span>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('.approve-btn').forEach(btn => {
    btn.addEventListener('click', () => approveSubmission(btn.getAttribute('data-id')));
  });
  tbody.querySelectorAll('.reject-btn').forEach(btn => {
    btn.addEventListener('click', () => rejectSubmission(btn.getAttribute('data-id')));
  });
}

function renderSubmissionsError(message) {
  const tbody = document.getElementById('submissions-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#FF7675; padding:2rem;">⚠️ ${escapeHtml(message)}</td></tr>`;
  }
}

// ── Approve Submission ──────────────────────────────────────────────────────
async function approveSubmission(id) {
  const sub = submissions.find(s => s.id === id);
  if (!sub) return;

  try {
    const newVocabRef = doc(collection(db, "vocabulary"));
    await setDoc(newVocabRef, {
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
    });

    await updateDoc(doc(db, "word_submissions", id), {
      status: "approved",
      reviewedAt: Date.now()
    });

    await logAudit("submission.approve", { submissionId: id, word: sub.kasiguranin });
    alert(`Successfully approved "${sub.kasiguranin}" and migrated to master dictionary!`);
  } catch (error) {
    console.error("Error approving submission:", error);
    alert("Failed to approve submission: " + error.message);
  }
}

// ── Reject Submission ───────────────────────────────────────────────────────
async function rejectSubmission(id) {
  if (!confirm("Are you sure you want to reject this user submission?")) return;
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

// ── Render Vocabulary Table ─────────────────────────────────────────────────
function renderVocabularyTable() {
  const tbody = document.getElementById('vocabulary-tbody');
  if (!tbody) return;

  tbody.innerHTML = '';

  const rawSearch = document.getElementById('search-vocab-input')?.value || '';
  const searchInput = rawSearch.trim().toLowerCase();
  const categoryFilter = document.getElementById('filter-vocab-category')?.value || '';

  const filtered = vocabulary.filter(item => {
    const matchesSearch = !searchInput || 
                          (item.kasiguranin || '').toLowerCase().includes(searchInput) || 
                          (item.tagalog || '').toLowerCase().includes(searchInput) ||
                          (item.english || '').toLowerCase().includes(searchInput);
    const matchesCat = !categoryFilter || item.category === categoryFilter;
    return matchesSearch && matchesCat;
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align:center; padding:2.5rem; color:var(--text-muted);">
          <iconsax-icon name="book-1" type="bulk" size="32" color="var(--play-purple-start)"></iconsax-icon>
          <div style="margin-top:8px;">No matching Kasiguranin entries found.</div>
        </td>
      </tr>`;
    return;
  }

  filtered.forEach(item => {
    const tr = document.createElement('tr');
    const conjugationsCount = item.conjugations ? item.conjugations.length : 0;
    const conjugationsBadge = conjugationsCount > 0 
      ? `<span class="badge badge-category" style="background:rgba(255, 201, 74, 0.15); color:var(--play-gold-start); border-color:rgba(255, 201, 74, 0.3);">${conjugationsCount} Conjugation${conjugationsCount > 1 ? 's' : ''}</span>`
      : `<code>${escapeHtml(item.ipaNotation || '-')}</code>`;

    tr.innerHTML = `
      <td><strong>${escapeHtml(item.kasiguranin)}</strong></td>
      <td>${escapeHtml(item.tagalog || '-')}</td>
      <td>${escapeHtml(item.english || '-')}</td>
      <td><span class="badge badge-category">${escapeHtml(item.category || 'General')}</span></td>
      <td><span class="badge badge-category" style="background:rgba(255,255,255,0.1); color:#fff; border-color:var(--border-color);">${escapeHtml(item.partOfSpeech || '-')}</span></td>
      <td>${conjugationsBadge}</td>
      <td>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" onclick="window.openEditVocabModal('${item.id}')"><iconsax-icon name="edit" type="bulk" size="16" color="#FFFFFF"></iconsax-icon> Edit</button>
          <button class="btn btn-danger btn-sm" onclick="window.deleteVocabWord('${item.id}')"><iconsax-icon name="close-circle" type="bulk" size="16" color="#FFFFFF"></iconsax-icon> Delete</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function renderVocabularyError(message) {
  const tbody = document.getElementById('vocabulary-tbody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#FF7675; padding:2rem;">⚠️ ${escapeHtml(message)}</td></tr>`;
  }
}

// ── Delete Vocabulary Word ──────────────────────────────────────────────────
window.deleteVocabWord = async function(id) {
  if (!confirm("Are you sure you want to delete this word from the master dictionary?")) return;
  try {
    await deleteDoc(doc(db, "vocabulary", id));
    await logAudit("vocabulary.delete", { id });
  } catch (e) {
    alert("Error deleting word: " + e.message);
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
        alert(`No valid INSERT INTO vocabulary statements found in "${file.name}"!`);
        return;
      }

      if (!confirm(`Import ${entries.length} SQL vocabulary records from "${file.name}" into Firestore?`)) return;

      let count = 0;
      for (const entry of entries) {
        const newDoc = doc(collection(db, "vocabulary"));
        await setDoc(newDoc, entry);
        count++;
      }

      alert(`Successfully imported ${count} entries from SQL migration script into Firestore!`);
    } catch (err) {
      console.error("SQL Parsing Error:", err);
      alert("Failed to parse SQL file: " + err.message);
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
      const rawRows = XLSX.utils.sheet_to_json(firstSheet);

      if (rawRows.length === 0) {
        alert("The selected Excel file contains no data rows!");
        return;
      }

      if (!confirm(`Import ${rawRows.length} rows from Excel "${file.name}" into Firestore vocabulary collection?`)) return;

      let count = 0;
      for (const rawRow of rawRows) {
        const row = {};
        Object.keys(rawRow).forEach(key => {
          row[key.trim().toLowerCase()] = rawRow[key];
        });

        const kasiguranin = row.kasiguranin || row.word || row.entry;
        if (!kasiguranin) continue;

        const wordClean = String(kasiguranin).trim();
        if (!wordClean) continue;

        const newDoc = doc(collection(db, "vocabulary"));
        await setDoc(newDoc, {
          kasiguranin: wordClean,
          tagalog: String(row.tagalog || "").trim() || null,
          english: String(row.english || "").trim() || null,
          category: String(row.category || "General").trim(),
          ipaNotation: String(row.ipa || row.ipanotation || "").trim() || null,
          importedFromExcel: file.name,
          createdAt: Date.now()
        });
        count++;
      }

      alert(`Successfully imported ${count} Kasiguranin entries into the cloud database!`);
    } catch (err) {
      console.error("Excel Parsing Error:", err);
      alert("Failed to parse Excel file: " + err.message);
    }
  };
  reader.readAsArrayBuffer(file);
}

// ── Render Releases List ────────────────────────────────────────────────────
function renderReleasesList() {
  const container = document.getElementById('releases-list-container');
  if (!container) return;

  container.innerHTML = '';

  if (releases.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:2.5rem; color:var(--text-muted); background:var(--bg-surface); border-radius:var(--radius-hero); border:1px solid var(--border-color);">
        <iconsax-icon name="box-search" type="bulk" size="36" color="var(--play-pink-start)"></iconsax-icon>
        <div style="margin-top:8px;">No APK releases published yet. Use the form above to publish Version 1.0.0!</div>
      </div>`;
    return;
  }

  releases.forEach(rel => {
    const card = document.createElement('div');
    card.className = 'panel';
    card.style.marginBottom = '1rem';
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <div>
          <h3 style="font-size:1.2rem; font-weight:800; color:#fff;">v${escapeHtml(rel.versionName)} <small style="color:var(--text-muted);">(Build ${rel.versionCode})</small></h3>
          <p style="color:var(--text-muted); font-size:0.85rem; margin-top:4px;">Released: ${new Date(rel.releasedAt).toLocaleDateString()}</p>
          <p style="margin-top:8px; font-size:0.95rem; color:var(--text-main);">${escapeHtml(rel.releaseNotes || 'No release notes provided.')}</p>
        </div>
        <div>
          <a href="${escapeHtml(rel.apkUrl || '#')}" target="_blank" class="btn btn-success btn-sm"><iconsax-icon name="document-download" type="bulk" size="18" color="#12161F"></iconsax-icon> Download APK</a>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// ── Form Listeners ──────────────────────────────────────────────────────────
function initFormListeners() {
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

      if (!word) return alert("Please enter the Kasiguranin word.");

      const isDuplicate = vocabulary.some(v => (v.kasiguranin || '').toLowerCase() === word.toLowerCase());
      if (isDuplicate) {
        if (!confirm(`"${word}" already exists in the master dictionary. Do you want to add it anyway?`)) return;
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
          createdAt: Date.now()
        });
        await logAudit("vocabulary.create", { word });
        addVocabForm.reset();
        closeModal('add-vocab-modal');
        alert(`Successfully added "${word}" to dictionary!`);
      } catch (error) {
        alert("Error adding word: " + error.message);
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

      if (!word) return alert("Please enter the Kasiguranin word.");

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
          updatedAt: Date.now()
        });
        await logAudit("vocabulary.update", { id, word });
        editVocabForm.reset();
        closeModal('edit-vocab-modal');
        alert(`Successfully updated "${word}"!`);
      } catch (error) {
        alert("Error updating word: " + error.message);
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

      if (isNaN(code) || code <= 0) return alert("Please enter a valid positive integer version code (e.g. 1, 2, 3).");
      if (!name) return alert("Please enter a version name (e.g. 1.0.0).");
      if (!url.startsWith('http://') && !url.startsWith('https://')) return alert("Direct APK Download link must start with http:// or https://");

      try {
        await addDoc(collection(db, "app_releases"), {
          versionCode: code,
          versionName: name,
          apkUrl: url,
          releaseNotes: notes,
          releasedAt: Date.now()
        });
        await logAudit("release.publish", { versionCode: code, versionName: name, apkUrl: url });
        releaseForm.reset();
        alert(`Successfully published KasiGuru v${name} APK release!`);
      } catch (err) {
        alert("Failed to publish release: " + err.message);
      }
    });
  }

  const searchInput = document.getElementById('search-vocab-input');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(searchDebounceTimer);
      searchDebounceTimer = setTimeout(() => renderVocabularyTable(), 300);
    });
  }

  const categoryFilter = document.getElementById('filter-vocab-category');
  if (categoryFilter) {
    categoryFilter.addEventListener('change', renderVocabularyTable);
  }
}

// ── Helpers ─────────────────────────────────────────────────────────────────
function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

window.openModal = function(id) {
  document.getElementById(id)?.classList.add('active');
};
window.closeModal = function(id) {
  document.getElementById(id)?.classList.remove('active');
};

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
