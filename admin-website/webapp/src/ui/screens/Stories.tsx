/**
 * The story reader (StoryReaderScreen): one page at a time, Kasiguranin first when a page has it,
 * Tagalog and English alongside. Pages save the reading position; finishing a story the first time
 * pays 20 XP and counts toward Story Reader. Page pictures, when an admin has uploaded one, come from
 * Firestore one page at a time.
 */
import { useEffect, useMemo, useState } from 'preact/hooks';
import { doc, getDoc } from 'firebase/firestore/lite';
import { normaliseToken } from '../../domain/lesson';
import type { StoryPage, Word } from '../../domain/types';
import { parsePages } from '../../lib/content';
import { db } from '../../lib/firebase';
import { back, navigate } from '../../lib/router';
import { act, useApp, useCorpus, useLearner } from '../../lib/store';
import { ClayButton, Confetti, Dialog, EmptyState, GroundScaffold, Icon, Jepjep, ProgressBar, sceneForIndex, sceneUrl } from '../kit';
import { AudioButton } from '../parts';
import { isStoryUnlocked } from '../derive';

const imageCache = new Map<string, string | null>();

async function pageImage(storyId: number, imageId: string): Promise<string | null> {
  const key = `${storyId}_${imageId}`;
  if (imageCache.has(key)) return imageCache.get(key)!;
  try {
    const snap = await getDoc(doc(db, 'story_page_images', key));
    const bytes: Uint8Array | undefined = snap.exists() ? snap.data().data?.toUint8Array?.() : undefined;
    const url = bytes && bytes.length ? URL.createObjectURL(new Blob([bytes.slice()], { type: snap.data()?.mimeType || 'image/webp' })) : null;
    imageCache.set(key, url);
    return url;
  } catch {
    return null;
  }
}

function WordSheet({ word, onClose }: { word: Word; onClose: () => void }) {
  return (
    <Dialog label={word.kasiguranin} onClose={onClose}>
      <div class="stack">
        <div class="row">
          <div class="grow">
            <p class="headword" style={{ fontSize: 30, lineHeight: '36px' }}>{word.kasiguranin}</p>
            <p class="t-body-l muted">{[word.tagalog, word.english].filter(Boolean).join(' · ')}</p>
          </div>
          <AudioButton word={word} />
        </div>
        {word.meaningEnglish && <p class="t-body-l">{word.meaningEnglish}</p>}
        <div class="stack-sm">
          <ClayButton label="Open in the dictionary" tone="quiet" onClick={() => { onClose(); navigate(`/word/${encodeURIComponent(word.id)}`); }} />
          <ClayButton label="Close" onClick={onClose} />
        </div>
      </div>
    </Dialog>
  );
}

export function StoryReaderScreen({ id }: { id: number }) {
  const story = useApp((s) => s.stories.find((x) => x.id === id));
  const learner = useLearner();
  const corpus = useCorpus();
  const progress = learner.stories[String(id)];
  const pages: StoryPage[] = useMemo(() => (story ? parsePages(story) : []), [story]);
  const [index, setIndex] = useState(() => (progress && !progress.isCompleted ? Math.min(progress.currentPage, Math.max(0, pages.length - 1)) : 0));
  const [finished, setFinished] = useState(false);
  const [earned, setEarned] = useState(0);
  const [selected, setSelected] = useState<Word | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [image, setImage] = useState<string | null>(null);
  const page = pages[index];

  useEffect(() => {
    setImage(null);
    if (!page?.imageId || !navigator.onLine) return;
    let alive = true;
    pageImage(id, page.imageId).then((u) => alive && setImage(u));
    const next = pages[index + 1];
    if (next?.imageId) void pageImage(id, next.imageId);
    return () => {
      alive = false;
    };
  }, [id, index, page?.imageId]);

  if (!story) {
    return (
      <GroundScaffold title="Story">
        <EmptyState pose="confused" title="This story couldn't be found." />
      </GroundScaffold>
    );
  }
  if (!isStoryUnlocked(story, learner)) {
    return (
      <GroundScaffold title={story.title}>
        <EmptyState
          pose="reading"
          title="This story is still locked"
          message={`Earn ${story.requiredXp - learner.progress.totalXp} more XP to open it. Lessons, reviews and games all count.`}
          actionLabel="Go learn"
          onAction={() => navigate('/learn')}
        />
      </GroundScaffold>
    );
  }
  if (finished) {
    return (
      <main class="page no-nav glow" style={{ '--gx': '50%', '--gy': '25%', minHeight: '100dvh', paddingTop: 'calc(var(--safe-top) + var(--s-xl))' } as never}>
        <Confetti />
        <div class="readable stack center">
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="reading" height={170} breathe />
          </div>
          <h1 class="t-display">Story complete!</h1>
          <p class="t-body-l muted">You finished “{story.title}”{earned > 0 ? ` and earned ${earned} XP.` : '.'}</p>
          <ClayButton label="Continue" onClick={() => back('/library?tab=stories')} />
        </div>
      </main>
    );
  }
  if (!pages.length) {
    return (
      <GroundScaffold title={story.title}>
        <EmptyState pose="confused" title="No pages available" />
      </GroundScaffold>
    );
  }

  const next = () => {
    if (index < pages.length - 1) {
      act((d) => d.storyPage(id, index + 1), { celebrate: false });
      setIndex(index + 1);
      window.scrollTo(0, 0);
    } else {
      setEarned(act((d) => d.completeStory(id)));
      setFinished(true);
    }
  };
  const prev = () => {
    if (index === 0) return;
    act((d) => d.storyPage(id, index - 1), { celebrate: false });
    setIndex(index - 1);
    window.scrollTo(0, 0);
  };
  const findWord = (raw: string) => {
    const n = normaliseToken(raw);
    return corpus.all.find((w) => w.kasiguranin.toLowerCase() === n);
  };

  return (
    <GroundScaffold title={story.titleKasiguranin || story.title}>
      <div class="readable stack" style={{ paddingBottom: 96 }}>
        <ProgressBar value={(index + 1) / pages.length} label="Story progress" />
        <div class="story-art">
          <img src={image ?? sceneUrl(sceneForIndex(id))} alt={image ? page.illustrationDesc || '' : ''} />
          {!image && page.illustrationDesc && <p class="t-body-s">{page.illustrationDesc}</p>}
        </div>

        {page.kasiguranin?.trim() && (
          <section class="stack-sm">
            <div class="story-chips">
              {page.kasiguranin.split(' ').filter(Boolean).map((w, i) => (
                <button
                  key={i}
                  class="story-chip"
                  onClick={() => {
                    const found = findWord(w);
                    if (found) setSelected(found);
                    else setNotFound(true);
                  }}
                >
                  {w}
                </button>
              ))}
            </div>
            <p class="t-body-s faint">Tap a word to look it up.</p>
          </section>
        )}

        <section class="card stack-sm">
          <p class="t-label muted">Tagalog</p>
          <p class="t-body-l">{page.tagalog}</p>
        </section>
        <section class="card stack-sm">
          <p class="t-label muted">English</p>
          <p class="t-body-l">{page.english}</p>
        </section>
      </div>

      <nav class="story-bar" aria-label="Pages">
        <button class="icon-btn boxed" aria-label="Previous page" disabled={index === 0} onClick={prev} style={{ opacity: index === 0 ? 0.4 : 1 }}>
          <Icon name="arrowLeft" size={22} />
        </button>
        <span class="t-label muted">{index + 1} / {pages.length}</span>
        <button class="clay small" onClick={next} style={{ marginBottom: 5 }}>
          {index < pages.length - 1 ? (
            <>
              Next <Icon name="arrowRight" size={18} />
            </>
          ) : (
            <>
              Finish <Icon name="tickCircle" size={18} />
            </>
          )}
        </button>
      </nav>

      {selected && <WordSheet word={selected} onClose={() => setSelected(null)} />}
      {notFound && (
        <Dialog label="Not in the dictionary yet" onClose={() => setNotFound(false)}>
          <div class="stack">
            <h2 class="t-headline-s">Not in the dictionary yet</h2>
            <p class="t-body-l muted">This word isn't in the vocabulary list yet, so there's no definition to show.</p>
            <ClayButton label="Got it" onClick={() => setNotFound(false)} />
          </div>
        </Dialog>
      )}
    </GroundScaffold>
  );
}
