/**
 * Library: Words and Stories behind one toggle (ui/screens/library, vocabulary, stories).
 */
import { useEffect, useMemo, useState } from 'preact/hooks';
import { CATEGORIES } from '../../domain/constants';
import { navigate } from '../../lib/router';
import { useApp, useCorpus, useLearner } from '../../lib/store';
import { EmptyState, GroundScaffold, Icon, Scene, SectionHeading, sceneForCategory } from '../kit';
import { AudioButton, StoryCover, WordRow } from '../parts';
import { isStoryUnlocked, wordOfTheDay } from '../derive';

const CATEGORY_BLURB: Record<string, string> = {
  'Greetings & Essentials': 'Hellos, politeness, questions & basic phrases',
  'Food & Dining': 'Rice, fruits, dishes, drinks & cooking',
  'Animals & Wildlife': 'Carabao, birds, dogs, fish & forest life',
  'Body Parts & Health': 'Anatomy, head, limbs, face & senses',
  'Numbers & Time': 'Counting 1-10, days, times of day & seasons',
  'Weather & Climate': 'Rain, wind, sun, clouds & temperature',
  'Emotions & Feelings': 'Happy, angry, sad, afraid & love',
  'House & Daily Life': 'Home objects, clothing, tools & routines',
  'Nature & Environment': 'Ocean, rivers, mountains, soil & plants',
  'Family & People': 'Parents, siblings, children & community',
  'Colors & Shapes': 'Black, white, red, round, sharp & flat',
  'Occupations & Tools': 'Adze, grater, arrow, farming & crafts',
};
export const categoryBlurb = (c: string) => CATEGORY_BLURB[c] ?? 'Kasiguranin vocabulary';

function WordsTab() {
  const corpus = useCorpus();
  const [query, setQuery] = useState('');
  const [settled, setSettled] = useState('');
  useEffect(() => {
    const t = setTimeout(() => setSettled(query.trim()), 200);
    return () => clearTimeout(t);
  }, [query]);
  const results = useMemo(() => (settled ? corpus.search(settled) : []), [settled, corpus]);
  const catMatches = settled ? CATEGORIES.filter((c) => c.toLowerCase().includes(settled.toLowerCase())) : [];
  const featured = useMemo(() => wordOfTheDay(corpus), [corpus]);
  const known = new Set(CATEGORIES);
  const extra = corpus.categories().filter((c) => !known.has(c));

  return (
    <div class="stack">
      <div class="search">
        <Icon name="search" size={20} color="var(--muted)" />
        <input type="search" placeholder="Search words and categories" value={query} onInput={(e) => setQuery((e.target as HTMLInputElement).value)} aria-label="Search the dictionary" enterKeyHint="search" />
        {query && (
          <button class="icon-btn" style={{ width: 36, height: 36 }} aria-label="Clear search" onClick={() => setQuery('')}>
            <Icon name="closeCircle" size={20} color="var(--muted)" />
          </button>
        )}
      </div>

      {settled ? (
        results.length || catMatches.length ? (
          <div class="stack-sm">
            {catMatches.map((c) => (
              <button key={c} class="card row" onClick={() => navigate(`/category/${encodeURIComponent(c)}`)}>
                <Icon name="element4" size={20} color="var(--lime)" />
                <span class="t-title grow">{c}</span>
                <Icon name="arrowRight" size={18} color="var(--faint)" />
              </button>
            ))}
            {results.length > 0 && (
              <>
                <SectionHeading text="Words" />
                <div class="list">
                  {results.map((w) => (
                    <WordRow key={w.id} word={w} />
                  ))}
                </div>
              </>
            )}
          </div>
        ) : (
          <EmptyState
            pose="curious"
            title={`No words match "${settled}"`}
            message="Try the Tagalog or English meaning, or check the spelling. If the word is missing, you can send it for review."
            actionLabel="Add a word"
            onAction={() => navigate('/submit-word')}
          />
        )
      ) : (
        <>
          <p class="t-body muted">{corpus.learnedCount()} of {corpus.size} words learned</p>
          {featured && (
            <button class="card panel glow" style={{ '--gx': '85%', '--gy': '30%' } as never} onClick={() => navigate(`/word/${encodeURIComponent(featured.id)}`)}>
              <div class="row">
                <div class="grow">
                  <p class="t-label muted">Word of the day</p>
                  <p class="headword" style={{ fontSize: 30, lineHeight: '36px', marginTop: 4 }}>{featured.kasiguranin}</p>
                  <p class="t-body muted">{[featured.tagalog, featured.english].filter(Boolean).join(' · ')}</p>
                </div>
                <AudioButton word={featured} label="Play pronunciation" />
              </div>
            </button>
          )}
          <SectionHeading text="Categories" />
          <div class="cat-grid">
            {[...CATEGORIES, ...extra].map((c) => {
              const words = corpus.all.filter((w) => w.category === c);
              if (!words.length) return null;
              const learned = words.filter((w) => w.isLearned).length;
              return (
                <button key={c} class="card flat" onClick={() => navigate(`/category/${encodeURIComponent(c)}`)}>
                  <Scene id={sceneForCategory(c)} height={112} radius="0" style={{ border: 0 }}>
                    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 'var(--s-sm)', textAlign: 'left' }}>
                      <p class="t-title">{c}</p>
                      <p class="t-body-s muted">{words.length} words · {learned} learned</p>
                    </div>
                  </Scene>
                </button>
              );
            })}
          </div>
          <button class="card row" onClick={() => navigate('/submit-word')}>
            <span class="ico" style={{ width: 44, height: 44, borderRadius: 14, background: 'var(--lime-tint)', display: 'grid', placeItems: 'center' }}>
              <Icon name="addCircle" size={22} color="var(--lime)" />
            </span>
            <div class="grow">
              <p class="t-title">Add a word</p>
              <p class="t-body-s muted">Know a Kasiguranin word that is missing? Send it for review.</p>
            </div>
            <Icon name="arrowRight" size={18} color="var(--faint)" />
          </button>
        </>
      )}
    </div>
  );
}

export function StoriesList() {
  const stories = useApp((s) => s.stories);
  const learner = useLearner();
  const inProgress = stories.filter((s) => {
    const p = learner.stories[String(s.id)];
    return p && !p.isCompleted && p.currentPage > 0 && isStoryUnlocked(s, learner);
  });
  if (!stories.length) {
    return <EmptyState pose="reading" title="No stories yet" message="The folk tales arrive with the next sync. Know one? You can share it." actionLabel="Share a story or poem" onAction={() => navigate('/submit-literature')} />;
  }
  return (
    <div class="stack">
      {inProgress.length > 0 && (
        <section class="stack-sm">
          <SectionHeading text="Continue reading" />
          {inProgress.map((s) => (
            <button key={s.id} class="card row" onClick={() => navigate(`/story/${s.id}`)}>
              <Icon name="book" size={22} color="var(--lime)" />
              <div class="grow">
                <p class="t-title">{s.titleKasiguranin || s.title}</p>
                <p class="t-body-s muted">Page {learner.stories[String(s.id)].currentPage + 1} of {s.totalPages}</p>
              </div>
              <Icon name="arrowRight" size={18} color="var(--faint)" />
            </button>
          ))}
        </section>
      )}
      <SectionHeading text="All stories" />
      <div class="story-grid">
        {stories
          .slice()
          .sort((a, b) => a.requiredXp - b.requiredXp)
          .map((s) => (
            <StoryCover
              key={s.id}
              id={s.id}
              title={s.title}
              titleKasiguranin={s.titleKasiguranin}
              totalPages={s.totalPages}
              unlocked={isStoryUnlocked(s, learner)}
              completed={!!learner.stories[String(s.id)]?.isCompleted}
              requiredXp={s.requiredXp}
              onClick={() => navigate(`/story/${s.id}`)}
            />
          ))}
      </div>
      <button class="card row" onClick={() => navigate('/submit-literature')}>
        <Icon name="edit" size={22} color="var(--lime)" />
        <span class="t-title grow">Share a story or poem</span>
        <Icon name="arrowRight" size={18} color="var(--faint)" />
      </button>
    </div>
  );
}

export function LibraryScreen({ tab }: { tab: 'words' | 'stories' }) {
  const setTab = (t: 'words' | 'stories') => history.replaceState(history.state, '', t === 'stories' ? '/library?tab=stories' : '/library');
  const [current, setCurrent] = useState(tab);
  useEffect(() => setCurrent(tab), [tab]);
  return (
    <GroundScaffold
      title="Library"
      onBack={false}
      nav
      largeTitle
      subtitle={current === 'words' ? 'Every word, with its meaning and how it sounds' : 'Stories with Tagalog and English alongside'}
    >
      <div class="stack">
        <div class="segmented" role="tablist" aria-label="Library">
          {(['words', 'stories'] as const).map((t) => (
            <button
              key={t}
              role="tab"
              aria-selected={current === t}
              onClick={() => {
                setCurrent(t);
                setTab(t);
              }}
            >
              {t === 'words' ? 'Words' : 'Stories'}
            </button>
          ))}
        </div>
        {current === 'words' ? <WordsTab /> : <StoriesList />}
      </div>
    </GroundScaffold>
  );
}
