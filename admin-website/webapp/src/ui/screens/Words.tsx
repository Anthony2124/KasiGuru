/**
 * A dictionary entry (VocabularyDetailScreen) and one category's words (CategoryDetailScreen).
 */
import { useMemo, useState } from 'preact/hooks';
import { navigate } from '../../lib/router';
import { act, useApp, useCorpus } from '../../lib/store';
import { submitSentence } from '../../lib/remote';
import { exampleSentenceProblem } from '../../domain/exampleSentence';
import type { Word } from '../../domain/types';
import { ClayButton, EmptyState, GroundScaffold, Icon, ProgressBar, categoryArt, toast } from '../kit';
import { AudioButton, WordRow } from '../parts';
import { categoryBlurb } from './Library';

function DetailRow({ label, value }: { label: string; value: string }) {
  if (!value) return null;
  return (
    <div class="list-row" style={{ minHeight: 48 }}>
      <span class="t-body-s muted grow">{label}</span>
      <span class="t-body" style={{ fontWeight: 600, textAlign: 'right' }}>{value}</span>
    </div>
  );
}

/**
 * Offers an example sentence for a word with none (VocabularyDetailScreen's AddExampleSentence). It
 * goes to the verifiers' queue, not the dictionary: Kasiguranin taught in lessons must have been
 * checked by someone who speaks it.
 */
function AddExampleSentence({ word }: { word: Word }) {
  const progress = useApp((s) => s.learner.progress);
  const [open, setOpen] = useState(false);
  const [sentence, setSentence] = useState('');
  const [translation, setTranslation] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const send = async (e: Event) => {
    e.preventDefault();
    const p = exampleSentenceProblem(word, sentence, translation);
    if (p) return setProblem(p);
    if (!navigator.onLine) return setProblem("You're offline. Connect to send your sentence.");
    setSending(true);
    try {
      await submitSentence({
        kasiguranin: word.kasiguranin,
        english: word.english,
        sentence,
        translation,
        contributorName: progress.fullName || (progress.userName !== 'Learner' ? progress.userName : '') || 'Anonymous',
      });
      act((d) => d.incrementSubmissionsMade());
      setSent(true);
    } catch {
      setProblem("Couldn't send it. Check your connection and try again.");
    } finally {
      setSending(false);
    }
  };

  if (sent) {
    return (
      <section class="card stack-sm">
        <h2 class="t-title-l">Thank you!</h2>
        <p class="t-body muted">A verifier will check your sentence. Once it's approved it appears here, in lessons and in the sentence games.</p>
      </section>
    );
  }
  if (!open) {
    return (
      <section class="card stack-sm">
        <h2 class="t-title-l">No example sentence yet</h2>
        <p class="t-body muted">Know how “{word.kasiguranin}” is used? Add a sentence to help learners and the sentence games.</p>
        <ClayButton label="Add an example sentence" tone="quiet" icon="addCircle" onClick={() => setOpen(true)} />
      </section>
    );
  }
  return (
    <form class="card stack" onSubmit={send} noValidate>
      <h2 class="t-title-l">Add an example sentence</h2>
      <label class="field">
        <span>Sentence in Kasiguranin, using “{word.kasiguranin}”</span>
        <textarea class="input" rows={2} value={sentence} maxLength={320} onInput={(e) => { setSentence((e.target as HTMLTextAreaElement).value); setProblem(null); }} />
      </label>
      <label class="field">
        <span>What it means in English</span>
        <textarea class="input" rows={2} value={translation} maxLength={320} onInput={(e) => { setTranslation((e.target as HTMLTextAreaElement).value); setProblem(null); }} />
      </label>
      {problem && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{problem}</p>}
      <div class="row">
        <button type="button" class="text-btn muted" onClick={() => { setOpen(false); setProblem(null); }}>Cancel</button>
        <span class="grow" />
        <ClayButton label={sending ? 'Sending…' : 'Send for review'} type="submit" disabled={sending} block={false} />
      </div>
    </form>
  );
}

export function WordDetailScreen({ id }: { id: string }) {
  const corpus = useCorpus();
  const word = corpus.byId(id);
  if (!word) {
    return (
      <GroundScaffold title="Word">
        <EmptyState pose="confused" title="This word couldn't be found." message="It may have been changed or removed in the dictionary." />
      </GroundScaffold>
    );
  }
  const aspects = [
    ['Neutral', word.neutralForm],
    ['Imperfective (Present)', word.imperfectiveForm],
    ['Perfective (Past)', word.perfectiveForm],
    ['Contemplative (Future)', word.contemplativeForm],
  ].filter(([, v]) => v);
  const toggleLearned = () => {
    if (word.isLearned) {
      act((d) => d.unmarkAsLearned(word));
      toast('Marked as not learned yet');
    } else {
      act((d) => d.markAsLearned(word));
      toast(`${word.kasiguranin} marked as learned`);
    }
  };
  return (
    <GroundScaffold title={word.kasiguranin}>
      <div class="stack readable" style={{ maxWidth: 640 }}>
        <section class="card panel glow" style={{ '--gx': '80%', '--gy': '20%', padding: 'var(--s-lg)' } as never}>
          <div class="row" style={{ alignItems: 'flex-start' }}>
            <div class="grow">
              <h1 class="headword">{word.kasiguranin}</h1>
              <p class="t-body-l muted">{[word.english, word.tagalog].filter(Boolean).join(' • ')}</p>
              {word.ipaNotation && <p class="t-body faint" style={{ marginTop: 4 }}>[{word.ipaNotation}]</p>}
            </div>
            <AudioButton word={word} size={52} />
          </div>
          <div class="row-xs wrap" style={{ marginTop: 'var(--s-md)' }}>
            {word.partOfSpeech && <span class="tag neutral">{word.partOfSpeech}</span>}
            <span class="tag neutral">{word.category}</span>
            {word.phoneticGlottal && <span class="tag info">Glottal stop ʔ</span>}
            {word.phoneticVowelLength && <span class="tag info">Long vowel ː</span>}
          </div>
          <button class={`clay small ${word.isLearned ? 'quiet' : ''}`} style={{ marginTop: 'var(--s-md)' }} onClick={toggleLearned} aria-pressed={word.isLearned}>
            <Icon name="tickCircle" size={18} />
            {word.isLearned ? 'Learned' : 'Mark learned'}
          </button>
        </section>

        {(word.meaningEnglish || word.meaningTagalog) && (
          <section class="card stack-sm">
            <h2 class="t-title-l">Meaning</h2>
            {word.meaningEnglish && <p class="t-body-l">{word.meaningEnglish}</p>}
            {word.meaningTagalog && <p class="t-body-l muted">{word.meaningTagalog}</p>}
          </section>
        )}

        {aspects.length > 0 && (
          <section>
            <h2 class="t-title-l" style={{ marginBottom: 8 }}>Verb aspect inflections</h2>
            <div class="list">
              {aspects.map(([l, v]) => (
                <DetailRow key={l} label={l} value={v} />
              ))}
            </div>
          </section>
        )}

        {word.rootForm && word.rootForm.toLowerCase() !== word.kasiguranin.toLowerCase() && (
          <div class="list">
            <DetailRow label="Root form" value={word.rootForm} />
          </div>
        )}

        {(word.exampleSentence || word.exampleSentence2) && (
          <section class="card stack-sm">
            <h2 class="t-title-l">Example sentence</h2>
            {word.exampleSentence && (
              <div>
                <p class="t-body-l">“{word.exampleSentence}”</p>
                {word.exampleTranslation && <p class="t-body muted">{word.exampleTranslation}</p>}
              </div>
            )}
            {word.exampleSentence2 && (
              <div>
                <p class="t-body-l">“{word.exampleSentence2}”</p>
                {word.exampleTranslation2 && <p class="t-body muted">{word.exampleTranslation2}</p>}
              </div>
            )}
          </section>
        )}

        {!word.exampleSentence && !word.exampleSentence2 && <AddExampleSentence word={word} />}

        {word.timesReviewed > 0 && (
          <p class="t-body-s faint">
            Reviewed {word.timesReviewed} {word.timesReviewed === 1 ? 'time' : 'times'}
            {word.nextReviewDate ? ` · next review ${word.nextReviewDate}` : ''}
          </p>
        )}

        <button class="text-btn muted row-xs" style={{ fontFamily: 'var(--body)', fontSize: 14 }} onClick={() => navigate(`/report?category=${encodeURIComponent('Wrong Word / Translation')}&word=${encodeURIComponent(word.kasiguranin)}&screen=${encodeURIComponent('Word detail')}`)}>
          <Icon name="danger" size={18} /> Report an issue with this word
        </button>
      </div>
    </GroundScaffold>
  );
}

export function CategoryScreen({ category }: { category: string }) {
  const corpus = useCorpus();
  const words = useMemo(() => corpus.inCategory(category), [corpus, category]);
  const [filter, setFilter] = useState<'all' | 'learning' | 'learned'>('all');
  const learned = words.filter((w) => w.isLearned).length;
  const shown = words.filter((w) => (filter === 'all' ? true : filter === 'learned' ? w.isLearned : !w.isLearned));
  return (
    <GroundScaffold title={category} wide>
      <div class="stack">
        {/* CategoryDetailScreen: the category's illustration, the same one its Library tile carries. */}
        <div class="stack-sm">
          <img src={categoryArt(category)} alt="" width={88} height={88} decoding="async" />
          <h1 class="t-headline-s">{category}</h1>
          <p class="t-body muted">{categoryBlurb(category)}</p>
        </div>
        <div class="row">
          <div class="grow">
            <ProgressBar value={words.length ? learned / words.length : 0} label="Words learned" />
          </div>
          <span class="t-label muted">{learned} / {words.length} learned</span>
        </div>
        <div class="segmented" role="tablist">
          {(['all', 'learning', 'learned'] as const).map((f) => (
            <button key={f} role="tab" aria-selected={filter === f} onClick={() => setFilter(f)}>
              {f === 'all' ? 'All' : f === 'learning' ? 'Learning' : 'Learned'}
            </button>
          ))}
        </div>
        {shown.length ? (
          <div class="list cols-2">
            {shown.map((w) => (
              <WordRow key={w.id} word={w} />
            ))}
          </div>
        ) : (
          <p class="t-body muted center" style={{ padding: 'var(--s-lg)' }}>{filter === 'learned' ? 'No words learned here yet.' : 'Every word here is learned.'}</p>
        )}
      </div>
    </GroundScaffold>
  );
}
