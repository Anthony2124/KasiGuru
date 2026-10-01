/**
 * The same Firebase project the Android app and both web portals use.
 *
 * Firestore Lite rather than the full SDK: the learner app only ever reads and writes documents on
 * demand (no realtime listeners, no offline cache - the app keeps its own), and Lite is roughly a
 * fifth of the size. That matters for an audience PRODUCT.md calls data-sensitive.
 */
import { initializeApp } from 'firebase/app';
import {
  browserLocalPersistence,
  browserPopupRedirectResolver,
  connectAuthEmulator,
  indexedDBLocalPersistence,
  initializeAuth,
} from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore/lite';

export const firebaseConfig = {
  apiKey: 'AIzaSyBIADrpzbQZpE4SoHRp9xKsh9A03RLZDlg',
  // Set VITE_AUTH_DOMAIN to this site's own domain once its /__/auth/handler is an authorised
  // redirect URI on the Google OAuth client (see docs/WEB_APP.md). vercel.json already proxies
  // /__/auth to Firebase, and a same-origin handler is what keeps Google sign-in working in
  // Safari and in an installed iPhone app, which block the cross-site storage it otherwise needs.
  authDomain: (import.meta.env.VITE_AUTH_DOMAIN as string | undefined) || 'kasiguru-86042.firebaseapp.com',
  projectId: 'kasiguru-86042',
  storageBucket: 'kasiguru-86042.firebasestorage.app',
  messagingSenderId: '25073459164',
  appId: '1:25073459164:web:95c6e2ed2d6b89af56b919',
};

export const app = initializeApp(firebaseConfig);

export const auth = initializeAuth(app, {
  persistence: [indexedDBLocalPersistence, browserLocalPersistence],
  popupRedirectResolver: browserPopupRedirectResolver,
});

export const db = getFirestore(app);

/**
 * `VITE_FIREBASE_EMULATORS=1 npm run dev` points the app at the local Auth and Firestore emulators
 * (`firebase emulators:start --only auth,firestore` from the repo root, which loads firestore.rules),
 * so testing never writes learner data into the live project.
 */
if (import.meta.env.VITE_FIREBASE_EMULATORS === '1') {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  connectFirestoreEmulator(db, '127.0.0.1', 8080);
}
