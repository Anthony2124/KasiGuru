/**
 * Proposes a learning-tree `theme` for every vocabulary word, from what the word means.
 *
 * Why this exists: the corpus's `category` field is a set of loose import bins, not teachable
 * themes. "Greetings & Essentials" holds *adëg* (behind), *at* (and) and *attëd* (give); "Numbers &
 * Time" holds *bangungut* (nightmare). A learning path built on those labels promises a subject the
 * words never deliver, which is exactly what a learner noticed in the first lesson. The `theme`
 * field is the second, journey-facing home for a word, and this script fills it in bulk from the
 * English and Tagalog glosses so the tagging does not have to start from 1,246 blank fields.
 *
 * This script proposes; it is not authoritative. Every word it tags can be corrected in the admin
 * portal's theme control, and every word it cannot place is left blank and reported, which is the
 * honest outcome for a word whose meaning does not belong to any section.
 *
 * Usage:
 *   node tag_themes.js <service-account.json> [--apply]
 *   node tag_themes.js --from-backup <path-to-vocabulary.json>
 *
 *   Without --apply it is a dry run: it prints the per-theme counts, flags any theme under the
 *   section floor, and lists a sample of what it could not place. --from-backup runs the same
 *   analysis against a local backup file, with no credentials and no network, which is the cheap way
 *   to tune the map below before touching the live corpus.
 */

const fs = require('fs');

/**
 * Words that must reach the app's learner, grouped by the section that should teach them.
 *
 * Order matters: the first theme that matches wins, so the specific ones come before the general. A
 * fisherman is a livelihood before he is a person; a fishing net is a tool before it is an object.
 *
 * Multi-word entries match as substrings; single words match whole tokens, so "ant" does not match
 * "want" and "eat" does not match "great".
 */
const THEMES = [
  ['pagbati', [
    'hello', 'greeting', 'greet', 'good morning', 'good day', 'good evening', 'good night',
    'thank', 'thanks', 'sorry', 'apolog', 'please', 'welcome', 'goodbye', 'farewell', 'excuse',
    'yes', 'no', 'maybe', 'introduce', 'name is', 'how are you', 'pardon', 'congratulat',
    'invite', 'invitation', 'ask permission', 'call out', 'answer back',
    'agree', 'refuse', 'accept', 'permission', 'request', 'beg', 'bless', 'blessing',
    'respect', 'polite', 'courtesy', 'visit', 'visitor', 'company', 'gather', 'meeting',
    'promise', 'consent', 'greetings'
  ]],
  ['kabuhayan', [
    'farmer', 'fisherman', 'fisher', 'teacher', 'carpenter', 'weaver', 'hunter', 'vendor',
    'merchant', 'trader', 'blacksmith', 'midwife', 'healer', 'priest', 'official', 'worker',
    'farm', 'field (rice', 'plow', 'harvest', 'planting', 'plant (', 'sow', 'livelihood',
    'net', 'trap', 'hook', 'bolo', 'knife', 'axe', 'hammer', 'chisel', 'rope', 'tool',
    'money', 'buy', 'sell', 'price', 'market', 'trade', 'wage', 'debt', 'earn', 'business',
    'boat', 'canoe', 'paddle', 'sail', 'raft', 'fishing'
  ]],
  ['katawan', [
    'head', 'skull', 'hair', 'forehead', 'eye', 'eyebrow', 'eyelash', 'ear', 'nose', 'mouth',
    'lip', 'tooth', 'teeth', 'tongue', 'throat', 'neck', 'shoulder', 'arm', 'elbow', 'wrist',
    'hand', 'finger', 'thumb', 'nail', 'chest', 'breast', 'back', 'waist', 'hip', 'stomach',
    'belly', 'navel', 'buttock', 'thigh', 'leg', 'knee', 'ankle', 'foot', 'feet', 'heel', 'toe',
    'skin', 'flesh', 'bone', 'blood', 'vein', 'heart', 'lung', 'liver', 'kidney', 'brain',
    'sick', 'illness', 'disease', 'fever', 'cough', 'cold (illness', 'wound', 'scar', 'itch',
    'pain', 'ache', 'swell', 'heal', 'medicine', 'cure', 'vomit', 'sneeze', 'breathe', 'sweat',
    'urine', 'excrement', 'sperm', 'pregnan', 'birth', 'die', 'death', 'dead', 'corpse', 'blind',
    'deaf', 'lame', 'testicle', 'genital', 'womb', 'saliva', 'tear'
  ]],
  ['pagkain', [
    'rice', 'food', 'eat', 'meal', 'cook', 'boil', 'fry', 'roast', 'grill', 'bake', 'stew',
    'fish (food', 'meat', 'pork', 'beef', 'chicken meat', 'egg', 'vegetable', 'fruit', 'banana',
    'coconut', 'mango', 'papaya', 'taro', 'yam', 'cassava', 'corn', 'bean', 'squash', 'onion',
    'garlic', 'ginger', 'pepper', 'salt', 'sugar', 'vinegar', 'oil', 'sauce', 'soup', 'bread',
    'snack', 'drink', 'water (drink', 'coffee', 'alcohol', 'wine', 'sweet', 'sour', 'bitter',
    'salty', 'spicy', 'ripe', 'unripe', 'rotten', 'hungry', 'thirst', 'full (from eating',
    'breakfast', 'lunch', 'dinner', 'supper', 'leftover', 'chew', 'swallow', 'taste', 'feed'
  ]],
  ['tahanan', [
    'house', 'home', 'roof', 'wall', 'door', 'window', 'floor', 'ceiling', 'post (house',
    'stair', 'ladder', 'room', 'kitchen', 'yard', 'fence', 'gate', 'bed', 'pillow', 'blanket',
    'mat', 'chair', 'bench', 'table', 'shelf', 'cabinet', 'lamp', 'candle', 'broom', 'sweep',
    'plate', 'bowl', 'cup', 'glass (drink', 'spoon', 'fork', 'pot', 'pan', 'kettle', 'jar',
    'basket', 'bucket', 'bottle', 'box', 'bag', 'clothes', 'clothing', 'dress', 'shirt',
    'trousers', 'skirt', 'hat', 'shoe', 'slipper', 'towel', 'soap', 'wash', 'laundry', 'sew',
    'needle', 'thread', 'cloth', 'blanket', 'comb', 'mirror', 'key', 'lock'
  ]],
  ['pamilya', [
    'father', 'mother', 'parent', 'child', 'son', 'daughter', 'brother', 'sister', 'sibling',
    'grandfather', 'grandmother', 'grandchild', 'grandparent', 'uncle', 'aunt', 'cousin',
    'nephew', 'niece', 'husband', 'wife', 'spouse', 'in-law', 'family', 'relative', 'ancestor',
    'baby', 'infant', 'boy', 'girl', 'man', 'woman', 'male', 'female', 'friend', 'neighbor',
    'neighbour', 'stranger', 'guest', 'person', 'people', 'orphan', 'widow', 'adopted',
    'godparent', 'namesake', 'surname', 'marry', 'wedding', 'courtship'
  ]],
  ['hayop', [
    'dog', 'cat', 'pig', 'chicken', 'rooster', 'hen', 'duck', 'goose', 'bird', 'snake', 'lizard',
    'frog', 'turtle', 'insect', 'ant', 'fly', 'mosquito', 'bee', 'wasp', 'spider', 'worm',
    'butterfly', 'moth', 'beetle', 'cockroach', 'louse', 'flea', 'carabao', 'cow', 'cattle',
    'goat', 'horse', 'deer', 'monkey', 'bat (animal', 'rat', 'mouse', 'snail', 'crab', 'shrimp',
    'shell', 'clam', 'fish', 'eel', 'squid', 'octopus', 'animal', 'beast', 'tail', 'wing',
    'feather', 'horn', 'claw', 'nest', 'egg (bird', 'bark (dog', 'crow (rooster'
  ]],
  ['kalikasan', [
    'tree', 'leaf', 'branch', 'trunk', 'root', 'flower', 'seed', 'grass', 'weed', 'vine',
    'bamboo', 'forest', 'woods', 'mountain', 'hill', 'valley', 'cliff', 'cave', 'river',
    'stream', 'spring (water', 'lake', 'sea', 'ocean', 'wave', 'tide', 'beach', 'shore',
    'island', 'stone', 'rock', 'soil', 'ground', 'mud', 'sand', 'dust', 'sun', 'moon', 'star',
    'sky', 'cloud', 'rain', 'wind', 'storm', 'typhoon', 'thunder', 'lightning', 'flood',
    'earthquake', 'fire', 'smoke', 'ash', 'water', 'ice', 'fog', 'dew', 'rainbow', 'east',
    'west', 'north', 'south', 'plant (', 'bud', 'fruit (tree'
  ]],
  ['bilang', [
    'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven',
    'twenty', 'hundred', 'thousand', 'number', 'count', 'first', 'second', 'third', 'half',
    'many', 'few', 'all', 'some', 'none', 'each', 'every', 'more', 'less', 'day', 'night',
    'morning', 'noon', 'afternoon', 'evening', 'week', 'month', 'year', 'hour', 'minute',
    'today', 'tomorrow', 'yesterday', 'now', 'later', 'early', 'late', 'always', 'never',
    'sometimes', 'often', 'again', 'time', 'season', 'age', 'old (person', 'young'
  ]],
  ['damdamin', [
    'happy', 'joy', 'glad', 'sad', 'sorrow', 'grief', 'angry', 'anger', 'mad', 'annoy', 'afraid',
    'fear', 'scare', 'love', 'like', 'hate', 'dislike', 'want', 'wish', 'hope', 'tired',
    'sleepy', 'lazy', 'bored', 'surprise', 'shock', 'shy', 'ashamed', 'shame', 'proud', 'pride',
    'lonely', 'worry', 'anxious', 'jealous', 'envy', 'pity', 'laugh', 'smile', 'cry', 'weep',
    'feel', 'feeling', 'emotion', 'brave', 'coward', 'patient', 'kind', 'cruel', 'greedy',
    'joyful', 'cheerful', 'grieve', 'mourn', 'sorry', 'regret', 'guilt', 'irritate', 'annoyed',
    'furious', 'terrif', 'frighten', 'disgust', 'excite', 'calm', 'content', 'satisfied',
    'longing', 'homesick', 'confuse', 'courage', 'temper', 'mood', 'grudge', 'resent',
    'affection', 'fond', 'desire', 'eager', 'nervous', 'stubborn', 'selfish', 'humble'
  ]],
  ['paglalarawan', [
    'big', 'large', 'small', 'little', 'tiny', 'long', 'short', 'tall', 'wide', 'narrow',
    'thick', 'thin', 'fat', 'heavy', 'light (weight', 'hard', 'soft', 'smooth', 'rough',
    'sharp', 'dull', 'hot', 'warm', 'cold', 'wet', 'dry', 'clean', 'dirty', 'new', 'old (thing',
    'good', 'bad', 'beautiful', 'ugly', 'fast', 'quick', 'slow', 'strong', 'weak', 'near',
    'far', 'deep', 'shallow', 'high', 'low', 'full', 'empty', 'straight', 'crooked', 'round',
    'flat', 'red', 'blue', 'green', 'yellow', 'black', 'white', 'brown', 'color', 'colour',
    'bright', 'dark', 'loud', 'quiet', 'true', 'false', 'right', 'wrong', 'same', 'different'
  ]],
  ['kilos', [
    'go', 'come', 'arrive', 'leave', 'enter', 'exit', 'walk', 'run', 'jump', 'climb', 'crawl',
    'swim', 'fly (move', 'fall', 'sit', 'stand', 'lie down', 'sleep', 'wake', 'rest', 'stop',
    'wait', 'follow', 'return', 'give', 'take', 'get', 'put', 'place', 'carry', 'bring',
    'throw', 'catch', 'hold', 'grab', 'pull', 'push', 'lift', 'drop', 'open', 'close', 'tie',
    'untie', 'break', 'fix', 'make', 'build', 'cut', 'dig', 'burn', 'wash (thing', 'clean (verb',
    'look', 'see', 'watch', 'hear', 'listen', 'smell (verb', 'touch', 'say', 'speak', 'talk',
    'tell', 'ask', 'answer', 'shout', 'whisper', 'sing', 'dance', 'play', 'work', 'help',
    'find', 'search', 'hide', 'show', 'know', 'think', 'remember', 'forget', 'learn', 'teach',
    'read', 'write', 'count (verb', 'steal', 'kill', 'hit', 'fight', 'bite', 'kick'
  ]]
];

/**
 * What a matcher may look at, lowercased.
 *
 * The short glosses only. `meaningEnglish` is a written definition -- "The solid waste the body
 * passes out", "A line of breakage in a jar or dish" -- and matching prose put 249 words into the
 * body section, because a definition that merely mentions a hand or the back is not a word about
 * the hand or the back. The gloss is what the word *is*; the definition is what it explains.
 */
function haystackFor(word) {
  return [word.english, word.tagalog]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
}

/**
 * The opening of the written definition, used as a second pass for words the glosses cannot place.
 *
 * Only the first few words, because these definitions are written genus-first -- "A large fish that
 * lives in the sea", "An ornament worn around the neck" -- so the head noun that says what the thing
 * *is* sits at the front, and everything after it is incidental. Reading the whole definition put
 * shark, necklace and durian into the body section, on "teeth", "neck" and "flesh" respectively.
 */
const DEFINITION_HEAD_WORDS = 6;

/** Keys at or below this length are ignored in the definition pass. See themeFor. */
const MIN_DEFINITION_KEY_LENGTH = 4;

function definitionFor(word) {
  return (word.meaningEnglish || '')
    .toLowerCase()
    .split(/\s+/)
    .slice(0, DEFINITION_HEAD_WORDS)
    .join(' ');
}

/** True when `key` appears in `hay` as a whole token, or as a substring for multi-word keys. */
function matches(hay, tokens, key) {
  return key.includes(' ') || key.includes('(') ? hay.includes(key) : tokens.has(key);
}

/**
 * The theme for one word, or null when nothing fits.
 *
 * A single-word key must match a whole token: "ant" should tag *langgam*, not *want*. A key with a
 * space is matched as a substring, which is how the disambiguating keys ("old (person", "fish (food")
 * do their work.
 */
function themeFor(word) {
  const hay = haystackFor(word);
  if (hay.trim()) {
    const tokens = new Set(hay.split(/[^a-z]+/).filter(Boolean));
    for (const [theme, keys] of THEMES) {
      for (const key of keys) if (matches(hay, tokens, key)) return theme;
    }
  }

  // Second pass, only for words the gloss could not place: the written definition. Kept second
  // rather than merged, because a definition mentioning a hand is weaker evidence than a gloss that
  // *is* "hand" -- merging the two put a quarter of the corpus into the body section.
  //
  // Short keys are dropped here. "Having no hair on the head" put *bald*, *naked*, *silent* and
  // *broken* into the greetings section on the strength of the word "no". A key of four characters
  // or fewer carries too little meaning to survive being read out of the middle of a sentence, and
  // the words those keys exist for -- sun, dog, eye -- are already caught by their own gloss in the
  // first pass, which is where they belong.
  const def = definitionFor(word);
  if (def.trim()) {
    const defTokens = new Set(def.split(/[^a-z]+/).filter(Boolean));
    for (const [theme, keys] of THEMES) {
      for (const key of keys) {
        if (key.length <= MIN_DEFINITION_KEY_LENGTH) continue;
        if (matches(def, defTokens, key)) return theme;
      }
    }
  }
  return null;
}

/** The floor a theme must clear before its section can appear in the app. Mirrors LearningTree. */
const MIN_WORDS_FOR_SECTION = 35;

function report(rows) {
  const counts = {};
  const unmatched = [];
  rows.forEach(({ id, word, theme }) => {
    if (theme) counts[theme] = (counts[theme] || 0) + 1;
    else unmatched.push(`${word.kasiguranin || id} — ${(word.english || '').slice(0, 48)}`);
  });

  const tagged = rows.length - unmatched.length;
  console.log(`\nTagged ${tagged} of ${rows.length} words (${Math.round((100 * tagged) / rows.length)}%).\n`);

  for (const [theme] of THEMES) {
    const n = counts[theme] || 0;
    const flag = n >= MIN_WORDS_FOR_SECTION ? '' : `  << under the ${MIN_WORDS_FOR_SECTION}-word floor, section will not ship`;
    console.log(`  ${String(n).padStart(4)}  ${theme}${flag}`);
  }

  console.log(`\n  ${String(unmatched.length).padStart(4)}  (untagged — taught in the closing section)`);
  console.log('\nA sample of what could not be placed:');
  unmatched.slice(0, 25).forEach((u) => console.log('   ', u));
  return { counts, unmatched };
}

async function main() {
  const args = process.argv.slice(2);

  // Offline mode: analyse a backup file. No credentials, no network, no writes -- for tuning the map.
  const backupFlag = args.indexOf('--from-backup');
  if (backupFlag !== -1) {
    const path = args[backupFlag + 1];
    if (!path || !fs.existsSync(path)) {
      console.error('Usage: node tag_themes.js --from-backup <path-to-vocabulary.json>');
      process.exit(1);
    }
    const parsed = JSON.parse(fs.readFileSync(path, 'utf8'));
    const docs = parsed.documents || parsed;
    const rows = docs.map((d) => {
      const word = d.data || d;
      return { id: d.id || word.kasiguranin, word, theme: themeFor(word) };
    });
    console.log(`Offline analysis of ${path} — nothing will be written.`);
    report(rows);
    return;
  }

  const [keyPath, ...flags] = args;
  const apply = flags.includes('--apply');

  if (!keyPath || !fs.existsSync(keyPath)) {
    console.error('Usage: node tag_themes.js <path-to-service-account.json> [--apply]');
    console.error('   or: node tag_themes.js --from-backup <path-to-vocabulary.json>');
    process.exit(1);
  }

  const admin = require('firebase-admin');
  admin.initializeApp({ credential: admin.credential.cert(require(keyPath)) });
  const db = admin.firestore();

  const snapshot = await db.collection('vocabulary').get();
  const rows = snapshot.docs.map((d) => ({ id: d.id, word: d.data(), theme: themeFor(d.data()) }));

  if (!apply) console.log('DRY RUN — pass --apply to write the theme field.');
  const { unmatched } = report(rows);

  if (!apply) {
    console.log('\nNothing written. Tune the map above, re-run, and add --apply when the counts look right.');
    return;
  }

  // Only words whose proposed theme differs from what is stored, so a re-run after hand-corrections
  // in the admin does not quietly overwrite them with the script's guess... except it would, which is
  // why the guard below exists: a word that already carries a theme is left alone. The admin is the
  // authority once a human has touched a word; this script only fills blanks.
  let written = 0;
  let skipped = 0;
  let batch = db.batch();
  let inBatch = 0;

  for (const { id, word, theme } of rows) {
    if (!theme) continue;
    if ((word.theme || '').trim()) { skipped++; continue; }

    batch.update(db.collection('vocabulary').doc(id), { theme, updatedAt: Date.now() });
    written++;
    inBatch++;
    if (inBatch === 400) {
      await batch.commit();
      batch = db.batch();
      inBatch = 0;
    }
  }
  if (inBatch > 0) await batch.commit();

  console.log(`\nWrote ${written} theme(s). Left ${skipped} word(s) alone because they already carry one.`);
  console.log(`${unmatched.length} word(s) remain untagged, by design.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
