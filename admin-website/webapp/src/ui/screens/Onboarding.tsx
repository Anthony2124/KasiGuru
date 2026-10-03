/**
 * Onboarding: the same fifteen-beat wizard as the Android app (ui/screens/onboarding), with one
 * web-specific swap. Android's "Let Jepjep remind you" step asks for notification permission; a
 * browser cannot deliver those reminders reliably, so the web app uses that slot to show how to add
 * KasiGuru to the home screen, which is what makes it behave like an app on an iPhone.
 */
import { STORIES_ENABLED } from '../../domain/constants';
import { useState } from 'preact/hooks';
import { ONBOARDING_DEFAULT_NAME } from '../../domain/types';
import { act } from '../../lib/store';
import { navigate } from '../../lib/router';
import { useInstall, isIOSSafari } from '../../lib/install';
import { AVATARS, Avatar, ClayButton, Confetti, Highlighted, Icon, Jepjep, Wordmark, sceneUrl } from '../kit';
import type { IconName } from '../icons.generated';

type Step =
  | 'Welcome' | 'Asleep' | 'Awake' | 'Name' | 'Greeting' | 'Words' | 'Minutes' | 'Games' | 'Level'
  | 'Goal' | 'FirstWord' | 'FirstWordLearned' | 'Install' | 'Avatar' | 'Badges';

const STEPS: { id: Step; framed: boolean; skippable: boolean; optional: boolean }[] = [
  { id: 'Welcome', framed: false, skippable: false, optional: true },
  { id: 'Asleep', framed: false, skippable: false, optional: true },
  { id: 'Awake', framed: false, skippable: false, optional: true },
  { id: 'Name', framed: true, skippable: true, optional: true },
  { id: 'Greeting', framed: true, skippable: true, optional: true },
  { id: 'Words', framed: true, skippable: true, optional: true },
  { id: 'Minutes', framed: true, skippable: true, optional: true },
  { id: 'Games', framed: true, skippable: true, optional: true },
  { id: 'Level', framed: true, skippable: true, optional: true },
  { id: 'Goal', framed: true, skippable: true, optional: true },
  { id: 'FirstWord', framed: true, skippable: false, optional: false },
  { id: 'FirstWordLearned', framed: true, skippable: false, optional: true },
  { id: 'Install', framed: true, skippable: true, optional: true },
  { id: 'Avatar', framed: true, skippable: true, optional: true },
  { id: 'Badges', framed: true, skippable: false, optional: true },
];
const FRAMED_COUNT = STEPS.filter((s) => s.framed).length;
const FIRST_WORD_INDEX = STEPS.findIndex((s) => s.id === 'FirstWord');

const GOALS = [
  { xp: 30, minutes: 5, label: 'Casual' },
  { xp: 50, minutes: 10, label: 'Regular' },
  { xp: 80, minutes: 15, label: 'Serious' },
  { xp: 100, minutes: 20, label: 'Intense' },
];
const LEVELS = [
  { label: "I'm new to it", goal: 0 },
  { label: 'I know a few words', goal: 1 },
  { label: 'I understand it but rarely speak it', goal: 2 },
  { label: 'I speak it', goal: 3 },
];
const FIRST_WORD = { word: 'aldew', ipa: '[ˈɁal.dɛw]', meaning: 'Day, sun', options: ['Water', 'Day, sun'], correct: 1 };
const MAX_NAME_LENGTH = 30;

/** One milestone from four of the eleven badge families (XP policy 2), each six tiers deep. */
const BADGES: { name: string; condition: string; tier: number; icon: IconName }[] = [
  { name: 'Word Explorer', condition: 'Verify 50 words in review', tier: 3, icon: 'book' },
  { name: 'Consistent Learner', condition: 'Keep a 7-day streak', tier: 3, icon: 'flash' },
  // Story Reader while stories are switched on; until then a badge a new learner can earn now.
  STORIES_ENABLED
    ? { name: 'Story Reader', condition: 'Finish 3 stories', tier: 3, icon: 'document' }
    : { name: 'Category Scholar', condition: 'Finish 3 categories', tier: 3, icon: 'element4' },
  { name: 'Journey Rank', condition: 'Reach Level 10', tier: 4, icon: 'medalStar' },
];
const TIER_LABEL = ['', 'Beginner', 'Learner', 'Achiever', 'Expert', 'Master', 'Legend'];

function Option({ label, state, onClick, disabled, center }: { label: string; state: 'idle' | 'selected' | 'correct' | 'wrong'; onClick: () => void; disabled?: boolean; center?: boolean }) {
  const cls = state === 'idle' ? '' : state;
  return (
    <button class={`option ${cls}`} onClick={onClick} disabled={disabled} role="radio" aria-checked={state !== 'idle'} style={center ? { justifyContent: 'center', textAlign: 'center' } : undefined}>
      <span class="label">{label}</span>
      {state !== 'idle' && <Icon name={state === 'wrong' ? 'closeCircle' : 'tickCircle'} size={20} class="mark" label={state === 'correct' ? 'Correct' : state === 'wrong' ? 'Not this one' : undefined} />}
    </button>
  );
}

function Title({ text, words = [], align = 'left' }: { text: string; words?: string[]; align?: 'left' | 'center' | 'right' }) {
  return (
    <h1 class="t-display" style={{ textAlign: align }}>
      <Highlighted text={text} words={words} />
    </h1>
  );
}

function Body({ text, words = [], align = 'left' }: { text: string; words?: string[]; align?: 'left' | 'center' | 'right' }) {
  return (
    <p class="t-body-l muted" style={{ textAlign: align }}>
      <Highlighted text={text} words={words} />
    </p>
  );
}

/** Jepjep large and partly off the edge, fading out where the headline takes over. */
function Cropped({ pose, align }: { pose: Parameters<typeof Jepjep>[0]['pose']; align: 'left' | 'center' | 'right' }) {
  return (
    <div
      style={{
        height: 'min(34vh, 260px)',
        overflow: 'hidden',
        display: 'flex',
        justifyContent: align === 'left' ? 'flex-start' : align === 'right' ? 'flex-end' : 'center',
        WebkitMaskImage: 'linear-gradient(to bottom, #000 72%, transparent)',
        maskImage: 'linear-gradient(to bottom, #000 72%, transparent)',
      }}
    >
      <Jepjep pose={pose} height={Math.min(window.innerHeight * 0.34 * 1.3, 338)} breathe style={{ marginLeft: align === 'left' ? '-10%' : undefined, marginRight: align === 'right' ? '-12%' : undefined }} />
    </div>
  );
}

function WordCard({ subtitle, faint }: { subtitle: string; faint?: boolean }) {
  return (
    <div class="card center" style={{ padding: 'var(--s-lg) var(--s-md)' }}>
      <p class="headword">{FIRST_WORD.word}</p>
      <p class={`t-body-l ${faint ? 'faint' : 'muted'}`}>{subtitle}</p>
    </div>
  );
}

function InstallStep() {
  const install = useInstall();
  const safari = isIOSSafari();
  return (
    <div class="stack">
      <div style={{ display: 'grid', placeItems: 'center' }}>
        <Jepjep pose="sitting" height={170} breathe />
      </div>
      <Title text="Keep KasiGuru on your home screen" words={['home screen']} />
      {install.installed ? (
        <Body text="You're already using KasiGuru as an app. It opens full screen and works offline." />
      ) : install.ios ? (
        <>
          <Body text="On iPhone and iPad, add it from Safari and it opens full screen like any other app, even offline." words={['Safari']} />
          <ol class="list" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            <li class="list-row">
              <span class="ico"><Icon name="share" size={20} color="var(--info)" /></span>
              <span class="t-body-l">Tap <b>Share</b> in Safari's toolbar</span>
            </li>
            <li class="list-row">
              <span class="ico"><Icon name="addCircle" size={20} color="var(--lime)" /></span>
              <span class="t-body-l">Choose <b>Add to Home Screen</b></span>
            </li>
            <li class="list-row">
              <span class="ico"><Icon name="tickCircle" size={20} color="var(--lime)" /></span>
              <span class="t-body-l">Tap <b>Add</b>, then open KasiGuru from your home screen</span>
            </li>
          </ol>
          {!safari && <p class="t-body faint">Open this page in Safari first: other browsers on iPhone cannot add apps to the home screen.</p>}
        </>
      ) : install.canPrompt ? (
        <>
          <Body text="Install it and it opens in its own window, straight from your home screen or desktop." />
          <ClayButton label="Install KasiGuru" icon="mobile" tone="quiet" onClick={() => void install.prompt()} />
        </>
      ) : (
        <Body text="Use your browser's menu to add KasiGuru to your home screen, and it will open like an app. On Android you can also get the full app from the download page." />
      )}
    </div>
  );
}

export function OnboardingScreen() {
  const [index, setIndex] = useState(0);
  const [name, setName] = useState('');
  const [level, setLevel] = useState(-1);
  const [goal, setGoal] = useState(-1);
  const [answer, setAnswer] = useState(-1);
  const [avatar, setAvatar] = useState(1);
  const [dir, setDir] = useState(1);
  const step = STEPS[index];
  const chosenGoal = GOALS[goal >= 0 ? goal : level >= 0 ? LEVELS[level].goal : 1];

  const goTo = (i: number) => {
    setDir(i >= index ? 1 : -1);
    setIndex(i);
    window.scrollTo(0, 0);
  };
  const finish = () => {
    act((d) => d.completeOnboarding(name.trim() || ONBOARDING_DEFAULT_NAME, avatar, chosenGoal.xp, 'Kasiguranin Apprentice'));
    navigate('/', { replace: true });
  };
  const advance = () => (index + 1 < STEPS.length ? goTo(index + 1) : finish());
  const goBack = () => {
    for (let i = index - 1; i >= 0; i--) if (STEPS[i].framed || STEPS[i].id === 'Welcome') return goTo(i);
  };
  const skip = () => (index < FIRST_WORD_INDEX ? goTo(FIRST_WORD_INDEX) : finish());

  if (step.id === 'Welcome') {
    return (
      <div style={{ position: 'relative', minHeight: '100dvh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <img src={sceneUrl('forest')} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to bottom, rgba(10,14,13,.35) 0%, transparent 12%, rgba(10,14,13,.35) 45%, rgba(10,14,13,.8) 72%, rgba(10,14,13,.92) 100%)' }} />
        <div style={{ position: 'relative', flex: 1, display: 'grid', placeItems: 'center', paddingTop: 'var(--safe-top)' }}>
          <Jepjep pose="celebrating" height={Math.min(320, window.innerHeight * 0.4)} breathe />
        </div>
        <div class="readable center stack" style={{ position: 'relative', padding: '0 var(--gutter) calc(var(--safe-bottom) + var(--s-md))' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Wordmark width={220} />
          </div>
          <p class="t-body-l">A gamified mobile learning app for the preservation and learning of the Kasiguranin language.</p>
          <div class="stack-sm" style={{ paddingTop: 'var(--s-md)' }}>
            <ClayButton label="Get started" onClick={advance} />
            <ClayButton label="I already have an account" tone="quiet" onClick={() => navigate('/account')} />
          </div>
        </div>
      </div>
    );
  }

  if (step.id === 'Asleep' || step.id === 'Awake') {
    return (
      <button
        class="glow"
        onClick={advance}
        aria-label="Continue"
        style={{ '--gx': '50%', '--gy': '45%', width: '100%', minHeight: '100dvh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 'var(--s-xl)', padding: 'var(--safe-top) var(--gutter) var(--safe-bottom)' } as never}
      >
        <div class={step.id === 'Awake' ? 'pop' : ''}>
          <Jepjep pose={step.id === 'Asleep' ? 'sleeping' : 'waving'} height={Math.max(160, Math.min(340, window.innerHeight * 0.42))} breathe />
        </div>
        <p class="t-body-l muted">Tap anywhere to continue</p>
      </button>
    );
  }

  const filled = STEPS.filter((s, i) => s.framed && i < index).length + 1;
  const primaryLabel = step.id === 'Badges' ? 'Start learning' : 'Next';
  const primaryEnabled = step.optional || answer >= 0;

  return (
    <div class="glow" style={{ '--gx': '50%', '--gy': '35%', minHeight: '100dvh', display: 'flex', flexDirection: 'column' } as never}>
      <div class="row" style={{ padding: 'calc(var(--safe-top) + 8px) var(--s-xs) 0 var(--gutter)', maxWidth: 560, width: '100%', margin: '0 auto' }}>
        <div class="segments grow" role="progressbar" aria-valuemin={1} aria-valuemax={FRAMED_COUNT} aria-valuenow={filled} aria-label={`Step ${filled} of ${FRAMED_COUNT}`}>
          {Array.from({ length: FRAMED_COUNT }, (_, i) => (
            <i key={i} class={i < filled ? 'on' : ''} />
          ))}
        </div>
        <button class="text-btn muted" onClick={skip} style={{ visibility: step.skippable ? 'visible' : 'hidden' }} tabIndex={step.skippable ? 0 : -1}>
          Skip
        </button>
      </div>

      <div key={step.id} class="readable" style={{ flex: 1, padding: 'var(--s-md) var(--gutter) var(--s-lg)', animation: `${dir > 0 ? 'rise' : 'fade-in'} 240ms var(--ease-out)` }}>
        {step.id === 'Name' && (
          <div class="stack center">
            <p class="t-title-l">
              <Highlighted text="Magandang aldew! I'm Jepjep." words={['Jepjep']} />
            </p>
            <div style={{ display: 'grid', placeItems: 'center' }}>
              <Jepjep pose="with_backpack" height={Math.max(112, Math.min(220, window.innerHeight * 0.28))} breathe />
            </div>
            <Title text="What should I call you?" align="center" />
            <Body text="This is the name you'll see in the app and on the leaderboard." align="center" />
            <input
              class="input"
              style={{ borderRadius: 'var(--r-pill)', background: 'var(--surface)', textAlign: 'center' }}
              placeholder="Enter your name"
              value={name}
              maxLength={MAX_NAME_LENGTH}
              autoCapitalize="words"
              autoComplete="given-name"
              enterKeyHint="next"
              aria-label="Your name"
              onInput={(e) => setName((e.target as HTMLInputElement).value.slice(0, MAX_NAME_LENGTH))}
              onKeyDown={(e) => e.key === 'Enter' && advance()}
            />
          </div>
        )}

        {step.id === 'Greeting' && (
          <div class="stack">
            <Title text={name.trim() ? `Nice to meet you, ${name.trim()}!` : 'Nice to meet you!'} words={[name.trim() ? `${name.trim()}!` : 'you!']} />
            <Body text="I'll show you what we'll do together." />
            <div style={{ display: 'flex', justifyContent: 'flex-end', overflow: 'hidden', marginRight: 'calc(-1 * var(--gutter))' }}>
              <Jepjep pose="peeking" height={Math.min(300, window.innerHeight * 0.4)} breathe style={{ marginRight: '-8%' }} />
            </div>
          </div>
        )}

        {step.id === 'Words' && (
          <div class="stack">
            <Cropped pose="reading" align="right" />
            <Title text="Learn words from Casiguran." words={['Casiguran.']} />
            <Body text="Over 1,100 words across 12 topics, each with its Tagalog and English meaning." words={['1,100 words']} />
          </div>
        )}

        {step.id === 'Minutes' && (
          <div class="stack">
            <Cropped pose="listening" align="left" />
            <Title text="A few minutes a day" words={['minutes']} align="right" />
            <Body text="Short lessons, and reviews that bring words back right before you'd forget them." words={['Short lessons', 'reviews']} align="right" />
          </div>
        )}

        {step.id === 'Games' && (
          <div class="stack">
            <Cropped pose="playing_a_game" align="center" />
            {STORIES_ENABLED ? (
              <>
                <Title text="Play games, read stories" words={['games']} align="center" />
                <Body text="Word games, and 10 stories with Tagalog and English alongside." words={['Word games', '10 stories']} align="center" />
              </>
            ) : (
              <>
                <Title text="Play games with the words" words={['games']} align="center" />
                <Body text="Word games to practise what you learn. Folk stories are coming soon." words={['Word games']} align="center" />
              </>
            )}
          </div>
        )}

        {step.id === 'Level' && (
          <div class="stack">
            <div class="row" style={{ alignItems: 'flex-end' }}>
              <div class="grow stack-sm">
                <Title text="How much Kasiguranin do you know?" words={['Kasiguranin']} />
                <Body text="This helps Jepjep suggest a daily goal." />
              </div>
              <Jepjep pose="thinking" height={120} breathe />
            </div>
            <div class="stack-sm" role="radiogroup">
              {LEVELS.map((l, i) => (
                <Option key={l.label} label={l.label} state={level === i ? 'selected' : 'idle'} onClick={() => setLevel(i)} />
              ))}
            </div>
          </div>
        )}

        {step.id === 'Goal' && (
          <div class="stack">
            <Title text="How much time each day?" words={['time']} />
            <Body text="Pick what fits your day. The XP you earn fills your daily goal." />
            <div class="grid-2" role="radiogroup">
              {GOALS.map((g, i) => {
                const on = chosenGoal === g;
                return (
                  <button
                    key={g.label}
                    role="radio"
                    aria-checked={on}
                    onClick={() => setGoal(i)}
                    class="card"
                    style={{ textAlign: 'center', padding: 'var(--s-lg) var(--s-sm)', background: on ? 'var(--olive)' : undefined, border: on ? '2px solid var(--lime)' : undefined }}
                  >
                    <p class="t-headline">{g.minutes} min</p>
                    <p class="t-title-s">{g.label}</p>
                    <p class="t-body muted">{g.xp} XP a day</p>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {step.id === 'FirstWord' && (
          <div class="stack">
            <Title text="Your first word" words={['first']} />
            <Body text="You saw it in Jepjep's greeting. What does it mean?" />
            <div style={{ display: 'grid', placeItems: 'center', height: 'min(26vh, 200px)', overflow: 'hidden' }}>
              <Jepjep pose="pointing_a_lesson" height={Math.min(220, window.innerHeight * 0.28)} breathe />
            </div>
            <WordCard subtitle={FIRST_WORD.ipa} faint />
            <div class="grid-2" role="radiogroup">
              {FIRST_WORD.options.map((o, i) => (
                <Option
                  key={o}
                  label={o}
                  center
                  disabled={answer >= 0}
                  state={answer < 0 ? 'idle' : i === FIRST_WORD.correct ? 'correct' : i === answer ? 'wrong' : 'idle'}
                  onClick={() => answer < 0 && setAnswer(i)}
                />
              ))}
            </div>
            {answer >= 0 &&
              (answer === FIRST_WORD.correct ? (
                <Body text="That's it: aldew is the day, and the sun." words={['aldew']} />
              ) : (
                <Body text="Aldew is the day, and the sun. Water is danom." words={['Aldew']} />
              ))}
          </div>
        )}

        {step.id === 'FirstWordLearned' && (
          <div class="stack">
            <Confetti />
            <Title text={name.trim() ? `Congrats on your first word, ${name.trim()}!` : 'Congrats on your first word!'} words={['first word']} />
            <div style={{ display: 'grid', placeItems: 'center' }}>
              <Jepjep pose="celebrating" height={Math.min(220, window.innerHeight * 0.28)} breathe />
            </div>
            <WordCard subtitle={FIRST_WORD.meaning} />
            {/* Onboarding grants no XP or streak under XP policy 2; the first lesson does. */}
            <div class="row" style={{ justifyContent: 'center' }}>
              <span class="chip" style={{ background: 'var(--gold)', color: 'var(--reward-ink)', border: 0 }}>
                <Icon name="star" size={16} /> First word
              </span>
            </div>
          </div>
        )}

        {step.id === 'Install' && <InstallStep />}

        {step.id === 'Avatar' && (
          <div class="stack">
            <Title text="Choose your avatar" words={['avatar']} />
            <Body text="Pick who you'll learn as. You can change it later." />
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-md)', justifyItems: 'center' }} role="radiogroup">
              {AVATARS.map((a) => (
                <div key={a.id} class="center" style={{ display: 'grid', justifyItems: 'center', gap: 6 }}>
                  <Avatar id={a.id} size={Math.min(96, (Math.min(window.innerWidth, 480) - 80) / 3)} selected={avatar === a.id} onClick={() => setAvatar(a.id)} label={a.name} />
                  <span class="t-label" style={{ color: avatar === a.id ? 'var(--lime)' : 'var(--muted)' }}>{a.name}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {step.id === 'Badges' && (
          <div class="stack">
            <Title text="Earn badges as you learn" words={['badges']} />
            <Body text="Eleven badges, six tiers each. Every tier you earn stays earned." />
            <div class="grid-2">
              {BADGES.map((b) => (
                <div key={b.name} class="card center stack-sm" style={{ padding: 'var(--s-md) var(--s-sm)' }}>
                  <div style={{ display: 'grid', placeItems: 'center' }}>
                    <div style={{ width: 56, height: 56, borderRadius: '50%', background: `var(--tier-${b.tier})`, display: 'grid', placeItems: 'center' }}>
                      <Icon name={b.icon} size={28} color="var(--reward-ink)" />
                    </div>
                  </div>
                  <p class="t-title-s">{b.name}</p>
                  <p class="t-body-s muted">{b.condition}</p>
                  <p class="t-label-s" style={{ color: `var(--tier-${b.tier})` }}>{TIER_LABEL[b.tier]}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div class="row readable" style={{ padding: 'var(--s-xs) var(--gutter) calc(var(--safe-bottom) + var(--s-sm))' }}>
        <button class="text-btn" onClick={goBack}>
          Back
        </button>
        <div class="grow">
          <ClayButton label={primaryLabel} onClick={advance} disabled={!primaryEnabled} />
        </div>
      </div>
    </div>
  );
}
