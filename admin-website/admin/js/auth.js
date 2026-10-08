// auth.js — the sign-in page (index.html): Google first, email and password behind a disclosure.
//
// The page has four states: checking (Firebase is still deciding whether someone is signed in, or a
// Google redirect is finishing), ready (the buttons), busy (a sign-in is in flight) and refused (a
// signed-in account that is neither an admin nor a verifier). Access itself is decided by roles.js
// and enforced by firestore.rules.
import { auth } from './firebase-config.js';
import { resolveRole } from './roles.js';
import {
  signInWithEmailAndPassword,
  sendPasswordResetEmail,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  GoogleAuthProvider,
  onAuthStateChanged,
  signOut
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

const googleProvider = new GoogleAuthProvider();
// Always offer the account chooser, so "Use a different Google account" really can pick another one
// rather than silently reusing the account that was just refused.
googleProvider.setCustomParameters({ prompt: 'select_account' });

const $ = (id) => document.getElementById(id);

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
  for (const el of [$('google-login-btn'), $('login-btn'), $('admin-email'), $('admin-password'), $('forgot-password-link')]) {
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
async function admit(user, { fresh = false, viaPassword = false } = {}) {
  if (await resolveRole(user, { forceRefresh: fresh })) {
    showChecking('Opening the console…');
    window.location.href = 'dashboard.html';
    return;
  }
  const email = user.email || 'This account';
  await signOut(auth).catch(() => {});
  setBusy(null);
  showActions();
  if (viaPassword && !user.emailVerified) {
    showError(`${email} isn't verified. Verifiers sign in with "Continue with Google".`);
  } else {
    showError(`${email} isn't an admin or verifier. Ask an admin to add it on the Team page.`, { offerOtherAccount: true });
  }
}

// ── First load ──────────────────────────────────────────────────────────────
// Wait for both the session check and any Google redirect before showing buttons, so someone who is
// already signed in never sees the form flash before being sent on.
const firstAuthState = new Promise((resolve) => {
  const stop = onAuthStateChanged(auth, (user) => { stop(); resolve(user); });
});

(async () => {
  showChecking('Checking your session…');
  let redirected = null;
  try {
    redirected = await getRedirectResult(auth);
    if (redirected?.user) showChecking('Finishing Google sign-in…');
  } catch (err) {
    console.error('Redirect result error:', err);
    showActions();
    showError('Google sign-in did not finish. Try again.');
    return;
  }
  const user = redirected?.user || await firstAuthState;
  if (user) {
    try {
      await admit(user, { fresh: !!redirected?.user });
      return;
    } catch (e) {
      console.warn('Session check failed:', e);
    }
  }
  showActions();
  // A browser that filled in saved credentials gets the password form open.
  setTimeout(() => {
    if ($('admin-email')?.value || $('admin-password')?.value) $('password-disclosure').open = true;
  }, 400);
})();

// ── Google ──────────────────────────────────────────────────────────────────

async function signInWithGoogle() {
  clearError();
  clearNotice();
  setBusy($('google-login-btn'), 'Opening Google…');
  try {
    const credential = await signInWithPopup(auth, googleProvider);
    setBusy($('google-login-btn'), 'Signing in…');
    await admit(credential.user, { fresh: true });
  } catch (err) {
    // Some browsers block pop-ups; the full-page redirect finishes in the first-load block above.
    if (err.code === 'auth/popup-blocked' || err.code === 'auth/operation-not-supported-in-this-environment') {
      try {
        await signInWithRedirect(auth, googleProvider);
        return;
      } catch (redirectErr) {
        console.error('Redirect sign in error:', redirectErr);
      }
    }
    setBusy(null);
    // Closing the Google window is a choice, not an error.
    if (err.code === 'auth/popup-closed-by-user' || err.code === 'auth/cancelled-popup-request') return;
    console.error('Google sign in error:', err);
    showError(err.code === 'auth/network-request-failed'
      ? "You're offline. Check your connection and try again."
      : 'Google sign-in did not work. Try again.');
  }
}

// ── Email and password ──────────────────────────────────────────────────────

async function signInWithPassword(e) {
  e.preventDefault();
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
    await admit(credential.user, { fresh: true, viaPassword: true });
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

$('google-login-btn').addEventListener('click', signInWithGoogle);
$('use-other-account').addEventListener('click', signInWithGoogle);
$('password-form').addEventListener('submit', signInWithPassword);
$('forgot-password-link').addEventListener('click', resetPassword);
$('toggle-password').addEventListener('click', togglePassword);
$('password-disclosure').addEventListener('toggle', (e) => {
  if (e.target.open) $('admin-email').focus();
});
