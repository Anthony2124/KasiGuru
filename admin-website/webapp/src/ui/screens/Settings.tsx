/**
 * Settings (ui/screens/settings). Android's notification switches have no reliable browser
 * equivalent, so they are replaced by the things a web learner can actually control here: appearance,
 * sound and music, the tutorial, syncing now, recovery questions, and installing the app.
 */
import { useEffect, useRef, useState } from 'preact/hooks';
import { getSecurityAnswers, saveSecurityQuestions, SECURITY_QUESTIONS } from '../../lib/auth';
import { refreshContent } from '../../lib/content';
import { navigate } from '../../lib/router';
import { canVibrate, previewSfx } from '../../lib/audio';
import { previewMusicVolume } from '../../lib/music';
import { setPrefs, useApp } from '../../lib/store';
import { flush } from '../../lib/sync';
import { APP_VERSION } from '../../lib/remote';
import { replayCoreTour } from '../../lib/tour';
import type { IconName } from '../icons.generated';
import { ClayButton, GroundScaffold, Icon, toast } from '../kit';
import { TourChapterList } from '../tour';

function SwitchRow({ icon, title, subtitle, checked, onChange }: { icon: IconName; title: string; subtitle: string; checked: boolean; onChange: (on: boolean) => void }) {
  return (
    <label class="list-row" style={{ cursor: 'pointer' }}>
      <Icon name={icon} size={22} color="var(--lime)" />
      <div class="grow">
        <p class="t-title-s">{title}</p>
        <p class="t-body-s muted">{subtitle}</p>
      </div>
      <span class="switch">
        <input type="checkbox" checked={checked} onChange={(e) => onChange((e.target as HTMLInputElement).checked)} />
        <i />
      </span>
    </label>
  );
}

/**
 * How loud a sound setting plays (VolumeSliderRow). The switch above it is the mute; this sits under
 * that row's text so the two read as one setting. Only the released value is saved.
 */
function VolumeSlider({ label, value, enabled, onSave, onPreview }: { label: string; value: number; enabled: boolean; onSave: (percent: number) => void; onPreview?: (percent: number) => void }) {
  const [current, setCurrent] = useState(value);
  const dragging = useRef(false);
  useEffect(() => {
    if (!dragging.current) setCurrent(value);
  }, [value]);
  return (
    <div class="list-row volume-row">
      <Icon name="volumeLow" size={18} color="var(--muted)" />
      <input
        type="range"
        min={0}
        max={100}
        step={5}
        value={current}
        disabled={!enabled}
        aria-label={label}
        aria-valuetext={`${current}%`}
        onInput={(e) => {
          dragging.current = true;
          const percent = Number((e.target as HTMLInputElement).value);
          setCurrent(percent);
          onPreview?.(percent);
        }}
        onChange={(e) => {
          dragging.current = false;
          onSave(Number((e.target as HTMLInputElement).value));
        }}
      />
      <Icon name="volumeHigh" size={18} color="var(--muted)" />
      <span class="t-label" style={{ minWidth: 40, textAlign: 'right', color: enabled ? 'var(--ink)' : 'var(--muted)' }}>{current}%</span>
    </div>
  );
}

export function SettingsScreen() {
  const account = useApp((s) => s.account);
  const prefs = useApp((s) => s.prefs);
  const syncStatus = useApp((s) => s.syncStatus);
  const lastSyncedAt = useApp((s) => s.lastSyncedAt);
  const wordCount = useApp((s) => s.words.length);
  const [answers, setAnswers] = useState<string[] | null>(null);
  const [savingAnswers, setSavingAnswers] = useState(false);
  const [showChapters, setShowChapters] = useState(false);
  const recoverable = !!account.uid && !account.isAnonymous;

  useEffect(() => {
    if (!recoverable) return;
    getSecurityAnswers()
      .then(setAnswers)
      .catch(() => setAnswers(SECURITY_QUESTIONS.map(() => '')));
  }, [recoverable]);

  return (
    <GroundScaffold title="Settings" largeTitle subtitle="Appearance, sound, sync and your account">
      <div class="stack-lg">
        <section class="stack-sm" data-tour="SettingsAccount">
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

        {/* AppearanceScreen's theme choice. System follows the device, as it does on Android. */}
        <section class="stack-sm" data-tour="SettingsAppearance">
          <h2 class="t-title-l">Appearance</h2>
          <div class="segmented" role="tablist" aria-label="Theme">
            {(['light', 'dark', 'system'] as const).map((t) => (
              <button key={t} role="tab" aria-selected={prefs.theme === t} onClick={() => setPrefs({ theme: t })}>
                {t === 'light' ? 'Light' : t === 'dark' ? 'Dark' : 'System'}
              </button>
            ))}
          </div>
          <p class="t-body-s muted">System follows your device's light or dark setting.</p>
        </section>

        <section class="stack-sm">
          <h2 class="t-title-l">App preferences</h2>
          <div class="list" data-tour="SettingsPreferences">
            <SwitchRow icon="volumeHigh" title="Sound effects" subtitle="Answers, wins and celebrations" checked={prefs.soundEnabled} onChange={(on) => setPrefs({ soundEnabled: on })} />
            <VolumeSlider
              label="Sound effects volume"
              value={prefs.sfxVolumePercent}
              enabled={prefs.soundEnabled}
              onSave={(percent) => {
                previewSfx(percent);
                setPrefs({ sfxVolumePercent: percent });
              }}
            />
            <SwitchRow icon="fingerTap" title="Tap sounds" subtitle="A soft click on buttons and tabs" checked={prefs.tapSoundsEnabled} onChange={(on) => setPrefs({ tapSoundsEnabled: on })} />
            <SwitchRow icon="music" title="Background music" subtitle="Music in menus and games" checked={prefs.musicEnabled} onChange={(on) => setPrefs({ musicEnabled: on })} />
            <VolumeSlider
              label="Music volume"
              value={prefs.musicVolumePercent}
              enabled={prefs.musicEnabled}
              onPreview={previewMusicVolume}
              onSave={(percent) => {
                setPrefs({ musicVolumePercent: percent });
                previewMusicVolume(null);
              }}
            />
            {canVibrate() && (
              <SwitchRow icon="flash" title="Lesson vibrations" subtitle="Feel answer feedback" checked={prefs.hapticsEnabled} onChange={(on) => setPrefs({ hapticsEnabled: on })} />
            )}
          </div>
          <div class="list">
            <button class="list-row" data-tour="SettingsReplayTutorial" onClick={replayCoreTour}>
              <Icon name="teacher" size={22} color="var(--lime)" />
              <div class="grow">
                <p class="t-title-s">Replay tutorial</p>
                <p class="t-body-s muted">Walk through the app again</p>
              </div>
            </button>
            <button class="list-row" aria-expanded={showChapters} onClick={() => setShowChapters(!showChapters)}>
              <Icon name="book" size={22} color="var(--lime)" />
              <div class="grow">
                <p class="t-title-s">Tutorial chapters</p>
                <p class="t-body-s muted">Replay a guide to any feature</p>
              </div>
              <Icon name={showChapters ? 'arrowUp' : 'arrowDown'} size={18} color="var(--faint)" />
            </button>
          </div>
          {showChapters && <TourChapterList />}
          <div class="list">
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
