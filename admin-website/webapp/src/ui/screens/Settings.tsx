/**
 * Settings (ui/screens/settings). Android's notification switches have no reliable browser
 * equivalent, so they are replaced by the things a web learner can actually control here: sound,
 * syncing now, recovery questions, and installing the app.
 */
import { useEffect, useState } from 'preact/hooks';
import { getSecurityAnswers, saveSecurityQuestions, SECURITY_QUESTIONS } from '../../lib/auth';
import { refreshContent } from '../../lib/content';
import { navigate } from '../../lib/router';
import { setPrefs, useApp } from '../../lib/store';
import { flush } from '../../lib/sync';
import { APP_VERSION } from '../../lib/remote';
import { ClayButton, GroundScaffold, Icon, toast } from '../kit';

export function SettingsScreen() {
  const account = useApp((s) => s.account);
  const prefs = useApp((s) => s.prefs);
  const syncStatus = useApp((s) => s.syncStatus);
  const lastSyncedAt = useApp((s) => s.lastSyncedAt);
  const wordCount = useApp((s) => s.words.length);
  const [answers, setAnswers] = useState<string[] | null>(null);
  const [savingAnswers, setSavingAnswers] = useState(false);
  const recoverable = !!account.uid && !account.isAnonymous;

  useEffect(() => {
    if (!recoverable) return;
    getSecurityAnswers()
      .then(setAnswers)
      .catch(() => setAnswers(SECURITY_QUESTIONS.map(() => '')));
  }, [recoverable]);

  return (
    <GroundScaffold title="Settings" largeTitle subtitle="Sync, sound and app preferences">
      <div class="stack-lg">
        <section class="stack-sm">
          <h2 class="t-title-l">Account & sign in</h2>
          <button class="card row" onClick={() => navigate('/account')}>
            <Icon name={recoverable ? 'shieldTick' : 'danger'} size={26} color={recoverable ? 'var(--lime)' : 'var(--amber)'} />
            <div class="grow">
              <p class="t-title-s">{recoverable ? 'Progress protected' : 'Guest mode — not signed in'}</p>
              <p class="t-body-s muted">{recoverable ? account.email ?? 'Signed in' : 'Tap to sign in or create an account'}</p>
            </div>
            <Icon name="arrowRight" size={18} color="var(--faint)" />
          </button>
        </section>

        <section class="stack-sm">
          <h2 class="t-title-l">App preferences</h2>
          <div class="list">
            <label class="list-row" style={{ cursor: 'pointer' }}>
              <Icon name="volumeHigh" size={22} color="var(--info)" />
              <div class="grow">
                <p class="t-title-s">Answer sounds</p>
                <p class="t-body-s muted">A short tone for right and wrong answers</p>
              </div>
              <span class="switch">
                <input type="checkbox" checked={prefs.soundEnabled} onChange={(e) => setPrefs({ soundEnabled: (e.target as HTMLInputElement).checked })} />
                <i />
              </span>
            </label>
            <button class="list-row" onClick={() => navigate('/install')}>
              <Icon name="mobile" size={22} color="var(--lime)" />
              <div class="grow">
                <p class="t-title-s">Add to home screen</p>
                <p class="t-body-s muted">Open KasiGuru like an app, even offline</p>
              </div>
              <Icon name="arrowRight" size={18} color="var(--faint)" />
            </button>
            <button
              class="list-row"
              onClick={() => {
                setPrefs({ gameRulesSeen: [], installHintDismissed: false, backupPromptDismissed: false });
                toast('Game rules and tips will show again.');
              }}
            >
              <Icon name="refresh" size={22} color="var(--gold)" />
              <div class="grow">
                <p class="t-title-s">Show tips again</p>
                <p class="t-body-s muted">Game rules and home-screen hints</p>
              </div>
            </button>
          </div>
        </section>

        <section class="stack-sm">
          <h2 class="t-title-l">Data sync</h2>
          <div class="card stack-sm">
            <div class="row">
              <div class="grow">
                <p class="t-title-s">Sync now</p>
                <p class="t-body-s muted">Save this device's progress to your account and fetch dictionary updates</p>
              </div>
              <button
                class="clay small"
                disabled={syncStatus === 'syncing'}
                onClick={async () => {
                  await Promise.all([flush(), refreshContent(true)]);
                  toast(recoverable ? 'Progress synced with your account.' : 'Dictionary updated. Sign in to back up your progress.');
                }}
              >
                {syncStatus === 'syncing' ? 'Syncing…' : 'Sync'}
              </button>
            </div>
            <p class="t-body-s faint">
              {wordCount} words in the dictionary
              {lastSyncedAt ? ` · last synced ${new Date(lastSyncedAt).toLocaleString()}` : ''}
            </p>
          </div>
        </section>

        {recoverable && (
          <section class="stack-sm">
            <h2 class="t-title-l">Recovery questions</h2>
            <p class="t-body-s muted">A lightweight hint to help confirm it's you, not a secret. Anyone who unlocks this device can read them.</p>
            {answers == null ? (
              <p class="t-body muted">Loading…</p>
            ) : (
              <div class="card stack">
                {SECURITY_QUESTIONS.map((q, i) => (
                  <label key={q} class="field">
                    <span>{q}</span>
                    <input class="input" value={answers[i]} maxLength={100} onInput={(e) => setAnswers(answers.map((a, j) => (j === i ? (e.target as HTMLInputElement).value : a)))} />
                  </label>
                ))}
                <ClayButton
                  label={savingAnswers ? 'Saving…' : 'Save answers'}
                  tone="quiet"
                  disabled={savingAnswers}
                  onClick={async () => {
                    setSavingAnswers(true);
                    try {
                      await saveSecurityQuestions(answers);
                      toast('Recovery answers saved.');
                    } catch {
                      toast('Could not save. Check your connection.');
                    } finally {
                      setSavingAnswers(false);
                    }
                  }}
                />
              </div>
            )}
          </section>
        )}

        <section class="stack-sm">
          <h2 class="t-title-l">Help</h2>
          <div class="list">
            <button class="list-row" onClick={() => navigate(`/report?screen=${encodeURIComponent('Settings')}`)}>
              <Icon name="danger" size={22} color="var(--coral)" />
              <div class="grow">
                <p class="t-title-s">Report a bug or wrong word</p>
                <p class="t-body-s muted">Submit a glitch or a wrong translation, with a screenshot</p>
              </div>
              <Icon name="arrowRight" size={18} color="var(--faint)" />
            </button>
            <button class="list-row" onClick={() => navigate('/about')}>
              <Icon name="infoCircle" size={22} color="var(--gold)" />
              <div class="grow">
                <p class="t-title-s">About KasiGuru</p>
                <p class="t-body-s muted">KasiGuru {APP_VERSION}</p>
              </div>
              <Icon name="arrowRight" size={18} color="var(--faint)" />
            </button>
          </div>
        </section>
      </div>
    </GroundScaffold>
  );
}
