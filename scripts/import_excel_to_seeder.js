const XLSX = require('xlsx');
const fs = require('fs');

const excelPath = 'C:\\Users\\U S E R - P C\\Downloads\\Database.xlsx';
const seederPath = 'C:\\KasiGuru\\KasiGuru-main\\app\\src\\main\\java\\com\\kasiguru\\data\\local\\DatabaseSeeder.kt';

try {
    const workbook = XLSX.readFile(excelPath);
    const sheet = workbook.Sheets['Sheet1'];
    const rows = XLSX.utils.sheet_to_json(sheet);

    console.log(`Read ${rows.length} entries from Excel Database.xlsx`);

    let seederContent = fs.readFileSync(seederPath, 'utf8');

    // Extract existing words in DatabaseSeeder.kt
    const existingWords = new Set();
    const wordMatches = seederContent.matchAll(/kasiguranin\s*=\s*"([^"]+)"/g);
    for (const match of wordMatches) {
        existingWords.add(match[1].toLowerCase());
    }

    console.log(`DatabaseSeeder.kt currently has ${existingWords.size} unique words.`);

    let addedCount = 0;
    const newEntries = [];

    rows.forEach(r => {
        const word = (r.KASIGURANIN || r.kasiguranin || '').trim();
        if (word && !existingWords.has(word.toLowerCase())) {
            const category = r.CATEGORY || 'Greetings & Essentials';
            const rootForm = word;
            newEntries.push(`        VocabularyEntity(
            kasiguranin = "${word}",
            tagalog = "${word}",
            english = "${word}",
            rootForm = "${rootForm}",
            category = "${category}"
        )`);
            existingWords.add(word.toLowerCase());
            addedCount++;
        }
    });

    console.log(`Adding ${addedCount} new words from Database.xlsx into DatabaseSeeder.kt...`);

    if (addedCount > 0) {
        const insertPosition = seederContent.lastIndexOf('    )');
        if (insertPosition !== -1) {
            const updatedContent = seederContent.slice(0, insertPosition) + ',\n' + newEntries.join(',\n') + '\n' + seederContent.slice(insertPosition);
            fs.writeFileSync(seederPath, updatedContent, 'utf8');
            console.log(`Successfully appended ${addedCount} words to DatabaseSeeder.kt!`);
        }
    } else {
        console.log('All words in Database.xlsx already exist in DatabaseSeeder.kt!');
    }

} catch (err) {
    console.error('Error importing Excel to seeder:', err);
}
