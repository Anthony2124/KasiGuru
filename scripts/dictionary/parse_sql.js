const fs = require('fs');
const { dictionaryData, downloadsFile } = require('../lib/project-paths');

const sqlPath = process.argv[2] || downloadsFile('kasiguranin_migration.sql');

try {
  const content = fs.readFileSync(sqlPath, 'utf8');
  const insertRegex = /INSERT INTO vocabulary \([^)]+\)\s+VALUES \('([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)'\);/g;

  const entries = [];
  let match;

  while ((match = insertRegex.exec(content)) !== null) {
    const clean = (val) => (val === 'nan' ? '' : val.trim());

    entries.push({
      kasiguranin: clean(match[1]),
      tagalog: clean(match[2]),
      english: clean(match[3]),
      rootWord: clean(match[4]),
      partOfSpeech: clean(match[5]),
      category: clean(match[6]) || 'General',
      audioFile: clean(match[7]),
      sampleSentence: clean(match[8]),
      ipaNotation: ''
    });
  }

  console.log(`Parsed ${entries.length} valid vocabulary records from SQL migration file.`);
  
  const outputPath = dictionaryData('kasiguranin_vocabulary_seed.json');
  fs.writeFileSync(outputPath, JSON.stringify(entries, null, 2));
  console.log(`Saved clean JSON seed data to ${outputPath}`);
} catch (e) {
  console.error("Error parsing SQL file:", e.message);
}
