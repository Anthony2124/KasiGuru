const XLSX = require('xlsx');

const filePath = 'C:\\Users\\U S E R - P C\\Downloads\\Database.xlsx';

try {
    const workbook = XLSX.readFile(filePath);
    const sheet = workbook.Sheets['Sheet1'];
    const data = XLSX.utils.sheet_to_json(sheet);
    
    const allKeys = new Set();
    data.forEach(row => {
        Object.keys(row).forEach(k => allKeys.add(k));
    });
    
    console.log('All unique columns found across rows:', Array.from(allKeys));
    
    // Count rows with category populated
    const withCategory = data.filter(r => r.CATEGORY);
    console.log(`Rows with CATEGORY: ${withCategory.length} / ${data.length}`);
    
    const categories = new Set(data.map(r => r.CATEGORY).filter(Boolean));
    console.log('Unique categories in Excel:', Array.from(categories));

} catch (error) {
    console.error('Error:', error);
}
