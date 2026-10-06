/**
 * Word Match (WordMatchViewModel): a Kasiguranin word, four Tagalog meanings. Rounds draw due words
 * first and every answer is an SM-2 review. A cleared round earns 5 XP + 2 per correct answer, and an
 * unassisted perfect round 5 more (XP policy 2).
 */
import { useEffect, useRef, useState } from 'preact/hooks';
import { GAMES } from '../../domain/constants';
import { questionsForLevel, starsFor } from '../../domain/gamification';
import { shuffled } from '../../domain/random';
import { meaningOf } from '../../domain/lesson';
import { ratingForAnswer } from '../../domain/sm2';
import type { Word } from '../../domain/types';
import { feedbackTone, playWord } from '../../lib/audio';
import { navigate } from '../../lib/router';
import { act, getCorpus, getState, useApp } from '../../lib/store';
import { ClayButton, Icon, Loading, sceneForIndex } from '../kit';
import { GameFrame, GameOver, GameUnavailable, HintButton, type ReviewItem } from './shell';

const answerOf = (w: Word) => meaningOf(w);

interface Question {
  word: Word;
  options: string[];
}

export function WordMatchGame({ level }: { level: number }) {
  const ready = useApp((s) => s.contentReady);
  const soundOn = useApp((s) => s.prefs.soundEnabled);
  const [round, setRound] = useState(0);
  const [questions, setQuestions] = useState<Question[] | null>(null);
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [hint, setHint] = useState(false);
  const [score, setScore] = useState(0);
  const [result, setResult] = useState<{ xp: number; stars: number; next: number | null } | null>(null);
  const usedHint = useRef(false);
  const review = useRef<ReviewItem[]>([]);
  const shownAt = useRef(Date.now());
  const total = questionsForLevel(level);

  useEffect(() => {
    if (!ready) return;
    const corpus = getCorpus();
    let words = corpus.practiceWords(total);
    if (!words.length) words = corpus.fresh(total);
    setQuestions(
      words.map((w) => ({
        word: w,
        // The meaning as lessons print it: a Tagalog gloss that only repeats the headword would
        // hand the answer over, so such a word is offered by its English meaning instead.
        options: shuffled([...new Set([...corpus.distractorsFor(w, 3).map(meaningOf).filter(Boolean), meaningOf(w)])]),
      }))
    );
    setIndex(0);
    setSelected(null);
    setHint(false);
    setScore(0);
    setResult(null);
    usedHint.current = false;
    review.current = [];
    shownAt.current = Date.now();
  }, [ready, level, round]);

  if (!questions) return <Loading label="Setting up the round" />;
  if (!questions.length) return <GameUnavailable message="The dictionary is still loading. Try again in a moment." />;

  if (result) {
    return (
      <GameOver
        score={score}
        total={total}
        xp={result.xp}
        stars={result.stars}
        nextLevel={result.next}
        review={review.current}
        onNext={() => navigate(`/games/word_match/${result.next}`, { replace: true })}
        onReplay={() => setRound((r) => r + 1)}
      />
    );
  }

  const q = questions[index];
  const answered = selected !== null;
  const correct = answered && selected === answerOf(q.word);
  const hintText = q.word.meaningEnglish.trim() || null;

  const choose = (option: string) => {
    if (answered) return;
    const isCorrect = option === answerOf(q.word);
    const rating = ratingForAnswer(isCorrect, Date.now() - shownAt.current, hint);
    if (isCorrect) setScore((s) => s + 1);
    review.current.push({ prompt: q.word.kasiguranin, subPrompt: q.word.english || undefined, userAnswer: option, correctAnswer: answerOf(q.word), isCorrect });
    setSelected(option);
    if (soundOn) feedbackTone(isCorrect);
    const word = getCorpus().byId(q.word.id) ?? q.word;
    act((d) => d.reviewWord(word, rating));
  };

  const next = () => {
    if (index + 1 < questions.length) {
      setIndex(index + 1);
      setSelected(null);
      setHint(false);
      shownAt.current = Date.now();
      return;
    }
    const finalScore = score;
    const perfect = finalScore >= total && !usedHint.current;
    const stars = starsFor(finalScore / total);
    const earned = act((d) => d.finishGame({ mode: GAMES.WORD_MATCH, level, correct: finalScore, total, stars, perfect }));
    const nextUnlocked = level < 30 && (stars >= 1 || !!getState().learner.gameLevels[`word_match_${level + 1}`]?.isUnlocked);
    setResult({ xp: earned, stars, next: nextUnlocked ? level + 1 : null });
  };

  return (
    <GameFrame
      title="Word Match"
      scene={sceneForIndex(level)}
      progress={index / total}
      label={`Round ${index + 1}/${total}`}
      score={`Score: ${score}`}
      active={index > 0 || answered}
      footer={
        answered ? (
          <div class={`feedback ${correct ? 'ok' : 'bad'}`} role="status">
            <p class="row-xs t-title-l" style={{ color: correct ? 'var(--green)' : 'var(--red)' }}>
              <Icon name={correct ? 'tickCircle' : 'infoCircle'} size={26} />
              {correct ? 'Correct' : 'Not quite'}
            </p>
            {!correct && <p class="t-body-l" style={{ marginTop: 8 }}>Answer: <b>{answerOf(q.word)}</b></p>}
            {q.word.exampleSentence && <p class="t-body-l" style={{ marginTop: 8 }}>{q.word.exampleSentence}</p>}
            <div style={{ marginTop: 'var(--s-md)' }}>
              <ClayButton label={correct ? 'Continue' : 'Got it'} tone={correct ? 'primary' : 'reward'} onClick={next} />
            </div>
          </div>
        ) : null
      }
    >
      <div class="card panel center" style={{ padding: 'var(--s-lg) var(--s-md)' }}>
        <p class="t-label muted">Match Kasiguranin word:</p>
        <p class="headword" style={{ marginTop: 6 }}>{q.word.kasiguranin}</p>
        {q.word.audioFileName && (
          <button class="text-btn row-xs" style={{ margin: '4px auto 0', color: 'var(--info)', fontSize: 14 }} onClick={() => void playWord(q.word)}>
            <Icon name="volumeHigh" size={18} /> Listen
          </button>
        )}
      </div>
      {!answered && <HintButton hint={hintText} revealed={hint} onReveal={() => { setHint(true); usedHint.current = true; }} />}
      <div class="stack-sm" role="radiogroup" aria-label="Meanings">
        {q.options.map((o) => {
          const cls = answered ? (o === answerOf(q.word) ? 'correct' : o === selected ? 'wrong' : 'dim') : '';
          return (
            <button key={o} class={`option ${cls}`} style={{ background: cls ? undefined : 'var(--surface)' }} disabled={answered} onClick={() => choose(o)} role="radio" aria-checked={o === selected} data-no-tap-sound>
              <span class="label" style={{ fontWeight: 700 }}>{o}</span>
              {answered && o === answerOf(q.word) && <Icon name="tickCircle" size={22} class="mark" label="Correct" />}
              {answered && o === selected && o !== answerOf(q.word) && <Icon name="closeCircle" size={22} class="mark" label="Your answer, incorrect" />}
            </button>
          );
        })}
      </div>
    </GameFrame>
  );
}
