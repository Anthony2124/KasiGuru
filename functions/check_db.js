const admin = require('firebase-admin');
const path = require('path');

const serviceAccountPath = process.argv[2];
if (!serviceAccountPath) {
  console.error("Please provide the path to your service account key.");
  console.error("Usage: node check_db.js C:\\KasiGuru\\firebase-service-account.json");
  process.exit(1);
}

const serviceAccount = require(path.resolve(serviceAccountPath));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkDatabase() {
  try {
    const snapshot = await db.collection('vocabulary').get();
    console.log(`\n--- DATABASE CHECK ---`);
    console.log(`Total words currently in the 'vocabulary' collection: ${snapshot.size}`);
    
    if (snapshot.size > 0) {
      console.log(`\nHere are the last 5 words added:`);
      // Sort by createdAt if available, else just take the last 5 from the snapshot
      const docs = snapshot.docs.map(doc => doc.data());
      docs.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
      const last5 = docs.slice(-5);
      
      last5.forEach(doc => {
        console.log(`- ${doc.kasiguranin || 'Unknown'} (Imported from: ${doc.importedFromExcel || 'Manual/Other'})`);
      });
    }
    console.log(`----------------------\n`);
  } catch (error) {
    console.error("Error connecting to Firestore:", error.message);
  } finally {
    process.exit(0);
  }
}

checkDatabase();
