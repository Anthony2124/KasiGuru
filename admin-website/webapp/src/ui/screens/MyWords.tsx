/**
 * My words (ui/screens/library/MyWordsContent.kt, Android 1.22): every dictionary word met in a
 * lesson, a flashcard review or a game, most recent first, so a learner can find again the word
 * they saw yesterday and review it.
 */
import { useEffect, useMemo, useState } from 'preact/hooks';
import { today as todayIso } from '../../domain/dates';
import { UNKNOWN_TIME } from '../../domain/learner';
import { wordsFor } from '../../domain/lesson';
import { plural } from '../../domain/plural';
import type { Word } from '../../domain/types';
import { enc, navigate } from '../../lib/router';
import { act, useCorpus, useLearner } from '../../lib/store';
import { ClayButton, EmptyState } from '../kit';
import { AudioButton } from '../parts';

/** The filter pills, in order. "Needs practice" is due or lapsed words, the ones to review first. */
const FILTERS = ['All', 'Lessons', 'Reviews', 'Games', 'Needs practice'] as const;
type Filter = (typeof FILTERS)[number];

/** MyWordsViewModel.sourceName: the name a source is shown with, matching Practice's game names. */
const SOURCE_NAMES: Record<string, string> = {
  lesson: 'Lesson', review: 'Flashcards', word_match: 'Word Match', reverse_match: 'Reverse Match',
  fill_blank: 'Fill in the Blank', recall: 'Word Recall', aspect_builder: 'Aspect Builder',
  sentence_order: 'Sentence Construction', word_search: 'Word Search', word_wheel: 'Word Wheel',
};
const metIn = (source: string): Filter => (source === 'lesson' ? 'Lessons' : source === 'review' ? 'Reviews' : 'Games');

interface MetWord {
  word: Word;
  lastSeenAt: number;
  source: string;
  isDue: boolean;
}

/** When a word was last met: "just now", "5 min ago", "3 h ago", "yesterday", "4 days ago", a date. */
function whenMet(at: number, now = Date.now()): string {
  if (at <= UNKNOWN_TIME) return 'earlier';
  const min = Math.floor((now - at) / 60_000);
  if (min < 1) return 'just now';
  if (min < 60) return `${min} min ago`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h} h ago`;
  const days = Math.floor(h / 24);
  if (days === 1) return 'yesterday';
  if (days < 7) return `${days} days ago`;
  return new Date(at).toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
}

export function MyWordsTab() {
  const learner = useLearner();
  const corpus = useCorpus();
  const [filter, setFilter] = useState<Filter>('All');
  const encounters = learner.encounters ?? {};

  // fillFromHistory: words met before encounters were recorded. Adds only what is missing, so it is
  // safe on every open, and it brings the list back after a sign-in restores progress.
  useEffect(() => {
    const reviewed = corpus.all.filter((w) => (w.timesReviewed > 0 || w.isLearned) && !encounters[w.id]).map((w) => w.id);
    const finished = Object.entries(learner.lessons)
      .filter(([, l]) => l.isComplete)
      .map(([key, l]) => {
        const at = key.lastIndexOf('#');
        const ids = wordsFor(corpus, { unitId: key.slice(0, at), lessonIndex: Number(key.slice(at + 1)) }).map((w) => w.id);
        return { at: l.lastCompletedAt, ids: ids.filter((id) => !encounters[id]) };
      })
      .filter((l) => l.ids.length);
    if (reviewed.length || finished.length) act((d) => d.fillMet(reviewed, finished));
  }, [corpus.all.length]);

  const words = useMemo<MetWord[]>(() => {
    const today = todayIso();
    return Object.entries(encounters)
      .map(([id, e]) => {
        const word = corpus.byId(id);
        if (!word) return null;
        const isDue = word.timesReviewed > 0 && word.nextReviewDate !== '' && word.nextReviewDate <= today;
        return { word, lastSeenAt: e.lastSeenAt, source: e.lastSource, isDue };
      })
      .filter((m): m is MetWord => !!m)
      .sort((a, b) => b.lastSeenAt - a.lastSeenAt);
  }, [encounters, corpus]);

  if (!words.length) {
    return (
      <EmptyState
        title="No words yet"
        message="Every word you meet in a lesson, a game or a flashcard review will collect here, so you can find it again and review it."
        actionLabel="Review words"
        onAction={() => navigate('/review')}
      />
    );
  }

  const due = words.filter((m) => m.isDue).length;
  const learned = words.filter((m) => m.word.isLearned).length;
  const shown = words.filter((m) =>
    filter === 'All' ? true : filter === 'Needs practice' ? m.isDue || m.word.lapses > 0 : metIn(m.source) === filter
  );

  return (
    <div class="stack">
      <section class="card panel stack-sm">
        <p class="t-title-l">{plural(words.length, 'word')} met</p>
        <p class="t-body muted">{learned} learned · {due} due for review</p>
        <ClayButton label={due > 0 ? `Review ${due} due` : 'Practise these words'} icon="repeat" onClick={() => navigate('/review')} />
      </section>

      <div class="row-xs wrap" role="group" aria-label="Show words met in">
        {FILTERS.map((f) => (
          <button key={f} class={`chip${filter === f ? ' on' : ''}`} aria-pressed={filter === f} onClick={() => setFilter(f)}>
            {f}
          </button>
        ))}
      </div>

      {shown.length ? (
        <div class="list cols-2">
          {shown.map((m) => (
            <div key={m.word.id} class="list-row">
              <button class="grow" style={{ textAlign: 'left', minWidth: 0 }} onClick={() => navigate(`/word/${enc(m.word.id)}`)}>
                <p class="t-title row-xs">
                  {m.word.kasiguranin}
                  {m.isDue && <span class="t-label-s" style={{ color: 'var(--lime)' }}>Due</span>}
                </p>
                <p class="t-body-s muted ellipsis">{[m.word.tagalog, m.word.english].filter(Boolean).join(' · ')}</p>
                <p class="t-label-s faint">{SOURCE_NAMES[m.source] ?? 'Practice'} · {whenMet(m.lastSeenAt)}</p>
              </button>
              <AudioButton word={m.word} label={`Listen to ${m.word.kasiguranin}`} />
            </div>
          ))}
        </div>
      ) : (
        <p class="t-body muted">
          {filter === 'Needs practice' ? 'Nothing needs practice right now. Well done.' : `No words met in ${filter.toLowerCase()} yet.`}
        </p>
      )}
    </div>
  );
}
