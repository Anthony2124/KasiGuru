/**
 * Share a story or poem (SubmitLiteratureScreen): a PDF of the piece (required, under 500 KB so it
 * fits Firestore as base64), a title, and an optional page-by-page transcription. Lands in
 * `literature_submissions` as "pending".
 */
import { useState } from 'preact/hooks';
import { back } from '../../lib/router';
import { submitLiterature } from '../../lib/remote';
import { act, useApp } from '../../lib/store';
import { ClayButton, GroundScaffold, Icon, Jepjep } from '../kit';

const MAX_PDF = 500 * 1024;
const COOLDOWN_MS = 30_000;
let lastSubmittedAt = 0;

interface PageDraft {
  kasiguranin: string;
  tagalog: string;
  english: string;
}

function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result));
    r.onerror = () => reject(r.error);
    r.readAsDataURL(file);
  });
}

export function SubmitLiteratureScreen() {
  const p = useApp((s) => s.learner.progress);
  const [titleKas, setTitleKas] = useState('');
  const [title, setTitle] = useState('');
  const [name, setName] = useState(p.fullName || '');
  const [pages, setPages] = useState<PageDraft[]>([{ kasiguranin: '', tagalog: '', english: '' }]);
  const [pdf, setPdf] = useState<{ name: string; size: number; base64: string } | null>(null);
  const [reading, setReading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const pick = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;
    setError(null);
    if (file.size > MAX_PDF) return setError(`PDF is too large (${Math.round(file.size / 1024)} KB). Please upload a file under 500 KB.`);
    setReading(true);
    try {
      const head = new Uint8Array(await file.slice(0, 4).arrayBuffer());
      const isPdf = head[0] === 0x25 && head[1] === 0x50 && head[2] === 0x44 && head[3] === 0x46;
      if (!isPdf && !file.name.toLowerCase().endsWith('.pdf')) {
        setError('The selected file is not a valid PDF document.');
        return;
      }
      const url = await readAsDataUrl(file);
      setPdf({ name: file.name, size: file.size, base64: url.replace(/^data:[^;]*;/, 'data:application/pdf;') });
    } catch (err) {
      setError(`Failed to load PDF: ${(err as Error).message || 'Unknown error'}`);
    } finally {
      setReading(false);
    }
  };

  const submit = async () => {
    if (busy) return;
    if (Date.now() - lastSubmittedAt < COOLDOWN_MS) return setError('Please wait a moment before submitting another piece.');
    if (!pdf) return setError('Please attach a PDF file of your story or poem.');
    if (!title.trim() && !titleKas.trim()) return setError('Please give the piece a title.');
    setBusy(true);
    setError(null);
    const nonEmpty = pages.filter((pg) => pg.kasiguranin.trim() || pg.tagalog.trim() || pg.english.trim());
    const pagesJson = JSON.stringify(
      nonEmpty.map((pg, i) => ({ pageNumber: i + 1, kasiguranin: pg.kasiguranin.trim(), tagalog: pg.tagalog.trim(), english: pg.english.trim() }))
    );
    try {
      await submitLiterature({
        title: title.trim(),
        titleKasiguranin: titleKas.trim(),
        pagesJson,
        contributorName: name.trim() || 'Anonymous',
        pdfBase64: pdf.base64,
        pdfFileName: pdf.name.slice(0, 255),
      });
      lastSubmittedAt = Date.now();
      act((d) => d.incrementSubmissionsMade());
      setDone(true);
    } catch (e) {
      setError((e as Error).message || 'Failed to submit. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  if (done) {
    return (
      <GroundScaffold title="Share a story">
        <div class="readable stack center" style={{ paddingTop: 'var(--s-xl)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="reading" height={160} breathe />
          </div>
          <h1 class="t-headline">Submitted for review!</h1>
          <p class="t-body-l muted">A moderator will read it before it joins the stories collection.</p>
          <ClayButton label="Done" onClick={() => back('/')} />
        </div>
      </GroundScaffold>
    );
  }

  return (
    <GroundScaffold
      title="Submit a story or poem"
      largeTitle
      subtitle="Attach the piece as a PDF file, with a title and optional text transcription. It joins the pending queue for a moderator to review, the same as a submitted word."
    >
      <form class="readable stack-lg" style={{ maxWidth: 640 }} onSubmit={(e) => { e.preventDefault(); void submit(); }}>
        <section class="stack">
          <label class="field">
            <span>Title, in Kasiguranin</span>
            <input class="input" value={titleKas} maxLength={200} onInput={(e) => setTitleKas((e.target as HTMLInputElement).value)} />
          </label>
          <label class="field">
            <span>Title, translated</span>
            <input class="input" value={title} maxLength={200} onInput={(e) => setTitle((e.target as HTMLInputElement).value)} />
          </label>
          <label class="field">
            <span>Your name (optional)</span>
            <input class="input" value={name} maxLength={60} onInput={(e) => setName((e.target as HTMLInputElement).value)} />
          </label>
        </section>

        <section class="stack-sm">
          <h2 class="t-title-l">Story / manuscript document *</h2>
          <p class="t-body-s muted">PDF file required (max 500 KB)</p>
          <label class="card row" style={{ cursor: 'pointer', borderStyle: pdf ? 'solid' : 'dashed' }}>
            <Icon name={pdf ? 'tickCircle' : 'document'} size={28} color={pdf ? 'var(--lime)' : 'var(--muted)'} />
            <div class="grow">
              {reading ? (
                <p class="t-title-s">Reading and attaching PDF…</p>
              ) : pdf ? (
                <>
                  <p class="t-title-s">{pdf.name}</p>
                  <p class="t-body-s muted">{Math.round(pdf.size / 1024)} KB · tap to replace</p>
                </>
              ) : (
                <>
                  <p class="t-title-s">Attach story / poem (PDF) *</p>
                  <p class="t-body-s muted">Tap to choose a .pdf document from your device</p>
                </>
              )}
            </div>
            <input type="file" accept="application/pdf,.pdf" class="sr-only" onChange={pick} />
          </label>
          {pdf && (
            <button type="button" class="text-btn muted" style={{ alignSelf: 'flex-start' }} onClick={() => setPdf(null)}>
              Remove PDF
            </button>
          )}
        </section>

        <section class="stack">
          <h2 class="t-title-l">Page transcription (optional)</h2>
          {pages.map((pg, i) => (
            <div key={i} class="card stack-sm">
              <div class="row">
                <p class="t-title-s grow">Page {i + 1}</p>
                {pages.length > 1 && (
                  <button type="button" class="icon-btn" aria-label={`Remove page ${i + 1}`} onClick={() => setPages(pages.filter((_, j) => j !== i))}>
                    <Icon name="trash" size={20} color="var(--red)" />
                  </button>
                )}
              </div>
              {(['kasiguranin', 'tagalog', 'english'] as const).map((k) => (
                <label key={k} class="field">
                  <span>{k === 'kasiguranin' ? 'Kasiguranin' : k === 'tagalog' ? 'Tagalog (optional)' : 'English (optional)'}</span>
                  <textarea class="input" style={{ minHeight: 80 }} value={pg[k]} onInput={(e) => setPages(pages.map((x, j) => (j === i ? { ...x, [k]: (e.target as HTMLTextAreaElement).value } : x)))} />
                </label>
              ))}
            </div>
          ))}
          {pages.length < 40 && (
            <ClayButton label="Add another page" tone="quiet" icon="addCircle" onClick={() => setPages([...pages, { kasiguranin: '', tagalog: '', english: '' }])} />
          )}
        </section>

        {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
        <ClayButton label={busy ? 'Submitting…' : 'Submit for review'} type="submit" disabled={busy || reading} />
      </form>
    </GroundScaffold>
  );
}
