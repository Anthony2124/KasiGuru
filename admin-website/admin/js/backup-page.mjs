// backup-page.mjs — the portal's Backup & Restore page (#tab-backup, admins only).
//
// Download: reads the whole database with the service-account key and saves it as a zip holding the
// same folder functions/backup_firestore.js writes. Restore: takes a dropped zip or backup folder,
// previews what it would change, and writes it once the operator types the project id: the steps
// and safeguards of functions/restore_firestore.js, on a page. docs/BACKUP_AND_RESET.md is the
// runbook for both; change them together.
//
// The admin login only opens the page. Every read and write goes through the key, which stays in
// this tab: it is never uploaded, never saved, and forgotten when the tab closes or on Forget key.

import { loadServiceAccount, firestoreRest, isQuotaError } from './firestore-rest.mjs';
import {
  takeBackup, selectBackupFiles, readBackupFiles, previewRestore, recheckGuarded, runRestore, sumPlans, LEARNER_COLLECTIONS
} from './database-backup.mjs';
import { createZip, readZip } from './zip.mjs';

const $ = (id) => document.getElementById(id);
const fmt = (n) => Number(n).toLocaleString();
const plural = (n, one, many = one + 's') => `${fmt(n)} ${n === 1 ? one : many}`;
const megabytes = (bytes) => `${(bytes / 1024 / 1024).toFixed(1)} MB`;

function saveBlob(blob, name) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

// A dropped folder, read through the File System entries API: every file under it with its path.
async function walkDirectory(dir) {
  const files = [];
  const visit = async (entry) => {
    if (entry.isFile) {
      files.push({ path: entry.fullPath.replace(/^\/+/, ''), read: () => new Promise((ok, fail) => entry.file(ok, fail)) });
      return;
    }
    const reader = entry.createReader();
    for (;;) {
      const batch = await new Promise((ok, fail) => reader.readEntries(ok, fail));
      if (batch.length === 0) break;
      for (const child of batch) await visit(child);
    }
  };
  await visit(dir);
  return files;
}

export function initBackupPage({ projectId, notify, logAudit, escapeHtml }) {
  const page = $('tab-backup');
  if (!page) return;
  page.querySelectorAll('[data-bk-project]').forEach((el) => { el.textContent = projectId; });

  let account = null;   // { projectId, clientEmail, getToken } from the dropped key
  let backup = null;    // the dropped backup, from readBackupFiles
  let plans = null;     // the preview for the current choice of collections
  let planWriteOnly = false;
  let busy = false;

  const client = () => firestoreRest({ projectId, getToken: account.getToken });
  const alert = (kind, html) => `<p class="bk-alert is-${kind}">${html}</p>`;

  function selected() {
    return new Set([...page.querySelectorAll('#bk-collection-list input:checked')].map((i) => i.value));
  }

  function refresh() {
    const ready = Boolean(account) && !busy;
    $('bk-key-drop').hidden = Boolean(account);
    $('bk-key-loaded').hidden = !account;
    $('bk-key-forget').disabled = busy;
    $('bk-download').disabled = !ready;
    $('bk-download-hint').hidden = Boolean(account);
    $('bk-collections').disabled = !backup || busy;
    $('bk-write-only').disabled = !backup || busy;
    $('bk-preview').disabled = !ready || !backup || selected().size === 0;
    $('bk-preview-hint').textContent = !backup ? 'Drop a backup first.' : !account ? 'Load the key first.' : '';
    const writes = plans ? sumPlans(plans).writes : 0;
    $('bk-confirm-input').disabled = !ready || writes === 0;
    $('bk-restore').disabled = !ready || writes === 0 || $('bk-confirm-input').value.trim() !== projectId;
    $('bk-restore').textContent = writes ? `Restore ${plural(writes, 'document')}` : 'Restore';
    page.querySelectorAll('.dropzone').forEach((z) => z.classList.toggle('is-disabled', busy));
  }

  function progress(prefix, state) {
    const box = $(`${prefix}-progress`);
    box.hidden = !state;
    if (!state) return;
    const bar = box.querySelector('progress');
    if (state.max) { bar.max = state.max; bar.value = state.value; } else bar.removeAttribute('value');
    box.querySelector('p').textContent = state.text;
  }

  // Closing the tab mid-restore would stop it part-way; the browser asks first.
  const holdTab = (e) => { e.preventDefault(); e.returnValue = ''; };
  async function run(task) {
    if (busy) return;
    busy = true;
    addEventListener('beforeunload', holdTab);
    refresh();
    try { await task(); } finally {
      busy = false;
      removeEventListener('beforeunload', holdTab);
      refresh();
    }
  }

  function dropzone(zone, input, onDrop) {
    zone.addEventListener('click', () => { if (!busy) input.click(); });
    zone.addEventListener('keydown', (e) => {
      if (e.target === zone && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); if (!busy) input.click(); }
    });
    zone.addEventListener('dragover', (e) => { e.preventDefault(); if (!busy) zone.classList.add('dragover'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
    zone.addEventListener('drop', (e) => {
      e.preventDefault();
      zone.classList.remove('dragover');
      // The drop's items can only be read now, inside the event; folders come back as entries.
      const entries = [...(e.dataTransfer.items || [])].map((i) => i.webkitGetAsEntry?.()).filter(Boolean);
      const files = [...e.dataTransfer.files];
      if (!busy) onDrop({ entries, files });
    });
  }

  // A file dropped just beside a drop zone would open in the tab and take the loaded key with it.
  // Outside the zones the drag shows "not allowed" and the drop does nothing.
  for (const type of ['dragover', 'drop']) {
    addEventListener(type, (e) => {
      if (!page.classList.contains('active') || e.target.closest?.('.dropzone')) return;
      e.preventDefault();
      if (type === 'dragover' && e.dataTransfer) e.dataTransfer.dropEffect = 'none';
    });
  }

  // ── Key ─────────────────────────────────────────────────────────────────────────────────────
  async function loadKey(file) {
    $('bk-key-error').hidden = true;
    if (!file) return;
    await run(async () => {
      try {
        if (file.size > 64 * 1024) throw new Error('That file is too large to be a service-account key.');
        const loaded = await loadServiceAccount(await file.text());
        if (loaded.projectId !== projectId) {
          throw new Error(`That key is for the project ${loaded.projectId}. This portal manages ${projectId}.`);
        }
        await loaded.getToken(); // Proves Google still accepts the key before anything relies on it.
        account = loaded;
        $('bk-key-email').textContent = loaded.clientEmail;
        notify('Key loaded. It stays in this tab.', 'success');
      } catch (err) {
        $('bk-key-error').textContent = err.message;
        $('bk-key-error').hidden = false;
      }
    });
  }

  dropzone($('bk-key-drop'), $('bk-key-input'), ({ files }) => loadKey(files[0]));
  $('bk-key-input').addEventListener('change', (e) => { loadKey(e.target.files[0]); e.target.value = ''; });
  $('bk-key-forget').addEventListener('click', () => {
    account = null;
    plans = null;
    $('bk-preview-result').innerHTML = '';
    $('bk-confirm-input').value = '';
    refresh();
    notify('Key forgotten.', 'info');
  });

  // ── Backup ──────────────────────────────────────────────────────────────────────────────────
  $('bk-download').addEventListener('click', () => run(async () => {
    const out = $('bk-backup-result');
    out.innerHTML = '';
    const c = client();
    try {
      const result = await takeBackup(c, {
        onProgress: ({ collection, index, total, documents }) => progress('bk-backup', {
          text: `Reading ${collection} (${index + 1} of ${total}), ${plural(documents, 'document')} so far`,
          value: index, max: total
        })
      });
      progress('bk-backup', { text: 'Packing the zip…' });
      const zip = await createZip(result.files.map((f) => ({ name: `${result.stamp}/${f.name}`, data: f.text })));
      const fileName = `kasiguru-backup-${result.stamp}.zip`;
      saveBlob(zip, fileName);

      const counts = Object.entries(result.manifest.collections);
      const total = counts.reduce((n, [, count]) => n + count, 0);
      out.innerHTML =
        alert('ok', `Saved <b>${escapeHtml(fileName)}</b> (${megabytes(zip.size)}) to your downloads: ` +
          `${plural(total, 'document')} in ${plural(counts.length, 'collection')}. Keep it private.`) +
        `<details class="bk-details"><summary>Documents per collection</summary><div class="table-responsive"><table class="bk-table">
          <thead><tr><th>Collection</th><th class="num">Documents</th></tr></thead>
          <tbody>${counts.map(([name, count]) => `<tr><td>${escapeHtml(name)}</td><td class="num">${fmt(count)}</td></tr>`).join('')}</tbody>
        </table></div></details>`;
      logAudit('database.backup', { file: fileName, documents: total, collections: counts.length });
      notify('Backup downloaded.', 'success');
    } catch (err) {
      out.innerHTML = alert('error', `The backup did not finish: ${escapeHtml(err.message)} Nothing was downloaded.`);
    } finally {
      progress('bk-backup', null);
    }
  }));

  // ── Restore: the backup ─────────────────────────────────────────────────────────────────────
  function renderBackup() {
    const box = $('bk-restore-summary');
    const list = $('bk-collection-list');
    if (!backup) {
      box.innerHTML = '';
      list.innerHTML = '<p class="bk-hint">Drop a backup first.</p>';
      return;
    }
    const m = backup.manifest;
    const total = backup.collections.reduce((n, c) => n + c.real, 0);
    const taken = m.createdAt ? new Date(m.createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : 'unknown';
    const warnings = [];
    if (m.projectId && m.projectId !== projectId) {
      warnings.push(alert('warn', `This backup is from another project, <b>${escapeHtml(m.projectId)}</b>. Restoring it copies that project's data into ${escapeHtml(projectId)}.`));
    }
    if (!m.format || m.format < 2) {
      warnings.push(alert('warn', 'This backup is from before September 2026 and holds no learner progress. It can restore content only.'));
    }
    box.innerHTML = `
      <dl class="bk-summary">
        <div><dt>Backup</dt><dd>${escapeHtml(backup.name)}</dd></div>
        <div><dt>Taken</dt><dd>${escapeHtml(taken)}</dd></div>
        <div><dt>Project</dt><dd>${escapeHtml(m.projectId || 'unknown')}</dd></div>
        <div><dt>Holds</dt><dd>${plural(total, 'document')} in ${plural(backup.collections.length, 'collection')}</dd></div>
      </dl>${warnings.join('')}`;
    list.innerHTML = backup.collections.map((c) => `
      <label class="check-label"><input type="checkbox" value="${escapeHtml(c.collection)}" checked>
        <span>${escapeHtml(c.collection)}</span><span class="bk-count">${fmt(c.real)}</span></label>`).join('');
  }

  async function loadBackup({ entries = [], files = [] }) {
    const error = $('bk-restore-error');
    error.hidden = true;
    await run(async () => {
      try {
        const dirs = entries.filter((e) => e.isDirectory);
        let found;
        let fallback = 'the dropped files';
        if (dirs.length > 1) throw new Error('Drop one backup folder at a time.');
        if (dirs.length === 1) {
          found = await walkDirectory(dirs[0]);
          fallback = dirs[0].name;
        } else if (files.length === 1 && /\.zip$/i.test(files[0].name)) {
          progress('bk-load', { text: `Opening ${files[0].name}…` });
          const decoder = new TextDecoder();
          const inside = await readZip(await files[0].arrayBuffer(), (name) => /\.json$/i.test(name));
          found = inside.map((f) => ({ path: f.name, text: decoder.decode(f.data) }));
          fallback = files[0].name.replace(/\.zip$/i, '');
        } else {
          found = files.map((f) => ({ path: f.webkitRelativePath || f.name, read: async () => f }));
        }
        if (found.length === 0) throw new Error('Nothing to read there. Drop a backup folder or zip.');

        const wanted = new Set(selectBackupFiles(found.map((f) => f.path)));
        const texts = [];
        for (const f of found.filter((x) => wanted.has(x.path))) {
          progress('bk-load', { text: `Reading ${f.path.split('/').pop()}…` });
          texts.push({ path: f.path, text: f.text ?? await (await f.read()).text() });
        }
        backup = readBackupFiles(texts, fallback);
        plans = null;
        $('bk-preview-result').innerHTML = '';
        $('bk-write-only').checked = false;
        renderBackup();
      } catch (err) {
        error.textContent = err.message;
        error.hidden = false;
      } finally {
        progress('bk-load', null);
      }
    });
  }

  dropzone($('bk-restore-drop'), $('bk-restore-input'), loadBackup);
  $('bk-restore-input').addEventListener('change', (e) => { loadBackup({ files: [...e.target.files] }); e.target.value = ''; });
  $('bk-restore-folder-btn').addEventListener('click', () => { if (!busy) $('bk-restore-folder').click(); });
  $('bk-restore-folder').addEventListener('change', (e) => { loadBackup({ files: [...e.target.files] }); e.target.value = ''; });

  // Any change to what would be restored makes the preview stale; the restore needs a fresh one.
  function invalidate() {
    plans = null;
    $('bk-preview-result').innerHTML = '';
    $('bk-confirm-input').value = '';
    refresh();
  }
  $('bk-collections').addEventListener('change', invalidate);
  $('bk-write-only').addEventListener('change', invalidate);
  page.querySelectorAll('[data-bk-tick]').forEach((btn) => btn.addEventListener('click', () => {
    const set = btn.dataset.bkTick === 'learner' ? new Set(LEARNER_COLLECTIONS) : null;
    page.querySelectorAll('#bk-collection-list input').forEach((i) => {
      i.checked = btn.dataset.bkTick === 'all' || Boolean(set?.has(i.value));
    });
    invalidate();
  }));

  // ── Restore: preview ────────────────────────────────────────────────────────────────────────
  function renderPreview() {
    const out = $('bk-preview-result');
    if (!plans) { out.innerHTML = ''; return; }
    const t = sumPlans(plans);
    const row = (name, p, cls = '') => `<tr class="${cls}"><td>${name}</td><td class="num">${fmt(p.restore)}</td>` +
      `<td class="num">${fmt(p.overwrite)}</td><td class="num">${fmt(p.same)}</td><td class="num">${fmt(p.kept)}</td></tr>`;
    const notes = [];
    if (planWriteOnly) {
      notes.push(alert('warn', "Firestore was not read, so every document is written and nothing is kept: a learner whose phone synced since the backup is overwritten. Safe right after a wipe. Once reads are back, preview normally: it should find nothing left to write."));
    }
    if (t.writes === 0) notes.push(alert('ok', 'Nothing to write. Firestore already holds everything in this backup.'));
    else if (t.writes > 20000) notes.push(alert('warn', `${plural(t.writes, 'write')} is more than the free plan's 20,000 a day. The restore will stop when they run out; preview and restore again the next day to finish.`));
    out.innerHTML = `
      <div class="table-responsive"><table class="bk-table">
        <thead><tr><th>Collection</th><th class="num">Restore</th><th class="num">Overwrite</th><th class="num">Same</th><th class="num">Kept</th></tr></thead>
        <tbody>${plans.map((p) => row(escapeHtml(p.collection), p.plan)).join('')}${row('Total', t, 'bk-total')}</tbody>
      </table></div>
      <p class="bk-legend"><b>Restore</b>: missing, written back. <b>Overwrite</b>: changed since the backup, replaced by the backup's copy,
        which undoes vandalism and also any edit made since. <b>Same</b>: already identical. <b>Kept</b>: a learner whose phone synced
        past the backup, or a device token already present; left as it is.</p>
      ${t.writes ? `<p class="bk-legend">${plural(t.writes, 'document write')}. The free plan allows 20,000 a day.</p>` : ''}
      ${notes.join('')}`;
  }

  $('bk-preview').addEventListener('click', () => run(async () => {
    plans = null;
    renderPreview();
    const only = selected();
    const writeOnly = $('bk-write-only').checked;
    try {
      plans = await previewRestore(client(), backup, {
        only: only.size === backup.collections.length ? null : only,
        writeOnly,
        onProgress: ({ collection, index, total, documents }) => progress('bk-preview', {
          text: writeOnly ? `Counting ${collection}…` : `Reading ${collection} from Firestore (${index + 1} of ${total}), ${plural(documents, 'document')} so far`,
          value: index, max: total
        })
      });
      planWriteOnly = writeOnly;
      renderPreview();
    } catch (err) {
      const out = $('bk-preview-result');
      out.innerHTML = alert('error', `The preview did not finish: ${escapeHtml(err.message)}`);
      if (isQuotaError(err) && !writeOnly) {
        out.innerHTML += alert('warn', `Writes have their own limit and still work, so learner data can come back now without reading.
          <button type="button" class="btn btn-outline btn-sm" id="bk-quota-setup">Set up a restore of learner data without reading</button>`);
        $('bk-quota-setup').addEventListener('click', () => {
          page.querySelectorAll('#bk-collection-list input').forEach((i) => { i.checked = LEARNER_COLLECTIONS.includes(i.value); });
          $('bk-write-only').checked = true;
          invalidate();
          $('bk-preview').focus();
        });
      }
    } finally {
      progress('bk-preview', null);
    }
  }));

  // ── Restore: write ──────────────────────────────────────────────────────────────────────────
  $('bk-confirm-input').addEventListener('input', refresh);

  $('bk-restore').addEventListener('click', () => run(async () => {
    const out = $('bk-restore-result');
    out.innerHTML = '';
    const c = client();
    try {
      let dropped = 0;
      if (!planWriteOnly) {
        progress('bk-restore', { text: 'Checking for learners who synced since the preview…' });
        dropped = await recheckGuarded(c, plans);
      }
      const totals = sumPlans(plans);
      const done = new Map();
      const written = await runRestore(c, plans, {
        onProgress: ({ collection, done: n, total }) => {
          done.set(collection, n);
          progress('bk-restore', {
            text: `Writing ${collection}: ${fmt(n)} of ${fmt(total)}`,
            value: [...done.values()].reduce((a, b) => a + b, 0), max: totals.writes
          });
        }
      });
      const only = plans.length === backup.collections.length ? null : plans.map((p) => p.collection);
      logAudit('database.restore', { restoredFrom: backup.name, only, writeOnly: planWriteOnly, written, kept: totals.kept });
      out.innerHTML = alert('ok', `Restore complete: ${plural(written, 'document')} written` +
        (dropped ? `, and ${plural(dropped, 'more', 'more')} kept because those phones synced after the preview` : '') +
        '. To check it, download a fresh backup and compare its counts with this one; only the activity log should differ.');
      notify('Restore complete.', 'success');
    } catch (err) {
      out.innerHTML = alert('error', `The restore stopped: ${escapeHtml(err.message)} It is safe to preview and restore again: documents already back count as same.`);
    } finally {
      progress('bk-restore', null);
      plans = null;
      renderPreview();
      $('bk-confirm-input').value = '';
    }
  }));

  renderBackup();
  refresh();
}
