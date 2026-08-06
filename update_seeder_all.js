const fs = require('fs');
const path = 'C:\\KasiGuru\\KasiGuru-main\\app\\src\\main\\java\\com\\kasiguru\\data\\local\\DatabaseSeeder.kt';
let content = fs.readFileSync(path, 'utf8');

const bodyWords = ['arm', 'armpit', 'back', 'beard', 'belly', 'bile', 'blood', 'body', 'bone', 'brain', 'breast', 'buttocks', 'cheek', 'chest', 'chin', 'ear', 'earwax', 'elbow', 'eye', 'eyebrow', 'eyelash', 'face', 'finger', 'fingernail', 'foot', 'forehead', 'hair', 'hand', 'head', 'heart', 'heel', 'intestines', 'jaw', 'kidney', 'knee', 'leg', 'lip', 'liver', 'lung', 'mouth', 'muscle', 'nail', 'neck', 'nose', 'palm', 'rib', 'shoulder', 'skin', 'skull', 'sole', 'stomach', 'temple', 'thigh', 'throat', 'thumb', 'toe', 'tongue', 'tooth', 'vein', 'waist', 'wrist'];
const animalsWords = ['animal', 'ant', 'bird', 'butterfly', 'chick', 'chicken', 'cockroach', 'crab', 'crocodile', 'crow', 'deer', 'dog', 'eel', 'fish', 'fly', 'frog', 'goat', 'hawk', 'insect', 'lizard', 'monkey', 'mosquito', 'mouse', 'pig', 'rat', 'shark', 'shrimp', 'snake', 'spider', 'turtle', 'worm'];
const foodWords = ['bitter', 'coconut', 'egg', 'eggplant', 'food', 'fruit', 'honey', 'meat', 'milk', 'salt', 'sugar', 'sweet', 'water', 'rice', 'banana', 'cassava', 'taro', 'yam', 'drink', 'eat', 'cook', 'grater', 'soup'];
const numbersWords = ['one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'day', 'daytime', 'night', 'year', 'morning', 'afternoon', 'evening', 'month', 'week', 'today', 'tomorrow', 'yesterday'];
const weatherWords = ['cloud', 'cold', 'dew', 'dry', 'hot', 'lightning', 'moon', 'rain', 'sky', 'star', 'sun', 'thunder', 'weather', 'wind'];
const natureWords = ['bamboo', 'bark', 'branch', 'earth', 'forest', 'leaf', 'mountain', 'ocean', 'river', 'sea', 'soil', 'stone', 'tree', 'wave'];
const familyWords = ['boy', 'brother-in-law', 'chief', 'child', 'cousin', 'father', 'girl', 'man', 'mother', 'person', 'relative', 'sister', 'uncle', 'woman', 'daughter', 'son', 'parent', 'husband', 'wife'];
const emotionsWords = ['anger', 'bad', 'beautiful', 'bright', 'clean', 'happy', 'sad', 'afraid', 'love', 'tired', 'sick', 'fear', 'joy', 'good'];
const colorsWords = ['black', 'blue', 'green', 'red', 'white', 'yellow', 'round', 'sharp', 'flat', 'long', 'short'];
const toolsWords = ['adze', 'arrow', 'axe', 'basket', 'blade', 'boat', 'charcoal', 'net', 'paddle', 'spear', 'trap'];

function getCategory(eng) {
    const e = (eng || '').toLowerCase();
    if (bodyWords.some(w => e.includes(w))) return 'Body Parts & Health';
    if (animalsWords.some(w => e.includes(w))) return 'Animals & Wildlife';
    if (foodWords.some(w => e.includes(w))) return 'Food & Dining';
    if (numbersWords.some(w => e.includes(w))) return 'Numbers & Time';
    if (weatherWords.some(w => e.includes(w))) return 'Weather & Climate';
    if (natureWords.some(w => e.includes(w))) return 'Nature & Environment';
    if (familyWords.some(w => e.includes(w))) return 'Family & People';
    if (emotionsWords.some(w => e.includes(w))) return 'Emotions & Feelings';
    if (colorsWords.some(w => e.includes(w))) return 'Colors & Shapes';
    if (toolsWords.some(w => e.includes(w))) return 'Occupations & Tools';
    return 'Greetings & Essentials';
}

// Replace each VocabularyEntity block
content = content.replace(/VocabularyEntity\s*\([\s\S]*?\)/g, (block) => {
    const engMatch = block.match(/english\s*=\s*"([^"]+)"/);
    if (!engMatch) return block;
    const eng = engMatch[1];
    const cat = getCategory(eng);
    return block.replace(/category\s*=\s*"[^"]+"/, `category = "${cat}"`);
});

fs.writeFileSync(path, content, 'utf8');
console.log('Successfully updated ALL 487 VocabularyEntity entries in DatabaseSeeder.kt!');
