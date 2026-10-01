/**
 * Level selection (LevelSelectionScreen) and the Word Search category picker
 * (WordSearchCategoryScreen). Thirty levels per track; clearing one with a star opens the next.
 */
import { CATEGORIES, categoryForWordSearchKey, GAMES, LEVELS_PER_GAME, wordSearchLevelKey } from '../../domain/constants';
import { difficultyForLevel } from '../../domain/gamification';
import { levelKey } from '../../domain/merge';
import { categoryPlayable } from '../../domain/wordSearch';
import { navigate, enc } from '../../lib/router';
import { useCorpus, useLearner } from '../../lib/store';
import { GroundScaffold, Icon, Scene, sceneForCategory } from '../kit';
import { GAME_RULES } from '../screens/Practice';

function levelState(levels: Record<string, { starsEarned: number; isUnlocked: boolean }>, type: string, n: number) {
  return levels[levelKey(type, n)] ?? { starsEarned: 0, isUnlocked: n === 1 };
}

function Stars({ count, size = 12 }: { count: number; size?: number }) {
  return (
    <span class="row-xs" style={{ gap: 1, justifyContent: 'center' }} aria-hidden="true">
      {[0, 1, 2].map((i) => (
        <Icon key={i} name="star" size={size} color={i < count ? 'var(--gold)' : 'var(--track)'} />
      ))}
    </span>
  );
}

export function LevelSelectScreen({ gameType }: { gameType: string }) {
  const learner = useLearner();
  const wsCategory = categoryForWordSearchKey(gameType);
  const title = wsCategory ?? GAME_RULES[gameType]?.title ?? 'Levels';
  const subtitle = wsCategory ? 'Word Search · clear a level to unlock the next' : 'Clear a level to unlock the next one';
  const open = (n: number) => {
    if (wsCategory) navigate(`/games/word_search/${enc(gameType)}/${n}`);
    else navigate(`/games/${gameType}/${n}`);
  };
  const bands: ['Easy' | 'Medium' | 'Hard', number, number][] = [
    ['Easy', 1, 10],
    ['Medium', 11, 20],
    ['Hard', 21, 30],
  ];
  return (
    <GroundScaffold title={title} largeTitle subtitle={subtitle}>
      <div class="stack-lg">
        {bands.map(([name, from, to]) => (
          <section key={name} class="stack-sm">
            <h3 class="t-title-l">{name}</h3>
            <div class="level-grid">
              {Array.from({ length: to - from + 1 }, (_, i) => {
                const n = from + i;
                const st = levelState(learner.gameLevels, gameType, n);
                return (
                  <button
                    key={n}
                    class={`level-cell${st.isUnlocked ? '' : ' locked'}${st.starsEarned > 0 ? ' cleared' : ''}`}
                    disabled={!st.isUnlocked}
                    onClick={() => open(n)}
                    aria-label={st.isUnlocked ? `Level ${n}, ${st.starsEarned} of 3 stars` : `Level ${n}, locked`}
                  >
                    {st.isUnlocked ? <span class="t-title-l">{n}</span> : <Icon name="lock" size={18} />}
                    {st.isUnlocked && <Stars count={st.starsEarned} />}
                  </button>
                );
              })}
            </div>
          </section>
        ))}
        {gameType === GAMES.WORD_MATCH && (
          <p class="t-body-s faint center">
            {difficultyForLevel(1)} levels ask 5 questions, Medium 10 and Hard 15.
          </p>
        )}
      </div>
    </GroundScaffold>
  );
}

export function WordSearchCategoriesScreen() {
  const corpus = useCorpus();
  const learner = useLearner();
  return (
    <GroundScaffold title="Word Search" largeTitle subtitle={`Pick a category. Each one has its own ${LEVELS_PER_GAME} levels, from a small grid to a big one.`}>
      <div class="stack-sm">
        {CATEGORIES.map((category) => {
          const key = wordSearchLevelKey(category);
          const playable = categoryPlayable(corpus.inCategory(category));
          let cleared = 0;
          let stars = 0;
          for (let n = 1; n <= LEVELS_PER_GAME; n++) {
            const st = learner.gameLevels[levelKey(key, n)];
            if (st?.starsEarned) {
              cleared++;
              stars += st.starsEarned;
            }
          }
          const status = !playable
            ? 'Not enough short words in this category yet'
            : cleared >= LEVELS_PER_GAME
              ? `All ${LEVELS_PER_GAME} levels cleared · ${stars} stars`
              : cleared === 0
                ? 'Start at level 1'
                : `Level ${cleared + 1} of ${LEVELS_PER_GAME} · ${stars} stars`;
          return (
            <button key={category} class="card flat" disabled={!playable} onClick={() => navigate(`/games/levels/${key}`)} style={{ opacity: playable ? 1 : 0.6 }}>
              <Scene id={sceneForCategory(category)} height={92} radius="0" style={{ border: 0 }}>
                <div class="row" style={{ height: '100%', padding: '0 var(--s-md)' }}>
                  <div class="grow" style={{ textAlign: 'left' }}>
                    <p class="t-title">{category}</p>
                    <p class="t-body-s muted">{status}</p>
                  </div>
                  {playable && <Icon name="arrowRight" size={18} color="var(--muted)" />}
                </div>
              </Scene>
            </button>
          );
        })}
      </div>
    </GroundScaffold>
  );
}

export const WORD_SEARCH = GAMES.WORD_SEARCH;
