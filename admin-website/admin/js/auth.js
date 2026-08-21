// auth.js — Firebase Auth (Google OAuth & Email/Password) for KasiGuru Admin Panel
import { app, auth } from './firebase-config.js';
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

// Helper to show/hide error message cleanly
function showLoginError(message) {
  const errEl = document.getElementById('login-error');
  const errText = document.getElementById('login-error-text');
  const btn = document.getElementById('login-btn');
  const googleBtn = document.getElementById('google-login-btn');

  if (errText) {
    errText.textContent = message;
  } else if (errEl) {
    errEl.textContent = message;
  }

  if (errEl) errEl.style.display = 'flex';
  if (btn) {
    btn.disabled = false;
    btn.innerHTML = `<iconsax-icon name="login" type="bulk" size="18" color="currentColor"></iconsax-icon> Sign In with Email`;
  }
  if (googleBtn) {
    googleBtn.disabled = false;
    googleBtn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/></svg> Continue with Google`;
  }
}

function clearLoginError() {
  const errEl = document.getElementById('login-error');
  if (errEl) errEl.style.display = 'none';
}

function showLoginNotice(message) {
  const el = document.getElementById('login-notice');
  const text = document.getElementById('login-notice-text');
  if (text) text.textContent = message;
  if (el) el.style.display = 'flex';
}

function clearLoginNotice() {
  const el = document.getElementById('login-notice');
  if (el) el.style.display = 'none';
}

// Password reset. Firebase mails the link; nothing about the password passes through this page or
// through anyone administering it.
//
// The reply is deliberately the same whether or not the address has an account. Saying "no such
// user" here would turn the sign-in page into a way to test whether a given email is an admin,
// which is worth more to an attacker than the convenience is worth to us. auth/user-not-found is
// therefore reported exactly like success. Firebase rate-limits the endpoint, and that one case
// -- too-many-requests -- is surfaced, since it is about the sender rather than the account.
window.resetAdminPassword = async function () {
  const emailInput = document.getElementById('admin-email');
  const email = emailInput?.value.trim() || '';
  const link = document.getElementById('forgot-password-link');

  if (!email) {
    clearLoginNotice();
    showLoginError('Enter your admin email address above, then choose "Forgot password".');
    emailInput?.focus();
    return;
  }

  clearLoginError();
  clearLoginNotice();
  if (link) {
    link.disabled = true;
    link.textContent = 'Sending...';
  }

  const sent = 'If an admin account exists for that address, a reset link is on its way. Check your inbox, and your spam folder.';

  try {
    await sendPasswordResetEmail(auth, email);
    showLoginNotice(sent);
  } catch (err) {
    console.error('Password reset error:', err);
    if (err.code === 'auth/user-not-found' || err.code === 'auth/invalid-credential') {
      showLoginNotice(sent);
    } else if (err.code === 'auth/invalid-email') {
      showLoginError('That does not look like a valid email address.');
    } else if (err.code === 'auth/too-many-requests') {
      showLoginError('Too many reset requests. Please wait a few minutes and try again.');
    } else if (err.code === 'auth/network-request-failed') {
      showLoginError('Network connection failed. Please check your internet connection.');
    } else {
      showLoginError('Could not send the reset email. Please try again shortly.');
    }
  } finally {
    if (link) {
      link.disabled = false;
      link.textContent = 'Forgot password?';
    }
  }
};

// On the LOGIN page (index.html): if user is already logged in as admin, go straight to dashboard
onAuthStateChanged(auth, async (user) => {
  if (user) {
    try {
      const tokenResult = await user.getIdTokenResult();
      if (tokenResult.claims && tokenResult.claims.admin === true) {
        window.location.href = 'dashboard.html';
      }
    } catch (e) {
      console.warn('Silent auth check error:', e);
    }
  }
});

// Complete the redirect flow if we came back from Google.
getRedirectResult(auth)
  .then(async (result) => {
    if (!result || !result.user) return;
    const tokenResult = await result.user.getIdTokenResult(true);
    if (tokenResult.claims && tokenResult.claims.admin === true) {
      window.location.href = 'dashboard.html';
    } else {
      showLoginError(`Access Denied: Account "${result.user.email}" is not authorized as an administrator.`);
      await signOut(auth);
    }
  })
  .catch((err) => {
    console.error('Redirect result error:', err);
    showLoginError('Google Sign-In failed. Allow pop-ups for this site, then try again.');
  });

// Google Sign-In
window.signInWithGoogle = async function () {
  const googleBtn = document.getElementById('google-login-btn');
  if (googleBtn) {
    googleBtn.disabled = true;
    googleBtn.textContent = 'Connecting to Google...';
  }
  clearLoginError();

  try {
    const userCredential = await signInWithPopup(auth, googleProvider);
    const tokenResult = await userCredential.user.getIdTokenResult(true);

    if (tokenResult.claims && tokenResult.claims.admin === true) {
      window.location.href = 'dashboard.html';
    } else {
      showLoginError(`Access Denied: Account "${userCredential.user.email}" is not authorized as an administrator.`);
      await signOut(auth);
    }
  } catch (err) {
    console.error('Google sign in error:', err);
    // Chrome blocks popups on some hosts; fall back to the full-page redirect flow.
    if (err.code === 'auth/popup-blocked' || err.code === 'auth/operation-not-supported-in-this-environment') {
      try {
        await signInWithRedirect(auth, googleProvider);
        return;
      } catch (redirectErr) {
        console.error('Redirect sign in error:', redirectErr);
        showLoginError('Google Sign-In could not start. Allow pop-ups for this site, then try again.');
        return;
      }
    }

    let msg = 'Google Sign-In failed. Please try again.';
    if (err.code === 'auth/popup-closed-by-user') {
      msg = 'Sign-in cancelled.';
    } else if (err.code === 'auth/cancelled-popup-request') {
      msg = 'Only one sign-in window at a time.';
    } else if (err.code === 'auth/network-request-failed') {
      msg = 'Network connection failed. Please check your internet connection.';
    }
    showLoginError(msg);
  }
};

// Email / Password Sign-In
window.signInAdmin = async function () {
  const emailInput = document.getElementById('admin-email');
  const passwordInput = document.getElementById('admin-password');
  const email = emailInput?.value.trim() || '';
  const password = passwordInput?.value || '';
  const btn = document.getElementById('login-btn');

  if (!email || !password) {
    showLoginError('Please enter both your admin email and password.');
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Signing in...';
  }
  clearLoginError();

  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const tokenResult = await userCredential.user.getIdTokenResult(true);

    if (tokenResult.claims && tokenResult.claims.admin === true) {
      window.location.href = 'dashboard.html';
    } else {
      showLoginError(`Access Denied: Account "${email}" does not have administrator privileges.`);
      await signOut(auth);
    }
  } catch (err) {
    console.error('Sign in error:', err);
    let msg = 'Invalid email or password. Please try again.';
    if (err.code === 'auth/invalid-credential' || err.code === 'auth/wrong-password' || err.code === 'auth/user-not-found') {
      msg = 'Invalid credentials. Please verify your email and password.';
    } else if (err.code === 'auth/too-many-requests') {
      msg = 'Too many failed attempts. Please wait a moment and try again.';
    } else if (err.code === 'auth/network-request-failed') {
      msg = 'Network connection failed. Please check your internet connection.';
    }
    showLoginError(msg);
  }
};

// Allow Enter key to submit
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('admin-password')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') window.signInAdmin();
  });
  document.getElementById('admin-email')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') window.signInAdmin();
  });
});
