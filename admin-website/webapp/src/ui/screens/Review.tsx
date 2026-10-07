/**
 * The review deck (ui/screens/flashcards): today's due words, flipped and rated Again / Hard / Good
 * / Easy through the same SM-2 path as every other surface. Finishing a deck is half of the day's
 * streak quota.
 */
import { useEffect, useState } from 'preact/hooks';
import { ReviewRating } from '../../domain/sm2';
import type { Word } from '../../domain/types';
import { playSfx, playWord, unlockAudio } from '../../lib/audio';
import { back } from '../../lib/router';
import { act, getCorpus, useApp } from '../../lib/store';
import { ClayButton, Confetti, GroundScaffold, Icon, Jepjep, Loading, ProgressBar } from '../kit';

const DECK_SIZE = 10;

export function ReviewScreen() {
  const ready = useApp((s) => s.contentReady);
  const [cards, setCards] = useState<Word[] | null>(null);
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [extra, setExtra] = useState(false);
  const [complete, setComplete] = useState(false);

  useEffect(() => {
    if (!ready) return;
    setCards(getCorpus().scheduledDue(DECK_SIZE));
  }, [ready]);

  if (!cards) return <Loading label="Shuffling your deck" />;

  if (complete) {
    return (
      <GroundScaffold title="Review">
        <Confetti />
        <div class="readable stack center" style={{ paddingTop: 'var(--s-xl)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="celebrating" height={160} breathe />
          </div>
          <h1 class="t-display">Daily deck complete!</h1>
          <p class="t-body-l muted">You reviewed {cards.length} Kasiguranin flashcards with SuperMemo-2 spaced repetition.</p>
          <ClayButton label="Done" onClick={() => back('/')} />
        </div>
      </GroundScaffold>
    );
  }

  if (!cards.length) {
    return (
      <GroundScaffold title="Review">
        <div class="readable stack center" style={{ paddingTop: 'var(--s-xl)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="sitting" height={160} breathe />
          </div>
          <h1 class="t-headline">You're all caught up</h1>
          <p class="t-body-l muted">
            No words are due for review today. They're scheduled to come back just before you'd forget them — that spacing is what makes them stick.
          </p>
          <ClayButton label="Back" onClick={() => back('/')} />
          <ClayButton
            label="Practise ahead anyway"
            tone="quiet"
            onClick={() => {
              const words = getCorpus().practiceWords(DECK_SIZE);
              setCards(words);
              setExtra(words.length > 0);
              setIndex(0);
            }}
          />
        </div>
      </GroundScaffold>
    );
  }

  const card = cards[index];
  const rate = (rating: ReviewRating) => {
    act((d) => {
      const fresh = getCorpus().byId(card.id) ?? card;
      // A due card is a scheduled review (1-3 XP, capped at 60 a day); practising ahead earns none.
      d.reviewWord(fresh, rating, !extra);
      d.met([fresh.id], 'review');
      if (index + 1 >= cards.length) d.recordDailyReviewCompleted();
    });
    if (index + 1 >= cards.length) {
      setComplete(true);
      playSfx('complete');
    } else {
      setIndex(index + 1);
      setFlipped(false);
    }
  };

  return (
    <GroundScaffold title={extra ? 'Practise ahead' : 'Review'}>
      <div class="readable stack">
        <div class="row">
          <div class="grow">
            <ProgressBar value={index / cards.length} label="Deck progress" />
          </div>
          <span class="t-label muted">{index + 1} / {cards.length}</span>
        </div>

        <button
          class={`flashcard${flipped ? ' flipped' : ''}`}
          data-tour="FlashcardCard"
          data-no-tap-sound
          onClick={() => {
            unlockAudio();
            playSfx('flip');
            setFlipped(!flipped);
          }}
          aria-label={flipped ? `${card.kasiguranin}: ${card.tagalog}. Tap to see the word again` : `${card.kasiguranin}. Tap card to flip`}
        >
          <div class="face front">
            <span class="tag neutral">{card.category.toUpperCase()}</span>
            <p class="headword" style={{ marginTop: 'var(--s-md)' }}>{card.kasiguranin}</p>
            {card.ipaNotation && <p class="t-body-l faint">[{card.ipaNotation}]</p>}
            <p class="t-body muted" style={{ marginTop: 'auto' }}>Tap card to flip</p>
          </div>
          <div class="face back">
            <p class="t-headline">{card.tagalog || card.english}</p>
            {card.tagalog && card.english && <p class="t-body-l muted">({card.english})</p>}
            {card.meaningEnglish && <p class="t-body muted" style={{ marginTop: 8 }}>{card.meaningEnglish}</p>}
            {card.exampleSentence && (
              <div style={{ marginTop: 'var(--s-md)' }}>
                <p class="t-body-l">“{card.exampleSentence}”</p>
                {card.exampleTranslation && <p class="t-body muted">{card.exampleTranslation}</p>}
              </div>
            )}
          </div>
        </button>

        {card.audioFileName && (
          <button class="text-btn row-xs" style={{ alignSelf: 'center', margin: '0 auto', color: 'var(--info)' }} onClick={() => void playWord(card)}>
            <Icon name="volumeHigh" size={20} /> Listen
          </button>
        )}

        {flipped ? (
          <div class="stack-sm">
            <p class="t-label muted center">How well did you remember it?</p>
            <div class="rating">
              <button style={{ background: 'var(--red-fill)' }} onClick={() => rate(ReviewRating.AGAIN)}>Again</button>
              <button style={{ background: 'var(--coral-fill)' }} onClick={() => rate(ReviewRating.HARD)}>Hard</button>
              <button style={{ background: 'var(--lime-fill)' }} onClick={() => rate(ReviewRating.GOOD)}>Good</button>
              <button style={{ background: 'var(--info-fill)' }} onClick={() => rate(ReviewRating.EASY)}>Easy</button>
            </div>
          </div>
        ) : (
          <ClayButton label="Show the meaning" tone="quiet" onClick={() => setFlipped(true)} />
        )}
      </div>
    </GroundScaffold>
  );
}
