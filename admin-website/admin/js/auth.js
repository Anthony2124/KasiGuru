// auth.js — the sign-in page (index.html): Google first, email and password behind a disclosure.
//
// The page has four states: checking (Firebase is still deciding whether someone is signed in, or a
// Google redirect is finishing), ready (the buttons), busy (a sign-in is in flight) and refused (a
// signed-in account that is neither an admin nor a verifier). Access itself is decided by roles.js
// and enforced by firestore.rules.
import { auth } from './firebase-config.js';
import { resolveRole, accessCheckMessage } from './roles.js';
import {
  signInWithEmailAndPassword,
  sendPasswordResetEmail,
  signInWithPopup,
  getRedirectResult,
  GoogleAuthProvider,
  onAuthStateChanged,
  signOut
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// "Continue with Google" signs in with the usual account, as it always did. Forcing the account
// chooser on every click confused admins ("it says use another email"), so only "Use a different
// Google account", offered after a refusal, asks Google to show the chooser.
const googleProvider = new GoogleAuthProvider();
const chooserProvider = new GoogleAuthProvider();
chooserProvider.setCustomParameters({ prompt: 'select_account' });

const $ = (id) => document.getElementById(id);
// The first-load check may still be running when a person starts a new sign-in.
// Only the latest attempt may change the page or sign an account out.
let attemptId = 0;

// ── States ──────────────────────────────────────────────────────────────────

function showChecking(text) {
  $('auth-checking-text').textContent = text;
  $('auth-checking').hidden = false;
  $('login-actions').hidden = true;
}

function showActions() {
  $('auth-checking').hidden = true;
  $('login-actions').hidden = false;
}

/** Disables everything while a sign-in is in flight; [button] shows a spinner and [label]. */
function setBusy(button, label) {
  const busy = !!button;
  for (const el of [$('google-login-btn'), $('use-other-account'), $('login-btn'), $('admin-email'), $('admin-password'), $('forgot-password-link')]) {
    if (el) el.disabled = busy;
  }
  for (const [btn, idle] of [[$('google-login-btn'), 'Continue with Google'], [$('login-btn'), 'Sign in']]) {
    const text = btn?.querySelector('.btn-label');
    if (!text) continue;
    const mine = btn === button;
    text.textContent = mine ? label : idle;
    btn.classList.toggle('is-busy', mine);
    btn.setAttribute('aria-busy', String(mine));
  }
}

function showError(message, { offerOtherAccount = false } = {}) {
  clearNotice();
  $('login-error-text').textContent = message;
  $('use-other-account').hidden = !offerOtherAccount;
  const el = $('login-error');
  el.style.display = 'flex';
  el.focus({ preventScroll: false });
}

function clearError() {
  $('login-error').style.display = 'none';
  $('use-other-account').hidden = true;
}

function showNotice(message) {
  clearError();
  $('login-notice-text').textContent = message;
  $('login-notice').style.display = 'flex';
}

function clearNotice() {
  $('login-notice').style.display = 'none';
}

/** Sends a signed-in account on, or explains why it cannot come in and signs it out. */
async function admit(user, { fresh = false, viaPassword = false, attempt = attemptId } = {}) {
  const role = await resolveRole(user, { forceRefresh: fresh });
  if (attempt !== attemptId) return;
  if (role) {
    showChecking('Opening the console…');
    window.location.href = 'dashboard.html';
    return;
  }
  const email = user.email || 'This account';
  await signOut(auth).catch(() => {});
  if (attempt !== attemptId) return;
  setBusy(null);
  showActions();
  if (viaPassword && !user.emailVerified) {
    showError(`${email} isn't verified. Verifiers sign in with "Continue with Google".`);
  } else {
    showError(`${email} isn't an admin or verifier. Ask an admin to add it on the Team page.`, { offerOtherAccount: true });
  }
}

// ── First load ──────────────────────────────────────────────────────────────
// Wait briefly for the session check, so someone already signed in is sent on without the form
// flashing first. Every wait has a ceiling: a check that never answers must not strand the page on
// "Checking your session…".
//
// getRedirectResult() is awaited only when this tab actually started a Google redirect. On browsers
// that block the hidden firebaseapp.com frame it uses (third-party storage), it can wait forever,
// which is how the sign-in page got stuck on 2026-10-08.
const REDIRECT_FLAG = 'kg-admin-google-redirect';
async function withTimeout(promise, ms, fallback) {
  let timer;
  try {
    return await Promise.race([promise, new Promise((resolve) => {
      timer = setTimeout(() => resolve(fallback), ms);
    })]);
  } finally {
    clearTimeout(timer);
  }
}

const firstAuthState = new Promise((resolve) => {
  const stop = onAuthStateChanged(auth, (user) => { stop(); resolve(user); });
});

(async () => {
  const attempt = attemptId;
  showChecking('Checking your session…');
  let redirectedUser = null;
  let redirectStarted = false;
  try { redirectStarted = sessionStorage.getItem(REDIRECT_FLAG) === '1'; } catch { /* storage blocked */ }
  if (redirectStarted) {
    try { sessionStorage.removeItem(REDIRECT_FLAG); } catch { /* storage blocked */ }
    showChecking('Finishing Google sign-in…');
    try {
      redirectedUser = (await withTimeout(getRedirectResult(auth), 10000, null))?.user || null;
    } catch (err) {
      if (attempt !== attemptId) return;
      console.error('Redirect result error:', err);
      showActions();
      showError('Google sign-in did not finish. Try again.');
      return;
    }
  }
  const user = redirectedUser || await withTimeout(firstAuthState, 6000, null);
  if (attempt !== attemptId) return;
  if (user) {
    try {
      await admit(user, { fresh: !!redirectedUser, attempt });
      return;
    } catch (e) {
      if (attempt !== attemptId) return;
      console.warn('Session check failed:', e);
      showError(accessCheckMessage(e));
    }
  }
  if (attempt !== attemptId) return;
  showActions();
  // A browser that filled in saved credentials gets the password form open.
  setTimeout(() => {
    if ($('admin-email')?.value || $('admin-password')?.value) $('password-disclosure').open = true;
  }, 400);
})();

// ── Google ──────────────────────────────────────────────────────────────────

async function signInWithGoogle(provider = googleProvider) {
  if ($('google-login-btn').disabled) return;
  const attempt = ++attemptId;
  clearError();
  clearNotice();
  setBusy($('google-login-btn'), 'Opening Google…');
  try {
    const credential = await signInWithPopup(auth, provider);
    setBusy($('google-login-btn'), 'Signing in…');
    try {
      await admit(credential.user, { fresh: true, attempt });
    } catch (err) {
      setBusy(null);
      showError(accessCheckMessage(err));
    }
  } catch (err) {
    // A blocked Google window gets a second click, not a full-page redirect. The redirect's result is
    // read back through a firebaseapp.com frame, and browsers that partition third-party storage
    // (Chrome 115+, Safari, Firefox) hide it from that frame, so the trip to Google would end back
    // here signed out. The second click opens the window at once: the frame has loaded by then.
    if (err.code === 'auth/popup-blocked') {
      setBusy(null);
      showError('Your browser blocked the Google window. Choose "Continue with Google" again, or allow pop-ups for this site.');
      return;
    }
    // This portal uses Firebase's cross-origin auth domain. Redirects cannot reliably return a
    // session in browsers that block third-party storage, so give an actionable way to sign in.
    if (err.code === 'auth/operation-not-supported-in-this-environment') {
      setBusy(null);
      showError('Open this console in Chrome, Safari or Firefox to continue with Google.');
      return;
    }
    setBusy(null);
    // Closing the Google window is a choice, not an error.
    if (err.code === 'auth/popup-closed-by-user' || err.code === 'auth/cancelled-popup-request') return;
    console.error('Google sign in error:', err);
    // An unlisted host is a setup fault, not the person's: say so instead of "try again", which
    // could never succeed. (admin-wheat-nu-52-phi.vercel.app was missing until 2026-10-08.)
    showError(err.code === 'auth/network-request-failed'
      ? "You're offline. Check your connection and try again."
      : err.code === 'auth/account-exists-with-different-credential'
        ? 'This email already uses another sign-in method. Use email and password for the same address, or reset its password.'
      : err.code === 'auth/unauthorized-domain'
        ? `Google sign-in isn't enabled for ${location.hostname} yet. Use email and password, or ask the project owner to add this address in Firebase.`
        : `Google sign-in did not work${err.code ? ` (${err.code.replace('auth/', '')})` : ''}. Try again.`);
  }
}

// ── Email and password ──────────────────────────────────────────────────────

async function signInWithPassword(e) {
  e.preventDefault();
  if ($('login-btn').disabled) return;
  const attempt = ++attemptId;
  const email = $('admin-email').value.trim();
  const password = $('admin-password').value;
  if (!email || !password) {
    showError('Enter your email and password.');
    (email ? $('admin-password') : $('admin-email')).focus();
    return;
  }
  clearError();
  clearNotice();
  setBusy($('login-btn'), 'Signing in…');
  try {
    const credential = await signInWithEmailAndPassword(auth, email, password);
    try {
      await admit(credential.user, { fresh: true, viaPassword: true, attempt });
    } catch (err) {
      setBusy(null);
      showError(accessCheckMessage(err));
    }
  } catch (err) {
    console.error('Sign in error:', err);
    setBusy(null);
    $('password-disclosure').open = true;
    if (err.code === 'auth/invalid-credential' || err.code === 'auth/wrong-password' || err.code === 'auth/user-not-found' || err.code === 'auth/invalid-email') {
      showError('That email and password do not match. Check them, or use "Forgot password?".');
    } else if (err.code === 'auth/too-many-requests') {
      showError('Too many attempts. Wait a few minutes, then try again.');
    } else if (err.code === 'auth/network-request-failed') {
      showError("You're offline. Check your connection and try again.");
    } else {
      showError('Signing in did not work. Try again.');
    }
  }
}

// Password reset. Firebase mails the link; nothing about the password passes through this page.
//
// The reply is deliberately the same whether or not the address has an account. Saying "no such
// user" here would turn the sign-in page into a way to test whether a given email has portal
// access, which is worth more to an attacker than the convenience is worth to us. Firebase
// rate-limits the endpoint, and that one case -- too-many-requests -- is surfaced, since it is
// about the sender rather than the account.
async function resetPassword() {
  const emailInput = $('admin-email');
  const email = emailInput.value.trim();
  if (!email) {
    showError('Enter your email above, then choose "Forgot password?".');
    emailInput.focus();
    return;
  }
  const link = $('forgot-password-link');
  link.disabled = true;
  link.textContent = 'Sending…';
  const sent = 'If that address has an account, a reset link is on its way. Check your inbox and spam folder.';
  try {
    await sendPasswordResetEmail(auth, email);
    showNotice(sent);
  } catch (err) {
    console.error('Password reset error:', err);
    if (err.code === 'auth/user-not-found' || err.code === 'auth/invalid-credential') showNotice(sent);
    else if (err.code === 'auth/invalid-email') showError("That doesn't look like an email address.");
    else if (err.code === 'auth/too-many-requests') showError('Too many reset requests. Wait a few minutes, then try again.');
    else if (err.code === 'auth/network-request-failed') showError("You're offline. Check your connection and try again.");
    else showError("Couldn't send the reset email. Try again shortly.");
  } finally {
    link.disabled = false;
    link.textContent = 'Forgot password?';
  }
}

function togglePassword() {
  const input = $('admin-password');
  const btn = $('toggle-password');
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  btn.setAttribute('aria-pressed', String(show));
  btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
  btn.querySelector('iconsax-icon')?.setAttribute('name', show ? 'eye-slash' : 'eye');
  input.focus();
}

$('google-login-btn').addEventListener('click', () => signInWithGoogle(googleProvider));
$('use-other-account').addEventListener('click', () => signInWithGoogle(chooserProvider));
$('password-form').addEventListener('submit', signInWithPassword);
$('forgot-password-link').addEventListener('click', resetPassword);
$('toggle-password').addEventListener('click', togglePassword);
$('password-disclosure').addEventListener('toggle', (e) => {
  if (e.target.open) $('admin-email').focus();
});
