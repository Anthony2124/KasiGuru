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
  apiKey: "AIzaSyKasiGuruDefaultKeyWebPortalApp",
  authDomain: "kasiguru.firebaseapp.com",
  projectId: "kasiguru",
  storageBucket: "kasiguru.appspot.com",
  messagingSenderId: "100000000000",
  appId: "1:100000000000:web:kasiguruappstore12345"
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
