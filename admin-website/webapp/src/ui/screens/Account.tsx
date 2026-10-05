/**
 * Account & Sign In (ui/screens/auth/AccountScreen): the one-time "About you" step, then Google or
 * email. A guest who creates an account keeps their progress (the uid is linked, not replaced); one
 * who signs in to an existing account gets that account exactly as saved, replacing the guest here.
 */
import { useState } from 'preact/hooks';
import type { AuthCredential } from 'firebase/auth';
import {
  confirmSignIn,
  deleteAccount,
  linkEmailPassword,
  linkGoogle,
  sendPasswordReset,
  signInEmailPassword,
  signOutToGuest,
  type AuthOutcome,
} from '../../lib/auth';
import { navigate } from '../../lib/router';
import { act, useApp } from '../../lib/store';
import { ClayButton, ConfirmDialog, Dialog, GoogleG, GroundScaffold, Icon, toast } from '../kit';

const MAX_NAME = 100;
const MAX_ADDRESS = 200;

function AboutYou({ onDone }: { onDone: () => void }) {
  const p = useApp((s) => s.learner.progress);
  const [name, setName] = useState(p.fullName);
  const [age, setAge] = useState(p.age != null ? String(p.age) : '');
  const [address, setAddress] = useState(p.address);
  const [error, setError] = useState<string | null>(null);
  const save = (e: Event) => {
    e.preventDefault();
    const n = name.trim();
    const a = Number(age.trim());
    const addr = address.trim();
    const problem = !n
      ? 'Enter your name.'
      : n.length > MAX_NAME
        ? 'Your name is too long.'
        : !age.trim() || !Number.isInteger(a)
          ? 'Enter your age.'
          : a < 3 || a > 120
            ? 'Enter an age between 3 and 120.'
            : !addr
              ? 'Enter your address.'
              : addr.length > MAX_ADDRESS
                ? 'Your address is too long.'
                : null;
    if (problem) return setError(problem);
    act((d) => {
      d.d.progress.fullName = n;
      d.d.progress.age = a;
      d.d.progress.address = addr;
      d.d.progress.updatedAt = Date.now();
    });
    onDone();
  };
  return (
    <form class="stack" onSubmit={save}>
      <p class="t-label muted">Step 1 of 2 · About you</p>
      <h2 class="t-headline">Tell us about yourself</h2>
      <p class="t-body-l muted">You only need to do this once. Next, you'll sign in with Google or email.</p>
      <label class="field">
        <span>Full name</span>
        <input class="input" value={name} maxLength={MAX_NAME} autoComplete="name" onInput={(e) => setName((e.target as HTMLInputElement).value)} />
      </label>
      <label class="field">
        <span>Age</span>
        <input class="input" value={age} inputMode="numeric" maxLength={3} onInput={(e) => setAge((e.target as HTMLInputElement).value.replace(/\D/g, ''))} />
      </label>
      <label class="field">
        <span>Address</span>
        <input class="input" value={address} maxLength={MAX_ADDRESS} placeholder="e.g. Barangay, Casiguran, Aurora" autoComplete="address-level2" onInput={(e) => setAddress((e.target as HTMLInputElement).value)} />
      </label>
      {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
      <ClayButton label="Continue" type="submit" />
    </form>
  );
}

export function AccountScreen() {
  const account = useApp((s) => s.account);
  const progress = useApp((s) => s.learner.progress);
  const syncStatus = useApp((s) => s.syncStatus);
  const [signInMode, setSignInMode] = useState(progress.isOnboardingCompleted ? false : true);
  const [aboutDone, setAboutDone] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState<AuthCredential | null>(null);
  const [confirmOut, setConfirmOut] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const [saveFailed, setSaveFailed] = useState<string | null>(null);

  const runSignOut = async (force: boolean) => {
    setBusy(true);
    try {
      await signOutToGuest(force);
    } catch (e) {
      setBusy(false);
      // Not a dead end: the dialog lets the learner retry later or sign out anyway.
      setSaveFailed(e instanceof Error ? e.message : 'Your progress could not be saved.');
      return;
    }
    setBusy(false);
    toast('Signed out. Sign back in any time to restore your progress.');
    navigate('/', { replace: true });
  };

  const hasDetails = !!progress.fullName.trim() && progress.age != null && !!progress.address.trim();
  const recoverable = !!account.uid && !account.isAnonymous;

  const handle = async (run: () => Promise<AuthOutcome>) => {
    setBusy(true);
    setError(null);
    setMessage(null);
    const outcome = await run();
    setBusy(false);
    switch (outcome.kind) {
      case 'linked':
        toast('Account secured. Your progress is now saved to it.');
        break;
      case 'signedIn':
        toast('Signed in. Restoring your progress…');
        // This browser's screens were showing the guest; start over from Home as the account.
        navigate('/', { replace: true });
        break;
      case 'alreadyRegistered':
        setPending(outcome.credential);
        break;
      case 'failed':
        setError(outcome.message);
    }
  };

  const validate = () => {
    const problem = !email.trim()
      ? 'Enter your email address.'
      : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
        ? "That email doesn't look right."
        : password.length < 6
          ? 'Password must be at least 6 characters.'
          : null;
    setError(problem);
    return !problem;
  };

  if (recoverable) {
    return (
      <GroundScaffold title="Account" largeTitle subtitle="Keep your progress safe across devices">
        {confirmOut && (
          <ConfirmDialog
            title="Sign out?"
            message="Your progress is saved to your account. Sign back in on any device to pick up where you left off."
            confirmLabel="Sign out"
            danger
            onCancel={() => setConfirmOut(false)}
            onConfirm={async () => {
              setConfirmOut(false);
              await runSignOut(false);
            }}
          />
        )}
        {saveFailed && (
          <ConfirmDialog
            title="Progress not saved"
            message={`${saveFailed} If you sign out anyway, anything you did in this browser since it last synced will be lost. Everything already saved stays on your account.`}
            confirmLabel="Sign out anyway"
            danger
            onCancel={() => setSaveFailed(null)}
            onConfirm={async () => {
              setSaveFailed(null);
              await runSignOut(true);
            }}
          />
        )}
        {confirmDelete && (
          <ConfirmDialog
            title="Delete your account?"
            message="This permanently deletes your XP, streaks, badges, and word progress from KasiGuru's servers, and can't be undone."
            confirmLabel="Delete permanently"
            danger
            onCancel={() => setConfirmDelete(false)}
            onConfirm={async () => {
              setConfirmDelete(false);
              setBusy(true);
              try {
                await deleteAccount();
                toast("Account deleted. You're starting fresh as a guest.");
                navigate('/', { replace: true });
              } catch (e) {
                setError((e as Error).message || "Couldn't delete your account. Check your connection and try again.");
              } finally {
                setBusy(false);
              }
            }}
          />
        )}
        <div class="stack">
          <div class="card panel row">
            <Icon name="shieldTick" size={36} color="var(--lime)" />
            <div class="grow">
              <p class="t-title-l">Progress protected</p>
              <p class="t-body muted">
                {account.email ? `Signed in as ${account.email}` : account.providers.includes('google.com') ? 'Signed in with Google' : 'Signed in'}
              </p>
            </div>
          </div>
          <div class="card stack-sm">
            <p class="t-title-s">Your progress syncs automatically</p>
            <p class="t-body-s muted">
              XP, streaks, badges, level stars and word reviews are saved to your account and restored whenever you sign in, on this website or in the Android app.
            </p>
            <p class="t-label-s" style={{ color: syncStatus === 'error' ? 'var(--red)' : 'var(--lime)' }}>
              {syncStatus === 'syncing' ? 'Syncing…' : syncStatus === 'error' ? 'Last sync failed — it will retry' : syncStatus === 'offline' ? 'Offline — will sync when you reconnect' : 'Up to date'}
            </p>
          </div>
          {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
          <ClayButton label={busy ? 'Working…' : 'Sign out'} tone="quiet" disabled={busy} onClick={() => setConfirmOut(true)} />
          <section class="card stack-sm" style={{ borderColor: 'rgba(255,107,91,.4)', marginTop: 'var(--s-lg)' }}>
            <p class="t-label" style={{ color: 'var(--red)' }}>Danger zone</p>
            <button class="text-btn" style={{ color: 'var(--red)', fontFamily: 'var(--body)', fontWeight: 700, textAlign: 'left', padding: 0 }} disabled={busy} onClick={() => setConfirmDelete(true)}>
              Delete my account and data
            </button>
          </section>
        </div>
      </GroundScaffold>
    );
  }

  // Guest: collect the one-time details first (only when creating an account), then sign-in.
  const needAbout = !signInMode && !hasDetails && !aboutDone;

  return (
    <GroundScaffold title="Account & Sign In" largeTitle subtitle="Keep your progress safe across devices">
      {pending && (
        <Dialog label="Account already exists" onClose={() => setPending(null)}>
          <div class="stack">
            <h2 class="t-headline-s">Account already exists</h2>
            <p class="t-body-l muted">
              An account already uses those details. Signing in opens that account exactly as it is saved — its name, details and progress stay the same. The guest progress in this browser is replaced by the account's.
            </p>
            <ClayButton
              label="Sign in to that account"
              onClick={() => {
                const c = pending;
                setPending(null);
                void handle(() => confirmSignIn(c));
              }}
            />
            <ClayButton label="Cancel" tone="quiet" onClick={() => setPending(null)} />
          </div>
        </Dialog>
      )}
      <div class="readable stack">
        {needAbout ? (
          <>
            <AboutYou onDone={() => setAboutDone(true)} />
            <button class="text-btn lime" onClick={() => setSignInMode(true)}>Already gave these before? Sign in to restore them</button>
          </>
        ) : (
          <>
            {!signInMode && <p class="t-label muted">Step 2 of 2 · Sign in</p>}
            <h2 class="t-headline">{signInMode ? 'Sign in to your account' : 'Create your account'}</h2>
            <p class="t-body-l muted">
              {signInMode ? 'Sign in to restore your cloud progress, XP, streak, and badges — including progress from the Android app.' : 'Your current progress will be securely attached to your new account.'}
            </p>
            <button class="clay quiet block" disabled={busy} onClick={() => void handle(linkGoogle)}>
              <GoogleG size={20} /> Continue with Google
            </button>
            <div class="row" aria-hidden="true">
              <hr class="hairline grow" />
              <span class="t-body-s muted">or with email</span>
              <hr class="hairline grow" />
            </div>
            <form
              class="stack"
              onSubmit={(e) => {
                e.preventDefault();
                if (!validate()) return;
                void handle(() => (signInMode ? signInEmailPassword(email, password) : linkEmailPassword(email, password)));
              }}
            >
              <label class="field">
                <span>Email address</span>
                <input class="input" type="email" autoComplete="email" inputMode="email" value={email} onInput={(e) => setEmail((e.target as HTMLInputElement).value)} />
              </label>
              <label class="field">
                <span>Password</span>
                <div style={{ position: 'relative' }}>
                  <input
                    class="input"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete={signInMode ? 'current-password' : 'new-password'}
                    value={password}
                    onInput={(e) => setPassword((e.target as HTMLInputElement).value)}
                    style={{ paddingRight: 56 }}
                  />
                  <button type="button" class="icon-btn" style={{ position: 'absolute', right: 2, top: 2 }} aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(!showPassword)}>
                    <Icon name={showPassword ? 'eyeSlash' : 'eye'} size={20} color="var(--muted)" />
                  </button>
                </div>
              </label>
              {signInMode && (
                <button
                  type="button"
                  class="text-btn lime"
                  style={{ alignSelf: 'flex-start', fontSize: 14, fontFamily: 'var(--body)', fontWeight: 700, padding: 0 }}
                  onClick={async () => {
                    if (!email.trim()) return setError('Enter your email first.');
                    try {
                      await sendPasswordReset(email);
                      setMessage('Password reset email sent.');
                      setError(null);
                    } catch {
                      setError('Could not send reset email.');
                    }
                  }}
                >
                  Forgot password?
                </button>
              )}
              {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
              {message && <p class="t-body" style={{ color: 'var(--lime)' }} role="status">{message}</p>}
              <ClayButton label={busy ? 'Working…' : signInMode ? 'Sign in' : 'Create account'} type="submit" disabled={busy} />
            </form>
            <p class="t-body center">
              {signInMode ? "Don't have an account? " : 'Already have an account? '}
              <button
                class="text-btn lime"
                style={{ minHeight: 0, padding: 0, fontSize: 'inherit' }}
                onClick={() => {
                  setSignInMode(!signInMode);
                  setError(null);
                }}
              >
                {signInMode ? 'Create one' : 'Sign in'}
              </button>
            </p>
            <section class="card stack-sm">
              <p class="t-title-s">Why sync your KasiGuru account?</p>
              {[
                ['shieldTick', 'Protect your hard work', 'Your streak, XP, badges, and review progress are backed up safely.'],
                ['mobile', 'Learn on any device', 'Sign in on your phone, a tablet or the Android app and resume where you left off.'],
                ['cup', 'Compete on leaderboards', 'Share your Kasiguranin learning rank with your community.'],
              ].map(([icon, t, s]) => (
                <div key={t} class="row" style={{ alignItems: 'flex-start' }}>
                  <Icon name={icon as 'cup'} size={22} color="var(--lime)" />
                  <div>
                    <p class="t-title-s">{t}</p>
                    <p class="t-body-s muted">{s}</p>
                  </div>
                </div>
              ))}
            </section>
          </>
        )}
      </div>
    </GroundScaffold>
  );
}
