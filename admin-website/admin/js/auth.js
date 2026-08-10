// auth.js — Firebase Email/Password Auth for KasiGuru Admin Panel
import { app, auth } from './firebase-config.js';
import { 
  signInWithEmailAndPassword, 
  onAuthStateChanged, 
  signOut 
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// On the LOGIN page (index.html): if user is already logged in, go straight to dashboard
onAuthStateChanged(auth, (user) => {
  if (user) {
    window.location.href = 'dashboard.html';
  }
});

window.signInAdmin = async function () {
  const email = document.getElementById('admin-email').value.trim();
  const password = document.getElementById('admin-password').value;
  const errEl = document.getElementById('login-error');
  const btn = document.getElementById('login-btn');

  if (!email || !password) {
    errEl.textContent = 'Please enter your email and password.';
    errEl.style.display = 'flex';
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Signing in...';
  errEl.style.display = 'none';

  try {
    await signInWithEmailAndPassword(auth, email, password);
    window.location.href = 'dashboard.html';
  } catch (err) {
    let msg = 'Invalid email or password. Please try again.';
    if (err.code === 'auth/too-many-requests') {
      msg = 'Too many failed attempts. Please wait a moment and try again.';
    }
    errEl.textContent = msg;
    errEl.style.display = 'flex';
    btn.disabled = false;
    btn.textContent = 'Sign In';
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
