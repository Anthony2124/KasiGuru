/**
 * The lesson player (ui/screens/lesson): a run of exercises built from the lesson's words, each
 * answered and checked, a miss coming back a few items later at a widening gap, and one SM-2 rating
 * per word per lesson. Finishing records the lesson and pays 20 XP (25 when perfect); a same-day
 * repeat pays nothing more, a repeat on a later day 5.
 */
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import {
  buildExercises,
  choosePrompt,
  contrastLine,
  exerciseInstruction,
  meaningOf,
  practiceWordsFor,
  rehearsalInsertIndex,
  sectionForUnit,
  type Exercise,
} from '../../domain/lesson';
import { matchRecall } from '../../domain/recall';
import { ratingForAnswer, ReviewRating } from '../../domain/sm2';
import { shuffled } from '../../domain/random';
import type { Word } from '../../domain/types';
import { answerHaptic, feedbackTone, playWord, prefetch, stopAudio, unlockAudio } from '../../lib/audio';
import { today } from '../../domain/dates';
import { back, navigate } from '../../lib/router';
import { act, getCorpus, getState, setPrefs, useApp } from '../../lib/store';
import { ClayButton, ConfirmDialog, CountUp, Icon, Jepjep, Loading, ProgressBar, sceneForSection, sceneUrl } from '../kit';

interface Run {
  queue: { ex: Exercise; id: number }[];
  words: Word[];
  total: number;
}

export function LessonScreen({ unitId, lessonIndex }: { unitId: string; lessonIndex: number }) {
  const contentReady = useApp((s) => s.contentReady);
  const [run, setRun] = useState<Run | null>(null);
  const [position, setPosition] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null);
  const [remediation, setRemediation] = useState<string | null>(null);
  const [solved, setSolved] = useState(0);
  const [confirmExit, setConfirmExit] = useState(false);
  const [done, setDone] = useState<{ xp: number; accuracy: number; showStreak: boolean } | null>(null);
  const [combo, setCombo] = useState(0);
  const missedFirst = useRef(new Set<number>());
  const missCounts = useRef(new Map<number, number>());
  const solvedIds = useRef(new Set<number>());
  const rated = useRef(new Set<string>());
  const shownAt = useRef(Date.now());
  const soundOn = useApp((s) => s.prefs.soundEnabled);
  const hapticsOn = useApp((s) => s.prefs.hapticsEnabled);

  useEffect(() => {
    if (!contentReady) return;
    const corpus = getCorpus();
    const ref = { unitId, lessonIndex };
    const exercises = buildExercises(corpus, practiceWordsFor(corpus, ref));
    setRun({ queue: exercises.map((ex, id) => ({ ex, id })), words: practiceWordsFor(corpus, ref), total: exercises.length });
    shownAt.current = Date.now();
    for (const e of exercises) if (e.type === 'listen') prefetch(e.word);
    document.title = `${sectionForUnit(unitId)?.title ?? 'Lesson'} · KasiGuru`;
    return () => stopAudio();
  }, [contentReady, unitId, lessonIndex]);

  if (!run) return <Loading label="Preparing your lesson" />;

  if (done) return <LessonComplete xp={done.xp} accuracy={done.accuracy} words={run.words} showStreak={done.showStreak} />;

  if (!run.total) {
    return (
      <main class="page no-nav center stack" style={{ paddingTop: 80 }}>
        <Jepjep pose="confused" height={140} />
        <h1 class="t-headline-s">This lesson has no words yet</h1>
        <ClayButton label="Back to the path" onClick={() => navigate('/learn', { replace: true })} />
      </main>
    );
  }

  const item = run.queue[position];
  const ex = item.ex;
  const answered = isCorrect !== null;

  const check = () => {
    if (selected == null || answered) return;
    unlockAudio();
    const match = ex.type === 'type' ? matchRecall(selected, ex.answer) : null;
    const correct = ex.type === 'type' ? match !== 'Wrong' : selected === ex.answer;
    if (!correct) missedFirst.current.add(item.id);
    setIsCorrect(correct);
    setCombo((c) => (correct ? c + 1 : 0));
    if (soundOn) feedbackTone(correct);
    if (hapticsOn) answerHaptic(correct);
    if (!correct) {
      const corpus = getCorpus();
      setRemediation(contrastLine(selected, corpus.findByWrittenForm(selected), ex.word));
    }
    // One rating per word per lesson, from the first time it is answered.
    if (!rated.current.has(ex.word.id)) {
      rated.current.add(ex.word.id);
      const rating = match === 'Close' ? ReviewRating.HARD : ratingForAnswer(correct, Date.now() - shownAt.current);
      const word = getCorpus().byId(ex.word.id) ?? ex.word;
      act((d) => d.reviewWord(word, rating));
    }
  };

  const advance = () => {
    if (isCorrect === null) return;
    const queue = run.queue.slice();
    if (isCorrect) solvedIds.current.add(item.id);
    else {
      const misses = (missCounts.current.get(item.id) ?? 0) + 1;
      missCounts.current.set(item.id, misses);
      queue.splice(rehearsalInsertIndex(position, misses, queue.length), 0, item);
    }
    const next = position + 1;
    if (next >= queue.length) {
      const total = Math.max(1, run.total);
      const accuracy = Math.min(1, Math.max(0, (total - missedFirst.current.size) / total));
      const xp = act((d) => d.completeLesson({ unitId, lessonIndex }, accuracy));
      // UserPreferencesRepository.markFirstLessonOfDay: the streak page follows the day's first lesson.
      const showStreak = getState().prefs.lastLessonDate !== today();
      setPrefs({ lastLessonDate: today() });
      setDone({ xp, accuracy, showStreak });
      return;
    }
    setRun({ ...run, queue });
    setPosition(next);
    setSelected(null);
    setIsCorrect(null);
    setRemediation(null);
    setSolved(solvedIds.current.size);
    shownAt.current = Date.now();
  };

  return (
    <div class="lesson">
      {confirmExit && (
        <ConfirmDialog
          title="Leave this lesson?"
          message="Your progress in this lesson won't be saved."
          confirmLabel="Leave"
          danger
          onCancel={() => setConfirmExit(false)}
          onConfirm={() => {
            setConfirmExit(false);
            back('/learn');
          }}
        />
      )}
      <header class="row lesson-head">
        <button class="icon-btn" aria-label="Leave lesson" onClick={() => setConfirmExit(true)}>
          <Icon name="arrowLeft" size={24} />
        </button>
        <div class="grow">
          <ProgressBar value={run.total ? solved / run.total : 0} height={12} label="Lesson progress" />
        </div>
      </header>
      <div class="lesson-scene">
        <img src={sceneUrl(sceneForSection(sectionForUnit(unitId)?.id ?? ''))} alt="" />
        <div class="row" style={{ position: 'relative', height: '100%', padding: '0 var(--gutter)' }}>
          {!answered && <Jepjep pose="pointing_a_lesson" height={60} decorative />}
          <p class="t-body-s grow" style={{ color: 'var(--ink)' }}>Take your time. You're learning.</p>
          {combo >= 3 && <span class="tag" style={{ background: 'var(--olive)', color: 'var(--ink)' }}>{combo} in a row</span>}
        </div>
      </div>

      <div class="lesson-body">
        <div class="lesson-prompt" key={`${item.id}-${position}`}>
          <p class="t-title-l muted">{exerciseInstruction(ex)}</p>
          <Prompt ex={ex} />
        </div>
        <div class="lesson-answers">
          <Answers ex={ex} selected={selected} answered={answered} onSelect={setSelected} onSubmit={check} key={`${item.id}-${position}-a`} />
        </div>
      </div>

      <footer class="lesson-action">
        {!answered ? (
          <ClayButton label="Check" onClick={check} disabled={!selected || !selected.trim()} />
        ) : (
          <Feedback correct={!!isCorrect} ex={ex} remediation={remediation} typed={ex.type === 'type' ? selected ?? '' : null} onContinue={advance} />
        )}
      </footer>
    </div>
  );
}

function Prompt({ ex }: { ex: Exercise }) {
  const [playing, setPlaying] = useState(false);
  // A clip that cannot load (offline and never heard, or missing upstream) would leave a listening
  // exercise with nothing to answer from, so the meaning stands in for the sound.
  const [failed, setFailed] = useState(false);
  const play = async () => {
    setPlaying(true);
    const ok = await playWord(ex.word);
    if (!ok) setFailed(true);
    setTimeout(() => setPlaying(false), 600);
  };
  useEffect(() => {
    if (ex.type === 'listen') void play();
  }, [ex]);

  switch (ex.type) {
    case 'type':
      return <p class="headword" style={{ fontSize: 30, lineHeight: '36px' }}>{ex.promptMeaning}</p>;
    case 'choose':
      return (
        <div>
          <p class="headword">{choosePrompt(ex)}</p>
          {ex.promptIsKasiguranin && ex.word.ipaNotation && <p class="t-body-l faint">[{ex.word.ipaNotation}]</p>}
        </div>
      );
    case 'listen':
      return (
        <div style={{ display: 'grid', placeItems: 'center', padding: 'var(--s-md) 0' }}>
          <button class="clay" style={{ width: 92, height: 92, borderRadius: '50%', padding: 0 }} aria-label="Play the word" onClick={play}>
            <Icon name="volumeHigh" size={38} class={playing ? 'pop' : ''} />
          </button>
          {failed && (
            <p class="t-body-l muted center" style={{ marginTop: 'var(--s-md)' }} role="status">
              The recording couldn't play. Pick the word that means <b style={{ color: 'var(--ink)' }}>{meaningOf(ex.word)}</b>.
            </p>
          )}
        </div>
      );
    case 'fill':
      return (
        <div>
          <p class="t-headline-m">{ex.sentenceWithBlank}</p>
          {ex.translation && <p class="t-body-l muted" style={{ marginTop: 8 }}>{ex.translation}</p>}
        </div>
      );
    case 'sentence':
      return <p class="t-headline-m">{ex.translation}</p>;
    case 'match':
      return null;
    case 'aspect':
      return (
        <div>
          <p class="headword">{ex.word.kasiguranin}</p>
          <div class="row-xs" style={{ marginTop: 8 }}>
            <span class="tag">verb</span>
            <span class="tag">{ex.aspectLabel}</span>
          </div>
        </div>
      );
  }
}

function Answers({ ex, selected, answered, onSelect, onSubmit }: { ex: Exercise; selected: string | null; answered: boolean; onSelect: (s: string) => void; onSubmit: () => void }) {
  if (ex.type === 'type') {
    return (
      <input
        class="input"
        style={{ fontSize: 20, textAlign: 'center', borderRadius: 'var(--r-pill)' }}
        placeholder="Type the Kasiguranin word"
        autoCapitalize="none"
        autoCorrect="off"
        autoComplete="off"
        spellcheck={false}
        enterKeyHint="done"
        aria-label="Your answer"
        disabled={answered}
        value={selected ?? ''}
        autoFocus
        onInput={(e) => onSelect((e.target as HTMLInputElement).value)}
        onKeyDown={(e) => e.key === 'Enter' && onSubmit()}
      />
    );
  }
  if (ex.type === 'sentence') return <SentenceBuilder ex={ex} enabled={!answered} onChange={onSelect} />;
  if (ex.type === 'match') return <MatchBoard ex={ex} enabled={!answered} onSolved={() => onSelect(ex.answer)} />;
  // Short options sit two to a row where there is room (a computer; see .options-2).
  const short = ex.options.length >= 2 && ex.options.length <= 4 && ex.options.every((o) => o.length <= 24);
  return (
    <div class={short ? 'stack-sm options-2' : 'stack-sm'} role="radiogroup">
      {ex.options.map((o) => {
        const isSel = selected === o;
        const cls = answered ? (o === ex.answer ? 'correct' : isSel ? 'wrong' : 'dim') : isSel ? 'selected' : '';
        return (
          <button key={o} class={`option ${cls}`} role="radio" aria-checked={isSel} disabled={answered} onClick={() => onSelect(o)}>
            <span class="label">{o}</span>
            {answered && o === ex.answer && <Icon name="tickCircle" size={20} class="mark" label="Correct answer" />}
            {answered && isSel && o !== ex.answer && <Icon name="closeCircle" size={20} class="mark" label="Your answer" />}
          </button>
        );
      })}
    </div>
  );
}

function SentenceBuilder({ ex, enabled, onChange }: { ex: Extract<Exercise, { type: 'sentence' }>; enabled: boolean; onChange: (s: string) => void }) {
  const [placed, setPlaced] = useState<number[]>([]);
  useEffect(() => onChange(placed.map((i) => ex.options[i]).join(' ')), [placed]);
  return (
    <div class="stack">
      <div class="card sunken" style={{ minHeight: 64, display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
        {placed.length === 0 ? (
          <span class="t-body muted">Tap the words below, in order</span>
        ) : (
          placed.map((i) => (
            <button key={i} class="word-chip placed" disabled={!enabled} onClick={() => setPlaced(placed.filter((p) => p !== i))}>
              {ex.options[i]}
            </button>
          ))
        )}
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {ex.options.map((o, i) => (
          <button key={i} class="word-chip" style={{ visibility: placed.includes(i) ? 'hidden' : 'visible' }} disabled={!enabled || placed.includes(i)} onClick={() => setPlaced([...placed, i])}>
            {o}
          </button>
        ))}
      </div>
    </div>
  );
}

function MatchBoard({ ex, enabled, onSolved }: { ex: Extract<Exercise, { type: 'match' }>; enabled: boolean; onSolved: () => void }) {
  const words = useMemo(() => shuffled(ex.pairs.map((p) => p[0])), [ex]);
  const meanings = useMemo(() => shuffled(ex.pairs.map((p) => p[1])), [ex]);
  const [matched, setMatched] = useState<string[]>([]);
  const [picked, setPicked] = useState<string | null>(null);
  const [wrong, setWrong] = useState<string | null>(null);
  useEffect(() => {
    if (matched.length === ex.pairs.length) onSolved();
  }, [matched.length]);
  useEffect(() => {
    if (!wrong) return;
    const t = setTimeout(() => {
      setWrong(null);
      setPicked(null);
    }, 600);
    return () => clearTimeout(t);
  }, [wrong]);
  const owner = (meaning: string) => ex.pairs.find((p) => p[1] === meaning)![0].kasiguranin;
  return (
    <div class="grid-2">
      <div class="stack-sm">
        {words.map((w) => {
          const isMatched = matched.includes(w.kasiguranin);
          const isSel = picked === w.kasiguranin;
          return (
            <button key={w.id} class={`match-tile${isMatched ? ' matched' : isSel ? ' picked' : ''}`} disabled={!enabled || isMatched} onClick={() => setPicked(w.kasiguranin)} aria-label={`${w.kasiguranin}${isMatched ? ', matched' : isSel ? ', selected' : ''}`}>
              {isMatched && <Icon name="tickCircle" size={16} color="var(--green)" />}
              <span>{w.kasiguranin}</span>
            </button>
          );
        })}
      </div>
      <div class="stack-sm">
        {meanings.map((m) => {
          const o = owner(m);
          const isMatched = matched.includes(o);
          return (
            <button
              key={m}
              class={`match-tile${isMatched ? ' matched' : ''}${wrong === m ? ' wrong shake' : ''}`}
              disabled={!enabled || isMatched || !picked}
              onClick={() => {
                if (!picked) return;
                if (picked === o) {
                  setMatched([...matched, picked]);
                  setPicked(null);
                } else setWrong(m);
              }}
              aria-label={`${m}${isMatched ? ', matched' : ''}`}
            >
              {isMatched && <Icon name="tickCircle" size={16} color="var(--green)" />}
              <span>{m}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function Feedback({ correct, ex, remediation, typed, onContinue }: { correct: boolean; ex: Exercise; remediation: string | null; typed: string | null; onContinue: () => void }) {
  const close = typed != null && correct && matchRecall(typed, ex.answer) === 'Close';
  const headline = close ? 'Almost — check the spelling' : correct ? 'Correct' : 'Not quite';
  const word = ex.word;
  return (
    <div class={`feedback ${correct ? 'ok' : 'bad'}`} role="status">
      <div class="row" style={{ justifyContent: 'space-between' }}>
        <p class="row-xs t-title-l" style={{ color: correct ? 'var(--green)' : 'var(--red)' }}>
          <Icon name={correct ? 'tickCircle' : 'infoCircle'} size={26} />
          {headline}
        </p>
        {word.audioFileName && (
          <button class="icon-btn boxed" style={{ width: 40, height: 40, color: 'var(--info)' }} aria-label={`Play ${word.kasiguranin}`} onClick={() => void playWord(word)}>
            <Icon name="volumeHigh" size={20} />
          </button>
        )}
      </div>
      {remediation && <p class="t-body-l" style={{ marginTop: 8 }}>{remediation}</p>}
      {(!correct || close) && ex.type !== 'match' && <p class="t-body-l" style={{ marginTop: 8 }}>Answer: <b>{ex.answer}</b></p>}
      {word.exampleSentence && (
        <div style={{ marginTop: 8 }}>
          <p class="t-body-l">{word.exampleSentence}</p>
          {word.exampleTranslation && <p class="t-body muted">{word.exampleTranslation}</p>}
        </div>
      )}
      <div style={{ marginTop: 'var(--s-md)' }}>
        <ClayButton label={correct ? 'Continue' : 'Got it'} tone={correct ? 'primary' : 'reward'} onClick={onContinue} />
      </div>
    </div>
  );
}

function LessonComplete({ xp, accuracy, words, showStreak }: { xp: number; accuracy: number; words: Word[]; showStreak: boolean }) {
  const perfect = accuracy >= 1;
  return (
    <main class="page no-nav glow" style={{ '--gx': '50%', '--gy': '20%', minHeight: '100dvh', paddingTop: 'calc(var(--safe-top) + var(--s-xl))' } as never}>
      <div class="readable stack center">
        <div style={{ display: 'grid', placeItems: 'center' }}>
          <Jepjep pose={perfect ? 'shocked' : 'celebrating'} height={170} breathe />
        </div>
        <h1 class="t-display">{perfect ? 'Perfect lesson' : 'Lesson complete'}</h1>
        <p class="t-body muted">{perfect ? 'Every answer right on the first try.' : `${Math.round(accuracy * 100)}% correct on your first attempt.`}</p>
        <div class="grid-2">
          <div class="card">
            <p class="t-headline-s" style={{ color: 'var(--gold)' }}>
              +<CountUp to={xp} />
            </p>
            <p class="t-label-s muted">XP earned</p>
          </div>
          <div class="card">
            <p class="t-headline-s">{words.length}</p>
            <p class="t-label-s muted">{words.length === 1 ? 'word covered' : 'words covered'}</p>
          </div>
        </div>
        {words.length > 0 && (
          <div style={{ textAlign: 'left' }}>
            <h2 class="t-title-l" style={{ marginBottom: 8 }}>In this lesson</h2>
            <div class="list">
              {words.map((w) => (
                <div key={w.id} class="list-row">
                  <p class="t-title grow">{w.kasiguranin}</p>
                  <p class="t-body muted" style={{ textAlign: 'right' }}>{w.tagalog || w.english}</p>
                </div>
              ))}
            </div>
          </div>
        )}
        <ClayButton label="Continue" onClick={() => (showStreak ? navigate('/streak', { replace: true }) : back('/learn'))} />
      </div>
    </main>
  );
}
