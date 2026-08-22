// Firebase Web SDK Setup for KasiGuru Admin Panel (Full Read/Write + Auth)
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { 
  getFirestore, 
  collection, 
  doc, 
  getDocs, 
  getDoc, 
  setDoc, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  query, 
  orderBy, 
  where, 
  onSnapshot,
  // Bytes carries a story page illustration as raw binary. Base64 in a string field would inflate
  // every picture by a third for an audience that is explicitly data-sensitive.
  Bytes,
  writeBatch
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// Firebase Configuration
const firebaseConfig = {
  apiKey: "AIzaSyBIADrpzbQZpE4SoHRp9xKsh9A03RLZDlg",
  authDomain: "kasiguru-86042.firebaseapp.com",
  projectId: "kasiguru-86042",
  storageBucket: "kasiguru-86042.firebasestorage.app",
  messagingSenderId: "25073459164",
  appId: "1:25073459164:web:95c6e2ed2d6b89af56b919"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

export { 
  app,
  db,
  auth,
  collection, 
  doc, 
  getDocs, 
  getDoc, 
  setDoc, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  query, 
  orderBy, 
  where, 
  onSnapshot,
  Bytes,
  writeBatch
};
