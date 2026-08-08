// Firebase Web SDK Setup for KasiGuru Admin Panel & App Download Store
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
  onSnapshot 
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

// Firebase Configuration (Matching Mobile App Firebase Instance)
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

export { 
  db, 
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
  onSnapshot 
};
