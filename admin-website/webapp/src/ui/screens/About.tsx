/**
 * About KasiGuru, How to use it, and how to install it on a phone.
 */
import { useState } from 'preact/hooks';
import { useInstall, isIOSSafari } from '../../lib/install';
import { navigate } from '../../lib/router';
import { useApp } from '../../lib/store';
import { APP_VERSION } from '../../lib/remote';
import { ClayButton, GroundScaffold, Icon, Jepjep, Wordmark } from '../kit';
import type { IconName } from '../icons.generated';

function Faq({ q, a }: { q: string; a: string }) {
  const [open, setOpen] = useState(false);
  return (
    <div class="card">
      <button class="row" style={{ width: '100%', textAlign: 'left' }} aria-expanded={open} onClick={() => setOpen(!open)}>
        <span class="t-title-s grow">{q}</span>
        <Icon name={open ? 'arrowUp' : 'arrowDown'} size={18} color="var(--muted)" />
      </button>
      {open && <p class="t-body-l muted" style={{ marginTop: 8 }}>{a}</p>}
    </div>
  );
}

export function AboutScreen() {
  const words = useApp((s) => s.words.length);
  return (
    <GroundScaffold title="About KasiGuru">
      <div class="readable stack-lg">
        <section class="card panel glow center stack" style={{ padding: 'var(--s-xl) var(--s-lg)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Wordmark width={200} />
          </div>
          <p class="t-body-l muted">Preserving Casiguran, Aurora's heritage</p>
          <p class="t-body-l">
            A gamified learning app for the preservation and learning of the Kasiguranin language, built as the software artifact of an undergraduate thesis.
          </p>
          <span class="tag" style={{ alignSelf: 'center', margin: '0 auto' }}>{APP_VERSION}</span>
        </section>
        <section class="stack-sm">
          <h2 class="t-title-l">The Kasiguranin people & language</h2>
          {[
            ['Geographic location', 'Casiguran is a coastal municipality in northern Aurora province, Luzon, bounded by the Sierra Madre to the west and the Pacific Ocean to the east.'],
            ['Language family & contact', 'Kasiguranin belongs to the Northern Luzon sub-branch of Malayo-Polynesian languages, with long historical contact with Casiguran Dumagat Agta.'],
            ['Grammar features', 'A rich aspectual verb system (neutral, imperfective, perfective, contemplative), glottal stops ʔ, long vowels ː, and predicate-initial word order.'],
            ['Preservation', "Documented by UP Diliman linguistics research ('A Grammatical Sketch of Kasiguranin', Supnet, 2016). KasiGuru helps keep the language alive for future generations."],
          ].map(([t, d]) => (
            <div key={t} class="card">
              <p class="t-title-s">{t}</p>
              <p class="t-body muted" style={{ marginTop: 4 }}>{d}</p>
            </div>
          ))}
        </section>
        <section class="stack-sm">
          <h2 class="t-title-l">Frequently asked questions</h2>
          <Faq q="What is KasiGuru?" a="KasiGuru teaches and preserves the Kasiguranin language of Casiguran, Aurora, through lessons, spaced-repetition review, word games and stories." />
          <Faq q="Is this the same as the Android app?" a="Yes. The website is the same app for iPhone, iPad and computers. Sign in with the same account on both and your XP, streak, badges and word reviews follow you." />
          <Faq q="Does it work offline?" a={`Once opened online, the ${words.toLocaleString()} dictionary words, lessons and games keep working offline in this browser. Progress syncs to your account when you reconnect. Pronunciation clips play offline once you have heard them.`} />
          <Faq q="Where does the language data come from?" a="The corpus is based on the 2016 UP Diliman thesis 'A Grammatical Sketch of Kasiguranin' by Chiara Paola E. Supnet, extended by community contributions that moderators review." />
          <Faq q="How do I report corrections or missing words?" a="Use 'Contribute a word' in Me, or 'Report an issue with this word' on any dictionary entry." />
        </section>
        <div class="stack-sm">
          <ClayButton label="Contribute a word" tone="quiet" onClick={() => navigate('/submit-word')} />
          <ClayButton label="Get the Android app" tone="quiet" onClick={() => window.open('https://kasiguru-download.vercel.app', '_blank', 'noopener')} />
        </div>
      </div>
    </GroundScaffold>
  );
}

const TABS_HELP: [IconName, string, string][] = [
  ['home', 'Home', "Your one next step, today's goal, words due for review, stories and your week."],
  ['teacher', 'Learn', 'The path through the language, section by section. Finish a lesson to open the next; each section ends with a mastery check.'],
  ['game', 'Practice', 'Flashcard review, mini-games that earn stars and XP, and the leaderboard.'],
  ['book', 'Library', 'The whole dictionary with meanings and recordings, and the stories.'],
  ['profile', 'Me', 'Your stats, badges, account and settings.'],
];

export function HelpScreen() {
  return (
    <GroundScaffold title="How to use KasiGuru" largeTitle subtitle="A short guide to every tab">
      <div class="readable stack-lg">
        <div class="list">
          {TABS_HELP.map(([icon, t, d]) => (
            <div key={t} class="list-row" style={{ alignItems: 'flex-start' }}>
              <span class="ico"><Icon name={icon} size={20} color="var(--lime)" /></span>
              <div>
                <p class="t-title-s">{t}</p>
                <p class="t-body muted">{d}</p>
              </div>
            </div>
          ))}
        </div>
        <section class="card stack-sm">
          <h2 class="t-title">Keeping your streak</h2>
          <p class="t-body-l muted">A day counts toward your streak once you finish the review deck and play three mini-game levels. Your daily goal ring fills with the XP you earn.</p>
        </section>
        <section class="card stack-sm">
          <h2 class="t-title">How reviews work</h2>
          <p class="t-body-l muted">Every answer schedules the word with SuperMemo-2 spaced repetition, so it comes back just before you would forget it. A word counts as learned after three correct recalls spread over at least six days.</p>
        </section>
        <section class="card stack-sm">
          <h2 class="t-title">Using both apps</h2>
          <p class="t-body-l muted">Sign in with the same Google account or email on Android and here. Progress merges both ways, keeping the higher value of every stat.</p>
        </section>
      </div>
    </GroundScaffold>
  );
}

export function InstallScreen() {
  const install = useInstall();
  return (
    <GroundScaffold title="Install the app" largeTitle subtitle="Open KasiGuru from your home screen, full screen and offline">
      <div class="readable stack-lg">
        <div style={{ display: 'grid', placeItems: 'center' }}>
          <Jepjep pose="sitting" height={150} breathe />
        </div>
        {install.installed ? (
          <div class="banner info">
            <Icon name="tickCircle" size={22} color="var(--lime)" />
            <p class="t-body-l">You're already using KasiGuru as an installed app.</p>
          </div>
        ) : null}
        <section class="stack-sm">
          <h2 class="t-title-l">iPhone & iPad</h2>
          <ol class="list" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            {[
              ['share', "Open this site in Safari and tap Share (the square with an arrow)."],
              ['addCircle', 'Scroll down and choose Add to Home Screen.'],
              ['tickCircle', 'Tap Add. KasiGuru appears on your home screen with its own icon.'],
            ].map(([icon, t], i) => (
              <li key={i} class="list-row">
                <span class="ico"><Icon name={icon as IconName} size={20} color="var(--lime)" /></span>
                <span class="t-body-l">{t}</span>
              </li>
            ))}
          </ol>
          {install.ios && !isIOSSafari() && <p class="t-body faint">You're in another browser: only Safari can add apps to the home screen on iPhone.</p>}
        </section>
        <section class="stack-sm">
          <h2 class="t-title-l">Android & computers</h2>
          {install.canPrompt ? (
            <ClayButton label="Install KasiGuru" icon="mobile" onClick={() => void install.prompt()} />
          ) : (
            <p class="t-body-l muted">In Chrome or Edge, open the browser menu and choose Install app (or Add to Home screen). On Android you can also install the full app from the download page.</p>
          )}
          <ClayButton label="Android app download page" tone="quiet" onClick={() => window.open('https://kasiguru-download.vercel.app', '_blank', 'noopener')} />
        </section>
      </div>
    </GroundScaffold>
  );
}
