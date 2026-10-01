/**
 * Contribute a word (ui/screens/contribute/SubmitWordScreen): lands in `word_submissions` as
 * "pending" for a moderator. Checks the dictionary as the contributor types, so an existing entry is
 * pointed out before it is sent again.
 */
import { useMemo, useState } from 'preact/hooks';
import { CATEGORIES } from '../../domain/constants';
import { matchRecall, normalise } from '../../domain/recall';
import type { WordContent } from '../../domain/types';
import { back, navigate } from '../../lib/router';
import { submitWord } from '../../lib/remote';
import { act, useApp } from '../../lib/store';
import { ClayButton, Dialog, GroundScaffold, Icon, Jepjep } from '../kit';

const PARTS_OF_SPEECH = ['Noun', 'Verb', 'Adjective', 'Adverb', 'Pronoun', 'Preposition', 'Conjunction / Connector', 'Interjection', 'Marker & Particle'];
const COOLDOWN_MS = 30_000;
let lastSubmittedAt = 0;

type Level = 'SameSense' | 'SameWord' | 'SimilarSpelling';

/** DuplicateWordCheck.find: same sense, same headword, or a spelling one slip away. */
export function findDuplicates(word: string, english: string, corpus: WordContent[]) {
  const typed = normalise(word);
  if (!typed) return [];
  const gloss = normalise(english);
  const exact: { w: WordContent; level: Level }[] = [];
  const similar: { w: WordContent; level: Level }[] = [];
  for (const w of corpus) {
    const m = matchRecall(word, w.kasiguranin);
    if (m === 'Exact') exact.push({ w, level: gloss && normalise(w.english) === gloss ? 'SameSense' : 'SameWord' });
    else if (m === 'Close' && typed.length >= 5) similar.push({ w, level: 'SimilarSpelling' });
  }
  exact.sort((a, b) => (a.level === 'SameSense' ? 0 : 1) - (b.level === 'SameSense' ? 0 : 1));
  return [...exact, ...similar.slice(0, 3)];
}

export function SubmitWordScreen() {
  const words = useApp((s) => s.words);
  const p = useApp((s) => s.learner.progress);
  const [f, setF] = useState({
    kasiguranin: '', tagalog: '', english: '', rootForm: '', category: CATEGORIES[0], partOfSpeech: '', ipaNotation: '',
    exampleSentence: '', pastTense: '', presentTense: '', futureTense: '', contributorName: p.fullName || (p.userName !== 'Learner' ? p.userName : ''),
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDup, setConfirmDup] = useState(false);
  const [done, setDone] = useState(false);
  const set = (k: keyof typeof f) => (e: Event) => setF({ ...f, [k]: (e.target as HTMLInputElement).value });
  const matches = useMemo(() => findDuplicates(f.kasiguranin, f.english, words), [f.kasiguranin, f.english, words]);
  const needsConfirm = matches.some((m) => m.level !== 'SimilarSpelling');
  const sameSense = matches.some((m) => m.level === 'SameSense');

  const submit = async (confirmed = false) => {
    if (busy) return;
    if (Date.now() - lastSubmittedAt < COOLDOWN_MS) return setError('Please wait a moment before submitting another word.');
    if (!f.kasiguranin.trim()) return setError('Please enter the Kasiguranin word');
    if (!f.tagalog.trim() && !f.english.trim()) return setError('Please enter either Tagalog or English definition');
    if (!f.contributorName.trim()) return setError('Please enter your name for contributor credit');
    if (!confirmed && needsConfirm) return setConfirmDup(true);
    setBusy(true);
    setError(null);
    try {
      await submitWord({
        kasiguranin: f.kasiguranin.trim(),
        tagalog: f.tagalog.trim(),
        english: f.english.trim(),
        rootForm: f.rootForm.trim(),
        category: f.category,
        partOfSpeech: f.partOfSpeech,
        ipaNotation: f.ipaNotation.trim(),
        exampleSentence: f.exampleSentence.trim(),
        pastTense: f.pastTense.trim(),
        presentTense: f.presentTense.trim(),
        futureTense: f.futureTense.trim(),
        contributorName: f.contributorName.trim(),
      });
      lastSubmittedAt = Date.now();
      act((d) => d.incrementSubmissionsMade());
      setDone(true);
    } catch (e) {
      setError((e as Error).message || 'Failed to submit word. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  if (done) {
    return (
      <GroundScaffold title="Contribute">
        <div class="readable stack center" style={{ paddingTop: 'var(--s-xl)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="encouraging_approve" height={160} breathe />
          </div>
          <h1 class="t-headline">Submission sent for review!</h1>
          <p class="t-body-l muted">Thank you for helping preserve the Kasiguranin heritage. Your contribution is queued for verification by cultural language experts.</p>
          <ClayButton
            label="Submit another word"
            onClick={() => {
              setF({ ...f, kasiguranin: '', tagalog: '', english: '', rootForm: '', ipaNotation: '', exampleSentence: '', pastTense: '', presentTense: '', futureTense: '' });
              setDone(false);
            }}
          />
          <ClayButton label="Done" tone="quiet" onClick={() => back('/')} />
        </div>
      </GroundScaffold>
    );
  }

  const field = (label: string, key: keyof typeof f, placeholder = '', textarea = false) => (
    <label class="field">
      <span>{label}</span>
      {textarea ? (
        <textarea class="input" placeholder={placeholder} value={f[key]} onInput={set(key)} />
      ) : (
        <input class="input" placeholder={placeholder} value={f[key]} onInput={set(key)} autoCapitalize="none" />
      )}
    </label>
  );

  return (
    <GroundScaffold title="Help expand Kasiguranin" largeTitle subtitle="Share native words, phrases, or local expressions to be verified and published to KasiGuru.">
      {confirmDup && (
        <Dialog label="Duplicate word" onClose={() => setConfirmDup(false)}>
          <div class="stack">
            <h2 class="t-headline-s">{sameSense ? 'This entry already exists' : 'This word already exists'}</h2>
            <p class="t-body-l muted">
              {sameSense
                ? 'KasiGuru already carries this word with this meaning. Submitting it again gives the reviewers a duplicate to reject. Check the entry shown on the form first.'
                : 'KasiGuru already carries this word with a different meaning. If you are recording a separate sense of it, please continue — that is a word the dictionary is missing.'}
            </p>
            <ClayButton label="Keep editing" onClick={() => setConfirmDup(false)} />
            <button class="text-btn" style={{ color: 'var(--amber)' }} onClick={() => { setConfirmDup(false); void submit(true); }}>
              Submit anyway
            </button>
          </div>
        </Dialog>
      )}
      <form class="readable stack-lg" style={{ maxWidth: 640 }} onSubmit={(e) => { e.preventDefault(); void submit(); }}>
        <section class="stack">
          <h2 class="t-title-l">1. Core translations</h2>
          {field('Kasiguranin word *', 'kasiguranin', 'e.g. apak, singët, lukag')}
          {matches.length > 0 && (
            <div class={`banner ${needsConfirm ? 'warn' : ''}`}>
              <Icon name="danger" size={20} color="var(--amber)" />
              <div class="grow stack-sm">
                <p class="t-title-s">{needsConfirm ? 'Already in the dictionary' : 'Similar words in the dictionary'}</p>
                {matches.map((m) => (
                  <button key={m.w.id} type="button" class="row-xs t-body" style={{ textAlign: 'left' }} onClick={() => navigate(`/word/${encodeURIComponent(m.w.id)}`)}>
                    <b>{m.w.kasiguranin}</b>
                    <span class="muted">— {[m.w.tagalog, m.w.english].filter(Boolean).join(' · ')} ({m.w.category})</span>
                  </button>
                ))}
              </div>
            </div>
          )}
          {field('Tagalog translation', 'tagalog', 'e.g. daras, langgam, gising')}
          {field('English definition / translation', 'english', 'e.g. adze, ant, awake')}
          <p class="t-body-s faint">* Kasiguranin word is required, plus at least one of Tagalog or English so learners can find it.</p>
        </section>
        <section class="stack">
          <h2 class="t-title-l">2. Category & usage details</h2>
          <label class="field">
            <span>Category</span>
            <select class="input" value={f.category} onChange={set('category')}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
          <label class="field">
            <span>Part of speech</span>
            <select class="input" value={f.partOfSpeech} onChange={set('partOfSpeech')}>
              <option value="">Not sure</option>
              {PARTS_OF_SPEECH.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
          {field('Root word (optional)', 'rootForm', 'e.g. apak')}
          {field('Pronunciation / IPA (optional)', 'ipaNotation', 'e.g. ˈʔa.pak')}
          {field('Example sentence (optional)', 'exampleSentence', 'How is this word used in a sentence?', true)}
        </section>
        <section class="stack">
          <h2 class="t-title-l">3. Verb tenses (optional)</h2>
          {field('Past tense', 'pastTense', 'e.g. naglakaw')}
          {field('Present tense', 'presentTense', 'e.g. nagalakaw')}
          {field('Future tense', 'futureTense', 'e.g. magalakaw')}
        </section>
        <section class="stack">
          <h2 class="t-title-l">4. Contributor credit</h2>
          {field('Your name / credit', 'contributorName', 'Enter your name')}
        </section>
        {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
        <ClayButton label={busy ? 'Submitting…' : 'Submit entry for verification'} type="submit" disabled={busy} />
      </form>
    </GroundScaffold>
  );
}

