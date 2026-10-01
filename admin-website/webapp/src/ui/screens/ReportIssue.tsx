/**
 * Report an issue (ui/screens/report/ReportIssueScreen): a bug or a wrong word, with a screenshot.
 * The photo is compressed in the browser until it fits the rule's 700,000-character cap, the same
 * quality-then-scale ladder ImageCompressor.kt uses on Android.
 */
import { useState } from 'preact/hooks';
import { back } from '../../lib/router';
import { submitReport } from '../../lib/remote';
import { useApp } from '../../lib/store';
import { ClayButton, GroundScaffold, Icon, Jepjep } from '../kit';
import type { IconName } from '../icons.generated';

const CATEGORIES: { label: string; icon: IconName }[] = [
  { label: 'Bug / System Issue', icon: 'setting' },
  { label: 'Wrong Word / Translation', icon: 'book' },
  { label: 'Grammar / Literature', icon: 'document' },
  { label: 'Audio Issue', icon: 'volumeHigh' },
  { label: 'Other', icon: 'infoCircle' },
];
const MAX_CHARS = 700_000;

async function compress(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const scales = [1, 0.75, 0.5, 0.35, 0.25];
  const qualities = [0.8, 0.65, 0.5, 0.35];
  const longest = Math.max(bitmap.width, bitmap.height);
  const base = longest > 1600 ? 1600 / longest : 1;
  for (const s of scales) {
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(bitmap.width * base * s));
    canvas.height = Math.max(1, Math.round(bitmap.height * base * s));
    canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    for (const q of qualities) {
      const url = canvas.toDataURL('image/jpeg', q);
      if (url.length <= MAX_CHARS) return url;
    }
  }
  throw new Error('That image is too large to attach even after compressing. Try a smaller screenshot.');
}

export function ReportIssueScreen({ category, word, screenContext }: { category: string | null; word: string | null; screenContext: string | null }) {
  const p = useApp((s) => s.learner.progress);
  const account = useApp((s) => s.account);
  const [cat, setCat] = useState(CATEGORIES.some((c) => c.label === category) ? category! : CATEGORIES[0].label);
  const [targetWord, setTargetWord] = useState(word ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [name, setName] = useState(p.fullName || '');
  const [email, setEmail] = useState(p.email || account.email || '');
  const [photo, setPhoto] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [doneId, setDoneId] = useState<string | null>(null);
  const isWord = cat === 'Wrong Word / Translation';

  const submit = async () => {
    if (isWord && !targetWord.trim()) return setError('Please enter the Kasiguranin word.');
    if (!title.trim()) return setError('Please add a short summary title.');
    if (!description.trim()) return setError('Please describe the issue.');
    if (!photo) return setError('Please attach a screenshot or photo.');
    if (!name.trim()) return setError('Please enter your name.');
    setBusy(true);
    setError(null);
    try {
      const id = await submitReport({
        category: cat,
        title: title.trim().slice(0, 250),
        description: description.trim().slice(0, 5000),
        targetWord: targetWord.trim().slice(0, 150),
        targetScreen: (screenContext ?? '').slice(0, 100),
        photoBase64: photo,
        reporterName: name.trim().slice(0, 100),
        reporterEmail: email.trim().slice(0, 150),
      });
      setDoneId(id);
    } catch (e) {
      setError((e as Error).message || 'Could not send the report. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  if (doneId) {
    return (
      <GroundScaffold title="Report an issue">
        <div class="readable stack center" style={{ paddingTop: 'var(--s-xl)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="encouraging_approve" height={160} breathe />
          </div>
          <h1 class="t-headline">Report submitted!</h1>
          <p class="t-body-l muted">Thank you for helping us improve KasiGuru! Our development and language moderation team will review this issue promptly.</p>
          <p class="t-label faint">Reference ID: #{doneId.slice(0, 8)}</p>
          <ClayButton label="Done" onClick={() => back('/')} />
        </div>
      </GroundScaffold>
    );
  }

  return (
    <GroundScaffold title="Report an issue" largeTitle subtitle="Found a bug, glitch, or wrong word? Help us fix it.">
      <form class="readable stack-lg" style={{ maxWidth: 640 }} onSubmit={(e) => { e.preventDefault(); void submit(); }}>
        <section class="stack-sm">
          <h2 class="t-title-l">1. Category</h2>
          <p class="t-body muted">What kind of issue are you reporting?</p>
          <div class="stack-sm" role="radiogroup">
            {CATEGORIES.map((c) => (
              <button type="button" key={c.label} role="radio" aria-checked={cat === c.label} class={`option${cat === c.label ? ' selected' : ''}`} onClick={() => setCat(c.label)}>
                <Icon name={c.icon} size={20} color={cat === c.label ? 'var(--lime)' : 'var(--muted)'} />
                <span class="label">{c.label}</span>
              </button>
            ))}
          </div>
        </section>
        <section class="stack">
          <h2 class="t-title-l">2. Details</h2>
          {isWord && (
            <label class="field">
              <span>Kasiguranin word *</span>
              <input class="input" value={targetWord} placeholder="e.g. apak, bëkas, talapid" onInput={(e) => setTargetWord((e.target as HTMLInputElement).value)} />
            </label>
          )}
          <label class="field">
            <span>Summary title *</span>
            <input class="input" value={title} maxLength={250} placeholder={isWord ? 'e.g. Wrong English meaning' : 'e.g. The lesson froze after Check'} onInput={(e) => setTitle((e.target as HTMLInputElement).value)} />
          </label>
          <label class="field">
            <span>Detailed description *</span>
            <textarea
              class="input"
              value={description}
              maxLength={5000}
              placeholder={isWord ? 'Describe what is wrong with the current word or translation, and what the correct Kasiguranin meaning or spelling should be.' : 'Describe what happened, the steps to reproduce it, or where in the app you encountered this problem.'}
              onInput={(e) => setDescription((e.target as HTMLTextAreaElement).value)}
            />
          </label>
        </section>
        <section class="stack-sm">
          <h2 class="t-title-l">3. Photo evidence *</h2>
          <p class="t-body-s muted">Attach a screenshot showing the bug or typo (required)</p>
          <label class="card" style={{ cursor: 'pointer', borderStyle: photo ? 'solid' : 'dashed', display: 'grid', gap: 8, justifyItems: 'center', textAlign: 'center' }}>
            {photo ? (
              <>
                <img src={photo} alt="Attached screenshot" style={{ maxHeight: 220, borderRadius: 12 }} />
                <span class="t-label" style={{ color: 'var(--lime)' }}>Photo attached · tap to change</span>
              </>
            ) : (
              <>
                <Icon name="gallery" size={32} color="var(--muted)" />
                <p class="t-title-s">Add screenshot / photo evidence *</p>
                <p class="t-body-s muted">Tap to choose an image</p>
              </>
            )}
            <input
              type="file"
              accept="image/*"
              class="sr-only"
              onChange={async (e) => {
                const file = (e.target as HTMLInputElement).files?.[0];
                if (!file) return;
                try {
                  setError(null);
                  setPhoto(await compress(file));
                } catch (err) {
                  setError((err as Error).message);
                }
              }}
            />
          </label>
        </section>
        <section class="stack">
          <h2 class="t-title-l">4. About you</h2>
          <p class="t-body-s muted">Your browser and device type are attached automatically to help diagnose errors.</p>
          <label class="field">
            <span>Your name *</span>
            <input class="input" value={name} maxLength={100} onInput={(e) => setName((e.target as HTMLInputElement).value)} />
          </label>
          <label class="field">
            <span>Email (optional, for follow-up)</span>
            <input class="input" type="email" value={email} maxLength={150} onInput={(e) => setEmail((e.target as HTMLInputElement).value)} />
          </label>
        </section>
        {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
        <ClayButton label={busy ? 'Sending…' : 'Submit report'} type="submit" disabled={busy} />
      </form>
    </GroundScaffold>
  );
}
