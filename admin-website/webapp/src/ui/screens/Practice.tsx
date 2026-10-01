/**
 * Practice: the mini-game shelf, the review deck, and the leaderboard (ui/screens/games/GameHubScreen).
 * Word Match, Word Search and Word Wheel are playable; the other five show "Coming soon", exactly as
 * the Android app ships them today.
 */
import { useState } from 'preact/hooks';
import { GAME_ENTRIES, gameTitle, type GameEntry } from '../../domain/gamification';
import { levelTitle } from '../../domain/constants';
import { GAMES } from '../../domain/constants';
import { totalStars } from '../../domain/learner';
import { navigate } from '../../lib/router';
import { setPrefs, useApp, useCorpus, useLearner } from '../../lib/store';
import { ClayButton, Dialog, GroundScaffold, Icon, SectionHeading } from '../kit';
import { LeaderboardContent } from './Leaderboard';
import { wordsToReview } from '../derive';

const TONE: Record<GameEntry['tone'], { fill: string; ink: string }> = {
  lime: { fill: 'rgba(113,189,29,0.16)', ink: 'var(--lime)' },
  coral: { fill: 'var(--coral)', ink: 'var(--reward-ink)' },
  gold: { fill: 'var(--gold)', ink: 'var(--reward-ink)' },
};

export const GAME_RULES: Record<string, { title: string; description: string; rules: string[] }> = {
  [GAMES.WORD_MATCH]: {
    title: 'Word Match',
    description: 'Match each Kasiguranin word to its meaning.',
    rules: [
      'Pick the right meaning from four choices.',
      'Stuck? Show the definition. A hinted answer still counts, but the round is no longer perfect.',
      'Clear a round for 5 XP + 2 per correct answer. An unassisted perfect round adds 5, up to 40 XP total.',
    ],
  },
  [GAMES.WORD_SEARCH]: {
    title: 'Word Search',
    description: 'Find hidden Kasiguranin words in a grid of letters. Pick a category first; each has its own 30 levels.',
    rules: [
      "Drag across a word, or tap its first letter and then its last.",
      'Levels 1-10 run across and down, 11-20 add diagonals, 21-30 go any direction, even backwards.',
      'Find every word with no wrong lines for three stars.',
    ],
  },
  [GAMES.WORD_WHEEL]: {
    title: 'Word Wheel',
    description: 'Spell Kasiguranin words from the letters on the wheel to fill the crossword.',
    rules: [
      'Swipe across the letters, or tap them and press Check. Words need 3 letters or more.',
      'Other real words you spell are bonus words; XP comes from completing the board.',
      'Solve the board with no hints for three stars.',
    ],
  },
};

export function openGame(type: string) {
  if (type === GAMES.WORD_SEARCH) navigate('/games/word_search');
  else navigate(`/games/levels/${type}`);
}

function GameRulesDialog({ type, onClose }: { type: string; onClose: () => void }) {
  const rules = GAME_RULES[type];
  const [dontShow, setDontShow] = useState(false);
  const seen = useApp((s) => s.prefs.gameRulesSeen);
  if (!rules) return null;
  return (
    <Dialog label={rules.title} onClose={onClose}>
      <div class="stack">
        <div class="row">
          <div style={{ width: 52, height: 52, borderRadius: 16, background: 'var(--lime-tint)', display: 'grid', placeItems: 'center', flex: 'none' }}>
            <Icon name="game" size={28} color="var(--lime)" />
          </div>
          <div class="grow">
            <h2 class="t-headline-s">{rules.title}</h2>
            <span class="tag">Unlocked</span>
          </div>
        </div>
        <p class="t-body-l muted">{rules.description}</p>
        <div>
          <h3 class="t-title-s" style={{ marginBottom: 8 }}>How to play</h3>
          <ul class="stack-sm" style={{ margin: 0, paddingLeft: 20 }}>
            {rules.rules.map((r) => (
              <li key={r} class="t-body-l">{r}</li>
            ))}
          </ul>
        </div>
        <label class="row" style={{ cursor: 'pointer' }}>
          <input type="checkbox" checked={dontShow} onChange={(e) => setDontShow((e.target as HTMLInputElement).checked)} style={{ width: 20, height: 20, accentColor: 'var(--lime)' }} />
          <span class="t-body">Don't show these rules again</span>
        </label>
        <ClayButton
          label="Start game"
          onClick={() => {
            if (dontShow) setPrefs({ gameRulesSeen: [...new Set([...seen, type])] });
            onClose();
            openGame(type);
          }}
        />
      </div>
    </Dialog>
  );
}

export function PracticeScreen() {
  const learner = useLearner();
  const corpus = useCorpus();
  const seen = useApp((s) => s.prefs.gameRulesSeen);
  const [rules, setRules] = useState<string | null>(null);
  const [segment, setSegment] = useState<'games' | 'leaderboard'>('games');
  const p = learner.progress;
  const stars = totalStars(learner.gameLevels);
  const due = Math.min(corpus.countScheduledDue(), 20);

  const highScores = new Map<string, number>();
  for (const s of learner.gameScores) highScores.set(s.gameType, Math.max(highScores.get(s.gameType) ?? 0, s.score));
  const playable = (g: GameEntry) => !g.comingSoon && stars >= g.unlockStars;
  const recommended =
    GAME_ENTRIES.filter(playable).sort((a, b) => (highScores.get(a.type) ?? 0) - (highScores.get(b.type) ?? 0))[0] ??
    GAME_ENTRIES.filter((g) => !g.comingSoon).sort((a, b) => a.unlockStars - b.unlockStars)[0];
  const tap = (type: string) => (seen.includes(type) ? openGame(type) : setRules(type));
  const accuracy = p.totalQuestionsAnswered === 0 ? '–' : `${Math.round((p.totalCorrectAnswers / p.totalQuestionsAnswered) * 100)}%`;
  const recent = learner.gameScores.slice(-5).reverse();

  return (
    <GroundScaffold title="Practice" onBack={false} nav largeTitle subtitle={levelTitle(p.level)}>
      {rules && <GameRulesDialog type={rules} onClose={() => setRules(null)} />}
      <div class="segmented" role="tablist" style={{ marginBottom: 'var(--s-md)' }}>
        <button role="tab" aria-selected={segment === 'games'} onClick={() => setSegment('games')}>Games</button>
        <button role="tab" aria-selected={segment === 'leaderboard'} onClick={() => setSegment('leaderboard')}>Leaderboard</button>
      </div>
      {segment === 'leaderboard' ? (
        <LeaderboardContent />
      ) : (
      <div class="stack-lg">
        <div class="card">
          <div class="stats3">
            <div><p class="t-headline-s">{p.totalXp}</p><p class="t-label-s muted">XP</p></div>
            <div><p class="t-headline-s">{stars}</p><p class="t-label-s muted">Stars</p></div>
            <div><p class="t-headline-s">{accuracy}</p><p class="t-label-s muted">Accuracy</p></div>
          </div>
        </div>

        <section class="stack-sm">
          <SectionHeading text="Review" />
          <button class="card" onClick={() => navigate('/review')}>
            <div class="row">
              <div style={{ width: 52, height: 52, borderRadius: 16, background: 'rgba(79,179,232,0.14)', display: 'grid', placeItems: 'center', flex: 'none' }}>
                <Icon name="repeat" size={26} color="var(--info)" />
              </div>
              <div class="grow">
                <p class="t-title">Flashcard review</p>
                <p class="t-body-s muted">{due > 0 ? `${wordsToReview(due)} due today` : 'Nothing due today. Practise any time.'}</p>
              </div>
              <Icon name="arrowRight" size={18} color="var(--faint)" />
            </div>
          </button>
        </section>

        {recommended && (
          <section class="stack-sm">
            <SectionHeading text="Continue practicing" />
            <button class="card panel" onClick={() => tap(recommended.type)}>
              <div class="row">
                <div style={{ width: 56, height: 56, borderRadius: 18, background: TONE[recommended.tone].fill, display: 'grid', placeItems: 'center', flex: 'none' }}>
                  <Icon name={recommended.icon} size={26} color={TONE[recommended.tone].ink} />
                </div>
                <div class="grow">
                  <p class="t-title">{recommended.title}</p>
                  <p class="t-body-s muted">{(highScores.get(recommended.type) ?? 0) > 0 ? `Best: ${highScores.get(recommended.type)} · beat your record` : 'Not played yet'}</p>
                </div>
                <Icon name="arrowRight" size={18} color="var(--faint)" />
              </div>
            </button>
          </section>
        )}

        <section class="stack-sm">
          <SectionHeading text="All mini-games" />
          <p class="t-body muted" style={{ marginTop: -4 }}>More games are on the way</p>
          <div class="game-grid">
            {GAME_ENTRIES.map((g) => {
              const unlocked = playable(g);
              const tone = TONE[g.tone];
              return (
                <button key={g.type} class="card" disabled={g.comingSoon} onClick={() => tap(g.type)} style={{ padding: 'var(--s-sm)' }} aria-label={`${g.title}${g.comingSoon ? ', coming soon' : unlocked ? '' : `, ${g.unlockStars} stars to unlock`}`}>
                  <div style={{ width: 44, height: 44, borderRadius: 14, display: 'grid', placeItems: 'center', background: unlocked ? tone.fill : 'var(--sunken)' }}>
                    <Icon name={unlocked || g.comingSoon ? g.icon : 'lock'} size={22} color={unlocked ? tone.ink : 'var(--faint)'} />
                  </div>
                  <p class="t-title" style={{ marginTop: 'var(--s-sm)', color: unlocked ? 'var(--ink)' : 'var(--faint)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{g.title}</p>
                  {g.comingSoon ? (
                    <span class="tag neutral" style={{ marginTop: 4 }}>Coming soon</span>
                  ) : (
                    <p class="t-label-s muted" style={{ marginTop: 2 }}>{unlocked ? `Best: ${highScores.get(g.type) ?? 0}` : `${g.unlockStars} stars to unlock`}</p>
                  )}
                </button>
              );
            })}
          </div>
        </section>

        {recent.length > 0 && (
          <section class="stack-sm">
            <SectionHeading text="Recent activity" />
            <div class="list">
              {recent.map((s, i) => (
                <div key={i} class="list-row">
                  <span class="ico"><Icon name="game" size={18} color="var(--lime)" /></span>
                  <div class="grow">
                    <p class="t-title-s">{gameTitle(s.gameType)}</p>
                    <p class="t-body-s muted">
                      {s.score}/{s.totalQuestions} · {s.totalQuestions ? Math.round((s.score / s.totalQuestions) * 100) : 0}%
                    </p>
                  </div>
                  <span class="t-label" style={{ color: 'var(--gold)' }}>+{s.xpEarned} XP</span>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
      )}
    </GroundScaffold>
  );
}
