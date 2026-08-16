// auth.js — Firebase Email/Password Auth for KasiGuru Admin Panel
import { app, auth } from './firebase-config.js';
import { 
  signInWithEmailAndPassword, 
  onAuthStateChanged, 
  signOut 
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// Helper to show/hide error message cleanly
function showLoginError(message) {
  const errEl = document.getElementById('login-error');
  const errText = document.getElementById('login-error-text');
  const btn = document.getElementById('login-btn');

  if (errText) {
    errText.textContent = message;
  } else if (errEl) {
    errEl.textContent = message;
  }

  if (errEl) errEl.style.display = 'flex';
  if (btn) {
    btn.disabled = false;
    btn.innerHTML = `<iconsax-icon name="login" type="bulk" size="18" color="#FFFFFF"></iconsax-icon> Sign In to Admin Panel`;
  }
}

function clearLoginError() {
  const errEl = document.getElementById('login-error');
  if (errEl) errEl.style.display = 'none';
}

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
