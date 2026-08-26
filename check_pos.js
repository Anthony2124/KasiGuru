const { initializeApp } = require("firebase/app");
const { getFirestore, collection, getDocs, limit, query } = require("firebase/firestore");

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

async function check() {
  const q = query(collection(db, "vocabulary"));
  const snap = await getDocs(q);
  console.log(`Total docs: ${snap.size}`);
  let posCount = 0;
  let sample = [];
  snap.forEach(doc => {
    const data = doc.data();
    if (data.partOfSpeech) {
      posCount++;
    }
    if (sample.length < 5) {
      sample.push({ kasiguranin: data.kasiguranin, english: data.english, pos: data.partOfSpeech });
    }
  });
  console.log(`Docs with partOfSpeech set: ${posCount}`);
  console.log("Sample docs:", sample);
  process.exit(0);
}

check();
