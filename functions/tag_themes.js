/**
 * Proposes a learning-tree stage and a part of speech for every vocabulary word, from what the word
 * means.
 *
 * Why this exists: two fields the corpus already carries are wrong, and both are load-bearing for the
 * learning tree.
 *
 * `category` is a set of loose import bins, not teachable themes. It files *egbër* (flight) and
 * *gabuk* (fragile) under "Colors & Shapes", and *dahil* (because), *sinu* (who?) and *dissëllad*
 * (below) under "Greetings & Essentials". A learning path built on those labels promises a subject
 * the words never deliver.
 *
 * `partOfSpeech` is worse: 1,010 of 1,246 words are marked Noun, including *angay* (go), *saneg*
 * (hear) and *tëllën* (swallow). That is why LearningTree.teachingOrder()'s noun-then-verb banding
 * does almost nothing and lessons still come out alphabetical — 81% of the corpus lands in one band.
 *
 * The fix for both is the same, and it is the one Adrian proposed: read what the word *means*.
 * `english` is populated on 1,244 of 1,246 words and `meaningEnglish` on 1,200 — the two most
 * complete fields in the record, and the only two that were written by a person describing the word
 * rather than chosen from a dropdown.
 *
 * **This script proposes; it is never authoritative.** It writes to `themeProposed` and
 * `partOfSpeechProposed`, never to `theme` or `partOfSpeech`, so a run can never overwrite a human
 * decision and can always be discarded. The admin portal's review screen is where a proposal becomes
 * a fact. A word that already carries a hand-set value, or that is marked `verifiedByAdmin`, is not
 * proposed over at all.
 *
 * Usage:
 *   node tag_themes.js <service-account.json> [--apply]
 *   node tag_themes.js --from-backup <path-to-vocabulary.json> [--show <stage>]
 *
 *   Without --apply it is a dry run: it prints per-stage counts, flags any stage under the section
 *   floor, reports the part-of-speech split, and lists what it could not place. --from-backup runs
 *   the same analysis against a local backup file, with no credentials and no network, which is the
 *   cheap way to tune the map below before touching the live corpus. --show <stage> additionally
 *   prints every word that landed in one stage, with the evidence that put it there.
 */

const fs = require('fs');

/**
 * The stages of the learning tree, and the meanings that belong to each.
 *
 * Order matters: the first stage that matches wins, so the specific ones come before the general. A
 * fisherman is a livelihood before he is a person; a fishing net is a tool before it is an object.
 * The last two stages are deliberate catch-alls — a describing word and an action that no topical
 * stage claimed still need somewhere to be taught.
 *
 * Multi-word entries and entries containing "(" match as substrings; single words match whole
 * tokens, so "ant" does not match "want" and "eat" does not match "great".
 */
const STAGES = [
  // First, because these are social and function words that later stages would otherwise grab:
  // "sorry" reads as an emotion, "visit" as an action, "who" as nothing at all. Feelings live here
  // too — on their own they are 22 words, well under the floor, and "kumusta?" answered with "I am
  // tired" is how a first unit actually teaches them.
  ['pagbati', [
    'hello', 'greeting', 'greet', 'good morning', 'good day', 'good evening', 'good night',
    'thank', 'thanks', 'sorry', 'apolog', 'please', 'welcome', 'goodbye', 'farewell', 'excuse',
    'yes', 'no', 'maybe', 'introduce', 'name is', 'how are you', 'pardon', 'congratulat',
    'invite', 'invitation', 'ask permission', 'call out', 'answer back',
    'agree', 'refuse', 'accept', 'permission', 'request', 'beg', 'bless', 'blessing',
    'respect', 'polite', 'courtesy', 'visit', 'visitor', 'company', 'gather', 'meeting',
    'promise', 'consent', 'greetings',
    // Question and function words. They have no topic, so nothing else will ever claim them, and a
    // learner cannot form a question without them.
    'who', 'what', 'when', 'why', 'how', 'which', 'whose', 'because', 'if', 'and', 'or', 'but',
    'this', 'that', 'here (', 'yes/no',
    // Feelings, folded in. See the note above.
    'happy', 'joy', 'glad', 'sad', 'sorrow', 'grief', 'angry', 'anger', 'annoy', 'afraid',
    'fear', 'scare', 'love', 'hate', 'dislike', 'want', 'wish', 'hope', 'tired',
    'sleepy', 'lazy', 'bored', 'surprise', 'shock', 'shy', 'ashamed', 'shame', 'proud', 'pride',
    'lonely', 'worry', 'anxious', 'jealous', 'envy', 'pity', 'laugh', 'smile', 'cry', 'weep',
    'feel', 'feeling', 'emotion', 'brave', 'coward', 'patient', 'kind', 'cruel', 'greedy',
    'grieve', 'mourn', 'regret', 'guilt', 'irritate', 'furious', 'frighten', 'disgust',
    'excite', 'calm', 'content', 'longing', 'homesick', 'confuse', 'courage', 'temper', 'mood',
    'grudge', 'resent', 'affection', 'fond', 'desire', 'eager', 'nervous', 'stubborn', 'selfish'
  ]],

  // Before pamilya: a fisherman is a livelihood before he is a person. Boats stay here rather than
  // in travel — Casiguran is a fishing town, and a canoe is a working tool before it is transport.
  ['kabuhayan', [
    'farmer', 'fisherman', 'fisher', 'teacher', 'carpenter', 'weaver', 'hunter', 'vendor',
    'merchant', 'trader', 'blacksmith', 'midwife', 'healer', 'priest', 'official', 'worker',
    'farm', 'field (rice', 'plow', 'harvest', 'planting', 'plant (', 'sow', 'livelihood',
    'net', 'trap', 'hook', 'bolo', 'knife', 'axe', 'hammer', 'chisel', 'rope', 'tool',
    'money', 'buy', 'sell', 'price', 'market', 'trade', 'wage', 'debt', 'earn', 'business',
    'boat', 'canoe', 'paddle', 'sail', 'raft', 'fishing', 'spear', 'hire', 'salary', 'craft'
  ]],

  // The stage the corpus has 196 words for and no theme at all. Direction and position words sit
  // here rather than with the describing words: "below", "downward" and "where" are how a learner
  // says where a thing is, which is a journey skill, not an adjective.
  ['paglalakbay', [
    'go', 'come', 'arrive', 'leave', 'depart', 'return', 'walk', 'run', 'travel', 'journey',
    'road', 'path', 'way', 'street', 'trail', 'bridge', 'ride', 'drive', 'cross', 'enter',
    'exit', 'climb', 'follow', 'pass', 'reach', 'wander', 'flee', 'escape', 'chase', 'send',
    'near', 'far', 'here', 'there', 'left', 'right (direction', 'north', 'south', 'east', 'west',
    'up', 'down', 'above', 'below', 'under', 'inside', 'outside', 'front', 'behind', 'beside',
    'between', 'across', 'around', 'toward', 'towards', 'away', 'where', 'upward', 'downward',
    'forward', 'backward', 'beneath', 'beyond', 'ahead', 'edge', 'corner', 'side', 'direction',
    'vehicle', 'cart', 'wheel', 'carry', 'bring', 'fetch', 'flight'
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
    'deaf', 'lame', 'testicle', 'genital', 'womb', 'saliva', 'tear', 'dandruff', 'bruise'
  ]],

  ['pagkain', [
    'rice', 'food', 'eat', 'meal', 'cook', 'boil', 'fry', 'roast', 'grill', 'bake', 'stew',
    'fish (food', 'meat', 'pork', 'beef', 'chicken meat', 'egg', 'vegetable', 'fruit', 'banana',
    'coconut', 'mango', 'papaya', 'taro', 'yam', 'cassava', 'corn', 'bean', 'squash', 'onion',
    'garlic', 'ginger', 'pepper', 'salt', 'sugar', 'vinegar', 'oil', 'sauce', 'soup', 'bread',
    'snack', 'drink', 'water (drink', 'coffee', 'alcohol', 'wine', 'sweet', 'sour', 'bitter',
    'salty', 'spicy', 'ripe', 'unripe', 'rotten', 'hungry', 'thirst', 'full (from eating',
    'breakfast', 'lunch', 'dinner', 'supper', 'leftover', 'chew', 'swallow', 'taste', 'feed',
    'viand', 'singe', 'porridge'
  ]],

  ['tahanan', [
    'house', 'home', 'roof', 'wall', 'door', 'window', 'floor', 'ceiling', 'post (house',
    'stair', 'ladder', 'room', 'kitchen', 'yard', 'fence', 'gate', 'bed', 'pillow', 'blanket',
    'mat', 'chair', 'bench', 'table', 'shelf', 'cabinet', 'lamp', 'candle', 'broom', 'sweep',
    'plate', 'bowl', 'cup', 'glass (drink', 'spoon', 'fork', 'pot', 'pan', 'kettle', 'jar',
    'basket', 'bucket', 'bottle', 'box', 'bag', 'clothes', 'clothing', 'dress', 'shirt',
    'trousers', 'skirt', 'hat', 'shoe', 'slipper', 'towel', 'soap', 'wash', 'laundry', 'sew',
    'needle', 'thread', 'cloth', 'comb', 'mirror', 'key', 'lock', 'necklace', 'ornament',
    'bracelet', 'earring', 'ring (jewel'
  ]],

  ['pamilya', [
    'father', 'mother', 'parent', 'child', 'son', 'daughter', 'brother', 'sister', 'sibling',
    'grandfather', 'grandmother', 'grandchild', 'grandparent', 'uncle', 'aunt', 'cousin',
    'nephew', 'niece', 'husband', 'wife', 'spouse', 'in-law', 'family', 'relative', 'ancestor',
    'baby', 'infant', 'boy', 'girl', 'man', 'woman', 'male', 'female', 'friend', 'neighbor',
    'neighbour', 'stranger', 'guest', 'person', 'people', 'orphan', 'widow', 'adopted',
    'godparent', 'namesake', 'surname', 'marry', 'wedding', 'courtship', 'youth', 'elder'
  ]],

  ['hayop', [
    'dog', 'cat', 'pig', 'chicken', 'rooster', 'hen', 'duck', 'goose', 'bird', 'snake', 'lizard',
    'frog', 'turtle', 'insect', 'ant', 'fly', 'mosquito', 'bee', 'wasp', 'spider', 'worm',
    'butterfly', 'moth', 'beetle', 'cockroach', 'louse', 'flea', 'carabao', 'cow', 'cattle',
    'goat', 'horse', 'deer', 'monkey', 'bat (animal', 'rat', 'mouse', 'snail', 'crab', 'shrimp',
    'shell', 'clam', 'fish', 'eel', 'squid', 'octopus', 'animal', 'beast', 'tail', 'wing',
    'feather', 'horn', 'claw', 'nest', 'egg (bird', 'bark (dog', 'crow (rooster', 'leech'
  ]],

  ['kalikasan', [
    'tree', 'leaf', 'branch', 'trunk', 'root', 'flower', 'seed', 'grass', 'weed', 'vine',
    'bamboo', 'forest', 'woods', 'mountain', 'hill', 'valley', 'cliff', 'cave', 'river',
    'stream', 'spring (water', 'lake', 'sea', 'ocean', 'wave', 'tide', 'beach', 'shore',
    'island', 'stone', 'rock', 'soil', 'ground', 'mud', 'sand', 'dust', 'sun', 'moon', 'star',
    'sky', 'cloud', 'rain', 'wind', 'storm', 'typhoon', 'thunder', 'lightning', 'flood',
    'earthquake', 'fire', 'smoke', 'ash', 'water', 'ice', 'fog', 'dew', 'rainbow',
    'bud', 'fruit (tree', 'world', 'weather', 'twilight', 'dawn'
  ]],

  ['bilang', [
    'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven',
    'twenty', 'hundred', 'thousand', 'number', 'count', 'first', 'second', 'third', 'half',
    'many', 'few', 'all', 'some', 'none', 'each', 'every', 'more', 'less', 'day', 'night',
    'morning', 'noon', 'afternoon', 'evening', 'week', 'month', 'year', 'hour', 'minute',
    'today', 'tomorrow', 'yesterday', 'now', 'later', 'early', 'late', 'always', 'never',
    'sometimes', 'often', 'again', 'time', 'season', 'age', 'old (person', 'young', 'how much',
    'how many'
  ]],

  // Catch-all one: a quality. Comes after every topical stage so "ripe" is food and "sick" is the
  // body, but before kilos so a describing word is not mistaken for the action it derives from.
  ['paglalarawan', [
    'big', 'large', 'small', 'little', 'tiny', 'long', 'short', 'tall', 'wide', 'narrow',
    'thick', 'thin', 'fat', 'heavy', 'light (weight', 'hard', 'soft', 'smooth', 'rough',
    'sharp', 'dull', 'hot', 'warm', 'cold', 'wet', 'dry', 'clean', 'dirty', 'new', 'old (thing',
    'good', 'bad', 'beautiful', 'ugly', 'fast', 'quick', 'slow', 'strong', 'weak',
    'deep', 'shallow', 'high', 'low', 'full', 'empty', 'straight', 'crooked', 'round',
    'flat', 'red', 'blue', 'green', 'yellow', 'black', 'white', 'brown', 'color', 'colour',
    'bright', 'dark', 'loud', 'quiet', 'true', 'false', 'wrong', 'same', 'different',
    'fragile', 'thickness', 'smell (noun'
  ]],

  // Catch-all two: an action no topical stage claimed. Cooking went to food, sweeping to the house,
  // walking to travel; what is left is the general verb vocabulary — say, think, make, help.
  ['kilos', [
    'jump', 'crawl', 'swim', 'fly (move', 'fall', 'sit', 'stand', 'lie down', 'sleep', 'wake',
    'rest', 'stop', 'wait', 'give', 'take', 'get', 'put', 'place', 'throw', 'catch', 'hold',
    'grab', 'pull', 'push', 'lift', 'drop', 'open', 'close', 'tie', 'untie', 'break', 'fix',
    'make', 'build', 'cut', 'dig', 'burn', 'clean (verb', 'look', 'see', 'watch', 'hear',
    'listen', 'touch', 'say', 'speak', 'talk', 'tell', 'ask', 'answer', 'shout', 'whisper',
    'sing', 'dance', 'play', 'work', 'help', 'find', 'search', 'hide', 'show', 'know', 'think',
    'remember', 'forget', 'learn', 'teach', 'read', 'write', 'steal', 'kill', 'hit', 'fight',
    'bite', 'kick', 'blow', 'appear', 'strike', 'grow', 'defend', 'grind'
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

/** Keys at or below this length are ignored in the definition pass. See stageFor. */
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
 * Confidence attached to a proposal, by which pass produced it.
 *
 * The gloss *is* the word's meaning; the definition merely mentions it. That difference is the whole
 * reason the two passes are kept apart, so it is carried through to the review screen: a reviewer
 * accepting a hundred proposals at once should be able to take the gloss matches on trust and read
 * the definition matches one by one.
 */
const CONFIDENCE = { GLOSS: 0.9, DEFINITION: 0.6 };

/**
 * The stage for one word, with the evidence that put it there, or null when nothing fits.
 *
 * A single-word key must match a whole token: "ant" should tag *langgam*, not *want*. A key with a
 * space is matched as a substring, which is how the disambiguating keys ("old (person", "fish (food")
 * do their work.
 */
function stageFor(word) {
  const hay = haystackFor(word);
  if (hay.trim()) {
    const tokens = new Set(hay.split(/[^a-z]+/).filter(Boolean));
    for (const [stage, keys] of STAGES) {
      for (const key of keys) {
        if (matches(hay, tokens, key)) {
          return { stage, confidence: CONFIDENCE.GLOSS, evidence: `gloss matched "${key}"` };
        }
      }
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
    for (const [stage, keys] of STAGES) {
      for (const key of keys) {
        if (key.length <= MIN_DEFINITION_KEY_LENGTH) continue;
        if (matches(def, defTokens, key)) {
          return { stage, confidence: CONFIDENCE.DEFINITION, evidence: `definition matched "${key}"` };
        }
      }
    }
  }
  return null;
}

/**
 * The part of speech for one word, read from the shape of its written definition.
 *
 * These definitions are written genus-first and to a consistent house style, which makes the opening
 * words a far better witness than the stored `partOfSpeech`: "To take in sound through the ears" is
 * a verb no matter what the import wrote, and *saneg* (hear), *angay* (go) and *tëllën* (swallow)
 * are all currently stored as nouns. 307 verbs are recoverable this way against 34 on record.
 *
 * Deliberately conservative: a definition that does not open in one of these three shapes returns
 * null rather than a guess. A wrong part of speech is worse than a missing one — the tree sorts
 * lessons by it, so a mislabelled word teaches in the wrong place.
 */
function partOfSpeechFor(word) {
  const def = (word.meaningEnglish || '').trim();
  if (!def) return null;

  if (/^to\s/i.test(def)) {
    return { pos: 'Verb', confidence: CONFIDENCE.GLOSS, evidence: 'definition opens "To …"' };
  }
  if (/^(having|being|not\s|describes?\s)/i.test(def)) {
    return { pos: 'Adjective', confidence: CONFIDENCE.GLOSS, evidence: 'definition opens with a quality' };
  }
  if (/^(a|an|the)\s/i.test(def)) {
    return { pos: 'Noun', confidence: CONFIDENCE.GLOSS, evidence: 'definition opens "A/An/The …"' };
  }
  return null;
}

/** The floor a stage must clear before its section can appear in the app. Mirrors LearningTree. */
const MIN_WORDS_FOR_SECTION = 35;

/** True when a word's existing values were set by a person and must not be proposed over. */
function isHumanHeld(word) {
  return Boolean(word.verifiedByAdmin) || Boolean((word.theme || '').trim());
}

function report(rows, showStage) {
  const counts = {};
  const byPass = { gloss: 0, definition: 0 };
  const unmatched = [];

  rows.forEach(({ id, word, proposal }) => {
    if (proposal) {
      counts[proposal.stage] = (counts[proposal.stage] || 0) + 1;
      if (proposal.confidence === CONFIDENCE.GLOSS) byPass.gloss++;
      else byPass.definition++;
    } else {
      unmatched.push(`${word.kasiguranin || id} — ${(word.english || '').slice(0, 48)}`);
    }
  });

  const tagged = rows.length - unmatched.length;
  console.log(`\nStaged ${tagged} of ${rows.length} words (${Math.round((100 * tagged) / rows.length)}%).`);
  console.log(`  ${byPass.gloss} from the gloss (confidence ${CONFIDENCE.GLOSS}), ${byPass.definition} from the definition (${CONFIDENCE.DEFINITION}).\n`);

  for (const [stage] of STAGES) {
    const n = counts[stage] || 0;
    const lessons = Math.ceil(n / 7);
    const flag = n >= MIN_WORDS_FOR_SECTION
      ? ''
      : `  << under the ${MIN_WORDS_FOR_SECTION}-word floor, stage will not ship`;
    console.log(`  ${String(n).padStart(4)}  ${stage.padEnd(14)} ${String(lessons).padStart(3)} lessons${flag}`);
  }

  console.log(`\n  ${String(unmatched.length).padStart(4)}  (unplaced — no stage claimed them)`);

  // Part of speech, the second half of the repair.
  const pos = {};
  let posNull = 0;
  let posChanged = 0;
  rows.forEach(({ word, partOfSpeech }) => {
    if (!partOfSpeech) { posNull++; return; }
    pos[partOfSpeech.pos] = (pos[partOfSpeech.pos] || 0) + 1;
    if ((word.partOfSpeech || '').trim().toLowerCase() !== partOfSpeech.pos.toLowerCase()) posChanged++;
  });
  console.log('\nPart of speech, proposed from the written definition:');
  Object.entries(pos).sort((a, b) => b[1] - a[1]).forEach(([k, v]) => console.log(`  ${String(v).padStart(4)}  ${k}`));
  console.log(`  ${String(posNull).padStart(4)}  (no proposal — definition does not open in a known shape)`);
  console.log(`\n  ${posChanged} word(s) would change part of speech.`);

  if (showStage) {
    console.log(`\n=== every word proposed for "${showStage}" ===`);
    rows
      .filter((r) => r.proposal && r.proposal.stage === showStage)
      .forEach(({ id, word, proposal }) => {
        console.log(
          `  ${(word.kasiguranin || id).padEnd(18)} ${(word.english || '').slice(0, 34).padEnd(36)} ${proposal.evidence}`
        );
      });
  }

  return { counts, unmatched };
}

async function main() {
  const args = process.argv.slice(2);
  const showFlag = args.indexOf('--show');
  const showStage = showFlag !== -1 ? args[showFlag + 1] : null;

  // Offline mode: analyse a backup file. No credentials, no network, no writes -- for tuning the map.
  const backupFlag = args.indexOf('--from-backup');
  if (backupFlag !== -1) {
    const path = args[backupFlag + 1];
    if (!path || !fs.existsSync(path)) {
      console.error('Usage: node tag_themes.js --from-backup <path-to-vocabulary.json> [--show <stage>]');
      process.exit(1);
    }
    const parsed = JSON.parse(fs.readFileSync(path, 'utf8'));
    const docs = parsed.documents || parsed;
    const rows = docs.map((d) => {
      const word = d.data || d;
      return {
        id: d.id || word.kasiguranin,
        word,
        proposal: stageFor(word),
        partOfSpeech: partOfSpeechFor(word)
      };
    });
    console.log(`Offline analysis of ${path} — nothing will be written.`);
    report(rows, showStage);
    return;
  }

  const [keyPath, ...flags] = args;
  const apply = flags.includes('--apply');

  if (!keyPath || !fs.existsSync(keyPath)) {
    console.error('Usage: node tag_themes.js <path-to-service-account.json> [--apply]');
    console.error('   or: node tag_themes.js --from-backup <path-to-vocabulary.json> [--show <stage>]');
    process.exit(1);
  }

  const admin = require('firebase-admin');
  admin.initializeApp({ credential: admin.credential.cert(require(keyPath)) });
  const db = admin.firestore();

  const snapshot = await db.collection('vocabulary').get();
  const rows = snapshot.docs.map((d) => {
    const word = d.data();
    return { id: d.id, word, proposal: stageFor(word), partOfSpeech: partOfSpeechFor(word) };
  });

  if (!apply) console.log('DRY RUN — pass --apply to write the proposal fields.');
  const { unmatched } = report(rows, showStage);

  if (!apply) {
    console.log('\nNothing written. Tune the map above, re-run, and add --apply when the counts look right.');
    return;
  }

  // Proposals only. `theme` and `partOfSpeech` are never touched here — a proposal becomes a fact in
  // the admin portal's review screen and nowhere else, which is what makes a bad run discardable
  // rather than a restore from backup.
  let written = 0;
  let heldByHuman = 0;
  let batch = db.batch();
  let inBatch = 0;

  for (const { id, word, proposal, partOfSpeech } of rows) {
    if (!proposal && !partOfSpeech) continue;
    if (isHumanHeld(word)) { heldByHuman++; continue; }

    const patch = { themeProposedAt: Date.now() };
    if (proposal) {
      patch.themeProposed = proposal.stage;
      patch.themeProposedConfidence = proposal.confidence;
      patch.themeProposedEvidence = proposal.evidence;
    }
    if (partOfSpeech) {
      patch.partOfSpeechProposed = partOfSpeech.pos;
      patch.partOfSpeechProposedConfidence = partOfSpeech.confidence;
      patch.partOfSpeechProposedEvidence = partOfSpeech.evidence;
    }

    batch.update(db.collection('vocabulary').doc(id), patch);
    written++;
    inBatch++;
    if (inBatch === 400) {
      await batch.commit();
      batch = db.batch();
      inBatch = 0;
    }
  }
  if (inBatch > 0) await batch.commit();

  console.log(`\nWrote ${written} proposal(s) to themeProposed / partOfSpeechProposed.`);
  console.log(`Left ${heldByHuman} word(s) alone — already themed by hand or marked verifiedByAdmin.`);
  console.log(`${unmatched.length} word(s) remain unplaced, by design.`);
  console.log('\nNothing is live yet. Review and accept them in the admin portal.');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
