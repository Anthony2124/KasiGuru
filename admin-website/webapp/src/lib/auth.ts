/**
 * Accounts: AuthRepository + AccountViewModel, for the browser.
 *
 * Everyone starts as an anonymous Firebase user, exactly as on Android, so progress syncs from the
 * first lesson. Adding an email or Google account links it to that same uid, which is what carries a
 * guest's progress over untouched. Signing in to an account that already exists switches uid; the
 * sync layer then merges this device's progress into that account's.
 */
import {
  EmailAuthProvider,
  GoogleAuthProvider,
  getRedirectResult,
  linkWithCredential,
  linkWithPopup,
  linkWithRedirect,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInAnonymously,
  signInWithCredential,
  signInWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  signOut as fbSignOut,
  type AuthCredential,
  type User,
} from 'firebase/auth';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore/lite';
import { auth, db } from './firebase';
import { isIOS, isStandalone } from './install';
import { getState, setState, type Account, type BanInfo } from './store';
import { adoptCloudOnNextSync, deleteCloudData, endSession, onAccountLinked, startSession, wipeLocal } from './sync';

export type AuthOutcome =
  | { kind: 'linked' }
  | { kind: 'signedIn' }
  | { kind: 'alreadyRegistered'; credential: AuthCredential }
  | { kind: 'failed'; message: string }
  /** The page is leaving for Google; the result arrives on return, through [takeRedirectOutcome]. */
  | { kind: 'redirecting' };

/**
 * Google sign-in as a full-page redirect rather than a pop-up, on iPhones and in installed apps.
 * There the pop-up comes back in a different tab, window or storage context from the one that
 * opened it (an iPhone home-screen app keeps its own storage; an installed Android app can capture
 * the return), and Firebase's handler stops with "missing initial state". A redirect stays in one
 * tab, through this site's own /__/auth handler, which is the path Firebase recommends for Safari.
 */
const prefersRedirect = () => isIOS() || isStandalone();

/** Survives the trip to Google in this tab's sessionStorage: what the return should finish. */
const REDIRECT_KEY = 'kasiguru.googleRedirect';
type RedirectMode = 'link' | 'signIn';

function takeRedirectMode(): RedirectMode | null {
  try {
    const mode = sessionStorage.getItem(REDIRECT_KEY);
    sessionStorage.removeItem(REDIRECT_KEY);
    return mode === 'link' || mode === 'signIn' ? mode : null;
  } catch {
    return null;
  }
}

function rememberRedirect(mode: RedirectMode): boolean {
  try {
    sessionStorage.setItem(REDIRECT_KEY, mode);
    return true;
  } catch {
    return false;
  }
}

// The app can render before initAuth runs (boot loads local data first), so a screen that asks
// early waits for initAuth's answer instead of reading "nothing pending".
let settleRedirect: (outcome: Promise<AuthOutcome | null>) => void = () => undefined;
let redirectOutcome = new Promise<AuthOutcome | null>((resolve) => {
  settleRedirect = resolve;
});

/** The result of a Google redirect that finished as this page loaded, once; null otherwise. */
export function takeRedirectOutcome(): Promise<AuthOutcome | null> {
  const outcome = redirectOutcome;
  redirectOutcome = Promise.resolve(null);
  return outcome;
}

function accountEmail(u: User | null): string | null {
  if (!u) return null;
  return u.email || u.providerData.find((p) => p.email)?.email || null;
}

function toAccount(u: User | null): Account {
  return {
    uid: u?.uid ?? null,
    isAnonymous: u?.isAnonymous ?? true,
    email: accountEmail(u),
    displayName: u?.displayName || u?.providerData.find((p) => p.displayName)?.displayName || null,
    providers: u?.providerData.map((p) => p.providerId).filter((p) => p !== 'firebase') ?? [],
    creationTime: u?.metadata.creationTime ? Date.parse(u.metadata.creationTime) : null,
    emailVerified: u?.emailVerified ?? false,
  };
}

function refreshAccount() {
  setState({ account: toAccount(auth.currentUser) });
}

function friendly(e: unknown): string {
  const code = (e as { code?: string })?.code ?? '';
  switch (code) {
    case 'auth/weak-password':
      return 'Password is too weak. Use at least 6 characters.';
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
    case 'auth/invalid-email':
      return 'That email or password looks incorrect.';
    case 'auth/user-not-found':
      return 'No account found for that email.';
    case 'auth/email-already-in-use':
    case 'auth/credential-already-in-use':
      return 'An account already exists for that email.';
    case 'auth/network-request-failed':
      return 'No internet connection. Please try again.';
    case 'auth/popup-closed-by-user':
    case 'auth/cancelled-popup-request':
      return 'Sign-in was cancelled.';
    case 'auth/popup-blocked':
      return 'Your browser blocked the sign-in window. Allow pop-ups for this site and try again.';
    case 'auth/unauthorized-domain':
      return 'Google sign-in is not enabled for this web address yet. Use email instead for now.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Wait a moment and try again.';
    case 'auth/requires-recent-login':
      return 'Please sign out and back in, then try again.';
    case 'auth/operation-not-allowed':
      return 'This sign-in method is not turned on for KasiGuru.';
    default:
      return (e as Error)?.message || 'Something went wrong. Please try again.';
  }
}

const isCollision = (e: unknown) => {
  const code = (e as { code?: string })?.code;
  return code === 'auth/credential-already-in-use' || code === 'auth/email-already-in-use';
};

/** True while [switchAccount] runs: the uid change it causes is handled there, not by the listener. */
let switching = false;

export function initAuth() {
  const mode = takeRedirectMode();
  if (mode) {
    // Coming back from Google. Hold the listener, as switchAccount does, until the redirect is
    // settled: a switch to an existing account must not start syncing the guest's data into it.
    switching = true;
    settleRedirect(completeRedirect(mode));
  } else {
    settleRedirect(Promise.resolve(null));
  }
  onAuthStateChanged(auth, (user) => {
    if (switching) return;
    void resume(user);
  });
}

async function resume(user: User | null) {
  if (!user) {
    // No session at all: start a guest one, as the Android app does at launch.
    try {
      await signInAnonymously(auth);
    } catch (e) {
      console.warn('anonymous sign-in failed', e);
      setState({ authReady: true, account: toAccount(null) });
    }
    return;
  }
  setState({ account: toAccount(user), authReady: true });
  void startSession(user.uid);
  void checkBan(user.uid);
}

/** Finishes what linkGoogle started before the redirect, with the outcome it would have returned. */
async function completeRedirect(mode: RedirectMode): Promise<AuthOutcome | null> {
  let outcome: AuthOutcome | null = null;
  try {
    const result = await getRedirectResult(auth);
    if (result && mode === 'signIn') return await finishSwitch();
    if (result) {
      // Same uid, now with Google on it: give the learner its identity before the session uploads.
      await result.user.reload();
      refreshAccount();
      await onAccountLinked();
      outcome = { kind: 'linked' };
    }
  } catch (e) {
    const credential = isCollision(e) ? GoogleAuthProvider.credentialFromError(e as never) : null;
    outcome = credential ? { kind: 'alreadyRegistered', credential } : { kind: 'failed', message: friendly(e) };
  }
  switching = false;
  await resume(auth.currentUser);
  return outcome;
}

export async function linkEmailPassword(email: string, password: string): Promise<AuthOutcome> {
  const credential = EmailAuthProvider.credential(email.trim(), password);
  const user = auth.currentUser;
  if (!user || !user.isAnonymous) return signInEmailPassword(email, password);
  try {
    await linkWithCredential(user, credential);
    refreshAccount();
    await onAccountLinked();
    sendEmailVerification(user).catch(() => undefined);
    return { kind: 'linked' };
  } catch (e) {
    if (isCollision(e)) return { kind: 'alreadyRegistered', credential };
    return { kind: 'failed', message: friendly(e) };
  }
}

/**
 * Signs in to an existing account so that this browser *becomes* that account.
 *
 * The account is never overwritten. Sync is held while the credential is used; once the sign-in
 * has succeeded the guest's local data is discarded without uploading, and only then is the
 * account's own copy pulled down, taken as it is. Merging the guest instead let the "About you"
 * details typed a minute earlier, stamped newer, replace the name, age, address and avatar the
 * account already had from the Android app. A failed sign-in leaves the guest exactly as it was.
 */
async function switchAccount(signIn: () => Promise<unknown>): Promise<AuthOutcome> {
  switching = true;
  endSession();
  try {
    await signIn();
  } catch (e) {
    switching = false;
    const guest = auth.currentUser;
    if (guest) void startSession(guest.uid);
    return { kind: 'failed', message: friendly(e) };
  }
  return finishSwitch();
}

/** After a successful switch: the guest's local data goes, and the account's is taken as saved. */
async function finishSwitch(): Promise<AuthOutcome> {
  try {
    await wipeLocal(false);
    adoptCloudOnNextSync();
  } finally {
    switching = false;
  }
  const user = auth.currentUser;
  setState({ account: toAccount(user), authReady: true });
  if (user) {
    void startSession(user.uid);
    void checkBan(user.uid);
  }
  return { kind: 'signedIn' };
}

export async function signInEmailPassword(email: string, password: string): Promise<AuthOutcome> {
  return switchAccount(() => signInWithEmailAndPassword(auth, email.trim(), password));
}

export async function linkGoogle(): Promise<AuthOutcome> {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  const user = auth.currentUser;
  const linking = !!user && user.isAnonymous;
  // Without the marker the return could not be finished properly, so the pop-up stays the fallback.
  if (prefersRedirect() && rememberRedirect(linking ? 'link' : 'signIn')) {
    try {
      // Both navigate away and settle only if the redirect could not start.
      if (user && linking) await linkWithRedirect(user, provider);
      else await signInWithRedirect(auth, provider);
      return { kind: 'redirecting' };
    } catch (e) {
      sessionStorage.removeItem(REDIRECT_KEY);
      return { kind: 'failed', message: friendly(e) };
    }
  }
  try {
    if (user && user.isAnonymous) {
      await linkWithPopup(user, provider);
      await user.reload();
      refreshAccount();
      await onAccountLinked();
      return { kind: 'linked' };
    }
    return await switchAccount(() => signInWithPopup(auth, provider));
  } catch (e) {
    if (isCollision(e)) {
      const credential = GoogleAuthProvider.credentialFromError(e as never);
      if (credential) return { kind: 'alreadyRegistered', credential };
    }
    return { kind: 'failed', message: friendly(e) };
  }
}

/** The learner chose to switch to the existing account; it opens as saved and replaces the guest here. */
export async function confirmSignIn(credential: AuthCredential): Promise<AuthOutcome> {
  return switchAccount(() => signInWithCredential(auth, credential));
}

export async function sendPasswordReset(email: string) {
  await sendPasswordResetEmail(auth, email.trim());
}

/**
 * Uploads, wipes this device, and returns to a fresh guest session. Refuses (throws) while the
 * rewards cannot be saved, as flushToCloud does on Android, rather than losing them.
 *
 * @param force skip the upload. Offered only after it has failed, behind a warning, so a learner
 *   who is offline (or on a day the read quota is spent) can still sign out.
 */
export async function signOutToGuest(force = false) {
  await wipeLocal(!force, !force);
  await fbSignOut(auth);
  // onAuthStateChanged(null) starts the new anonymous session.
}

export async function deleteAccount() {
  const user = auth.currentUser;
  if (!user) throw new Error('Not signed in.');
  await deleteCloudData(user.uid);
  try {
    await user.delete();
  } catch (e) {
    throw new Error(friendly(e));
  }
  await wipeLocal(false);
  await signInAnonymously(auth).catch(() => undefined);
}

export const SECURITY_QUESTIONS = [
  'What was the name of your barangay or hometown?',
  'What is the name of a family member who still speaks Kasiguranin?',
  'What was your first pet\'s name?',
];

export async function saveSecurityQuestions(answers: string[]) {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error('Not signed in.');
  const data: Record<string, unknown> = { updatedAt: Date.now() };
  answers.forEach((a, i) => (data[`answer${i}`] = a.trim()));
  await setDoc(doc(db, 'security_questions', uid), data);
}

export async function getSecurityAnswers(): Promise<string[]> {
  const uid = auth.currentUser?.uid;
  if (!uid) return SECURITY_QUESTIONS.map(() => '');
  const snap = await getDoc(doc(db, 'security_questions', uid));
  const d = snap.exists() ? snap.data() : {};
  return SECURITY_QUESTIONS.map((_, i) => (typeof d[`answer${i}`] === 'string' ? (d[`answer${i}`] as string) : ''));
}

// ── Suspension ────────────────────────────────────────────────────────────────

/** Fail-open like BanCheckRepository: a network error never locks a learner out. */
export async function checkBan(uid: string) {
  try {
    const snap = await getDoc(doc(db, 'user_bans', uid));
    const d = snap.exists() ? snap.data() : null;
    if (!d || d.isBanned !== true) {
      setState({ ban: null });
      return;
    }
    const ban: BanInfo = {
      isBanned: true,
      reason: typeof d.reason === 'string' && d.reason.trim() ? d.reason : 'Your account has been suspended. Please contact support.',
      bannedAt: Number(d.bannedAt) || 0,
      appealText: d.appealText,
      appealStatus: d.appealStatus,
      appealReviewNotes: d.appealReviewNotes,
    };
    setState({ ban });
  } catch {
    setState({ ban: null });
  }
}

export async function submitAppeal(text: string) {
  const uid = getState().account.uid;
  if (!uid) throw new Error('Not signed in.');
  await updateDoc(doc(db, 'user_bans', uid), {
    appealText: text.trim(),
    appealSubmittedAt: Date.now(),
    appealStatus: 'pending',
  });
  await checkBan(uid);
}
