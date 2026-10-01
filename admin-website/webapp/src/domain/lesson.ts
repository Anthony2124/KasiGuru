/**
 * The lesson system: LessonPlan, LearningTree, Interleaving, ExpandingRehearsal, Remediation,
 * SentenceBank, ExerciseGenerator and the tree assembly in LessonRepository.
 *
 * A lesson's identity is `(unitId, lessonIndex)` and its words are a positional slice of the unit's
 * teaching order, so that order must be computed exactly as on Android (code-unit string order, not
 * locale collation) or the same lesson index would teach different words on the two apps.
 */
import type { Corpus } from './corpus';
import { distinctBy, randomOf, shuffled } from './random';
import { meaningFor } from './recall';
import { isLeech, MIN_LEARNED_REVIEWS } from './sm2';
import type { LessonState, Word } from './types';
import { lessonKey } from './merge';

// ── LessonPlan ───────────────────────────────────────────────────────────────

export const WORDS_PER_LESSON = 7;
export const EXERCISES_PER_LESSON = 11;
export const XP_PER_LESSON = 20;
export const XP_PERFECT_BONUS = 5;

export const lessonCountFor = (wordCount: number) =>
  wordCount <= 0 ? 0 : Math.floor((wordCount + WORDS_PER_LESSON - 1) / WORDS_PER_LESSON);

/** [start, endExclusive) of a lesson's slice, or null past the end. */
export function wordIndicesFor(lessonIndex: number, wordCount: number): [number, number] | null {
  const start = lessonIndex * WORDS_PER_LESSON;
  if (start >= wordCount) return null;
  return [start, Math.min(start + WORDS_PER_LESSON, wordCount)];
}

export const xpForLesson = (accuracy: number) => XP_PER_LESSON + (accuracy >= 1 ? XP_PERFECT_BONUS : 0);

// ── LearningTree ─────────────────────────────────────────────────────────────

export const MIN_WORDS_FOR_SECTION = 35;
export const CORE_LESSONS_PER_STAGE = 6;
export const GATE_FRACTION = 0.6;
export const GATE_XP_CAP = 300;
export const MASTERY_WORD_COUNT = 10;
export const REMAINDER_UNIT_ID = 'theme:_remainder';
export const MASTERY_UNIT_PREFIX = 'mastery:';

export enum Mastery {
  NONE = 0,
  FAMILIAR = 1,
  PRACTICING = 2,
  MASTERED = 3,
}

export interface SectionDefinition {
  id: string;
  title: string;
  gloss: string;
  journeyLine: string;
  /** A theme tag, or null for the remainder section. */
  theme: string | null;
}

export const SECTIONS: SectionDefinition[] = [
  { id: 'pagbati', title: 'Pagbati at Sarili', gloss: 'Greetings, courtesy and how you feel', journeyLine: 'Say hello, and be understood', theme: 'pagbati' },
  { id: 'pamilya', title: 'Pamilya at Mga Tao', gloss: 'Family and people', journeyLine: 'Meet the household', theme: 'pamilya' },
  { id: 'tahanan', title: 'Tahanan', gloss: 'The house and what is in it', journeyLine: 'Step inside a Casiguran home', theme: 'tahanan' },
  { id: 'pagkain', title: 'Pagkain at Kainan', gloss: 'Food and eating', journeyLine: 'Share a meal', theme: 'pagkain' },
  { id: 'paglalakbay', title: 'Paglalakbay at Direksyon', gloss: 'Travel, direction and position', journeyLine: 'Find your way, and say where things are', theme: 'paglalakbay' },
  { id: 'katawan', title: 'Katawan at Kalusugan', gloss: 'The body and health', journeyLine: 'Say how you feel, and where it hurts', theme: 'katawan' },
  { id: 'kalikasan', title: 'Kalikasan at Panahon', gloss: 'Land, sea, sky and weather', journeyLine: 'Read the land, the sea and the sky', theme: 'kalikasan' },
  { id: 'hayop', title: 'Mga Hayop', gloss: 'Animals and wildlife', journeyLine: 'Name what lives here', theme: 'hayop' },
  { id: 'bilang', title: 'Bilang at Oras', gloss: 'Numbers and time', journeyLine: 'Count, and tell the time', theme: 'bilang' },
  { id: 'kabuhayan', title: 'Kabuhayan', gloss: 'Work, tools and livelihood', journeyLine: 'Learn how people live', theme: 'kabuhayan' },
  { id: 'kilos', title: 'Mga Kilos', gloss: 'Actions and doing', journeyLine: 'Say what people do', theme: 'kilos' },
  { id: 'paglalarawan', title: 'Paglalarawan', gloss: 'Describing words', journeyLine: 'Describe what you see', theme: 'paglalarawan' },
  { id: 'araw_araw', title: 'Pang-araw-araw', gloss: 'Everything else people say', journeyLine: 'Everything else people actually say', theme: null },
];

export const themeUnitId = (tag: string) => `theme:${tag.trim().toLowerCase()}`;
export const unitIdFor = (s: SectionDefinition) => (s.theme ? themeUnitId(s.theme) : REMAINDER_UNIT_ID);
export const masteryUnitId = (sectionId: string) => `${MASTERY_UNIT_PREFIX}${sectionId}`;

export function sectionIdForMasteryUnit(unitId: string): string | null {
  if (!unitId.startsWith(MASTERY_UNIT_PREFIX)) return null;
  const id = unitId.slice(MASTERY_UNIT_PREFIX.length);
  return id.trim() ? id : null;
}

export function sectionForUnit(unitId: string): SectionDefinition | undefined {
  const mastery = sectionIdForMasteryUnit(unitId);
  if (mastery) return SECTIONS.find((s) => s.id.toLowerCase() === mastery.toLowerCase());
  return SECTIONS.find((s) => unitIdFor(s).toLowerCase() === unitId.toLowerCase());
}

export const isViable = (wordCount: number) => wordCount >= MIN_WORDS_FOR_SECTION;

export function requiredXpToOpenNext(lessonNodeCount: number): number {
  const available = lessonNodeCount * XP_PER_LESSON;
  return Math.min(Math.trunc(available * GATE_FRACTION), GATE_XP_CAP);
}

export function masteryOf(w: Word): Mastery {
  if (w.isLearned) return Mastery.MASTERED;
  if (w.timesReviewed >= MIN_LEARNED_REVIEWS) return Mastery.PRACTICING;
  if (w.timesReviewed >= 1) return Mastery.FAMILIAR;
  return Mastery.NONE;
}

const codeUnitCompare = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

function posBand(partOfSpeech: string): number {
  switch (partOfSpeech.trim().toLowerCase()) {
    case 'noun':
      return 0;
    case 'verb':
      return 1;
    case 'adjective':
      return 2;
    case 'adverb':
      return 3;
    default:
      return 4;
  }
}

export function teachingOrder(words: Word[]): Word[] {
  return words
    .slice()
    .sort((a, b) => posBand(a.partOfSpeech) - posBand(b.partOfSpeech) || codeUnitCompare(a.kasiguranin.toLowerCase(), b.kasiguranin.toLowerCase()));
}

export function masterySelection(coreWords: Word[]): Word[] {
  return coreWords
    .slice()
    .sort((a, b) => masteryOf(a) - masteryOf(b))
    .slice(0, MASTERY_WORD_COUNT);
}

/** Which lessons can be opened: every finished one, and the next one in line per tier. */
export function openLessons(completed: boolean[], coreCount: number): boolean[] {
  const core = Math.max(0, Math.min(coreCount, completed.length));
  let coreNextOpen = true;
  let deepDiveNextOpen = completed.slice(0, core).every(Boolean);
  return completed.map((done, i) => {
    if (done) return true;
    if (i < core) {
      const open = coreNextOpen;
      coreNextOpen = false;
      return open;
    }
    const open = deepDiveNextOpen;
    deepDiveNextOpen = false;
    return open;
  });
}

export function nodeMastery(isLessonComplete: boolean, words: Word[]): Mastery {
  if (!isLessonComplete) return Mastery.NONE;
  if (!words.length) return Mastery.FAMILIAR;
  const mastered = words.filter((w) => w.isLearned).length;
  if (mastered >= words.length * 0.8) return Mastery.MASTERED;
  const practising = words.filter((w) => masteryOf(w) >= Mastery.PRACTICING).length;
  if (practising >= words.length * 0.5) return Mastery.PRACTICING;
  return Mastery.FAMILIAR;
}

export interface LessonRef {
  unitId: string;
  lessonIndex: number;
}

export type TreeNode = { kind: 'lesson'; ref: LessonRef; positionInSection: number } | { kind: 'mastery'; sectionId: string };

export interface TreeNodeState {
  node: TreeNode;
  title: string;
  mastery: Mastery;
  isUnlocked: boolean;
  isCurrent: boolean;
  isDeepDive: boolean;
}

export interface TreeSection {
  definition: SectionDefinition;
  wordCount: number;
  nodes: TreeNodeState[];
  earnedXp: number;
  requiredXp: number;
  isUnlocked: boolean;
}

export const sectionIsComplete = (s: TreeSection) => {
  const core = s.nodes.filter((n) => !n.isDeepDive);
  return core.length > 0 && core.every((n) => n.mastery >= Mastery.FAMILIAR);
};
export const sectionOpensNext = (s: TreeSection) => s.earnedXp >= s.requiredXp;
export const sectionGateFraction = (s: TreeSection) =>
  s.requiredXp <= 0 ? 1 : Math.min(1, Math.max(0, s.earnedXp / s.requiredXp));
export const deepDiveCount = (s: TreeSection) => s.nodes.filter((n) => n.isDeepDive).length;

export const nodeKey = (n: TreeNodeState) =>
  n.node.kind === 'lesson' ? `${n.node.ref.unitId}#${n.node.ref.lessonIndex}` : `mastery#${n.node.sectionId}`;

// ── LessonRepository ─────────────────────────────────────────────────────────

/** The corpus grouped by the unit key a lesson is recorded against, in section order. */
export function wordsByUnit(corpus: Corpus): Map<string, Word[]> {
  const byTheme = new Map<string, Word[]>();
  for (const w of corpus.all) {
    if (!w.theme.trim()) continue;
    const tag = w.theme.trim().toLowerCase();
    if (!byTheme.has(tag)) byTheme.set(tag, []);
    byTheme.get(tag)!.push(w);
  }
  const shipping = SECTIONS.filter((s) => s.theme && isViable(byTheme.get(s.theme)?.length ?? 0)).map((s) => s.theme!);
  const shippingSet = new Set(shipping);
  const units = new Map<string, Word[]>();
  for (const tag of shipping) units.set(themeUnitId(tag), teachingOrder(byTheme.get(tag) ?? []));
  const remainder = corpus.all.filter((w) => {
    const tag = w.theme.trim().toLowerCase();
    return !tag || !shippingSet.has(tag);
  });
  units.set(REMAINDER_UNIT_ID, teachingOrder(remainder));
  return units;
}

/**
 * What keeps a section open regardless of today's XP gate (LessonRepository.treeSections under
 * policy 2): an `access` receipt for `section:<id>`, saved the first time it was seen open, and,
 * for learners who had lessons before normalization, the gate measured with the old lesson XP.
 */
export interface TreeAccess {
  savedAccess: ReadonlySet<string>;
  /** Imported lesson receipts: source (`lesson:<unit>#<index>`) to their XP, 20 or 25. */
  importedLessons: Readonly<Record<string, number>>;
}

const NO_ACCESS: TreeAccess = { savedAccess: new Set(), importedLessons: {} };

export function buildTree(corpus: Corpus, lessons: Record<string, LessonState>, access: TreeAccess = NO_ACCESS): TreeSection[] {
  const units = wordsByUnit(corpus);
  let previousOpensNext: boolean = true;
  let legacyOpensNext = true;
  const hasImported = Object.keys(access.importedLessons).length > 0;
  const out: TreeSection[] = [];
  for (const def of SECTIONS) {
    const unitId = unitIdFor(def);
    const words = units.get(unitId) ?? [];
    if (!isViable(words.length)) continue;

    const isUnlocked: boolean =
      previousOpensNext || access.savedAccess.has(`section:${def.id}`) || (hasImported && legacyOpensNext);
    const nodes = buildNodes(def, unitId, words, lessons, isUnlocked);
    let earnedXp = 0;
    for (const [key, state] of Object.entries(lessons)) {
      if (state.isComplete && key.slice(0, key.lastIndexOf('#')) === unitId) earnedXp += xpForLesson(state.bestAccuracy);
    }
    const section: TreeSection = {
      definition: def,
      wordCount: words.length,
      nodes,
      earnedXp,
      requiredXp: requiredXpToOpenNext(nodes.filter((n) => n.node.kind === 'lesson' && !n.isDeepDive).length),
      isUnlocked,
    };
    previousOpensNext = isUnlocked && sectionOpensNext(section);
    // The pre-normalization gate: 30 XP a lesson, 45 when perfect, against 60% of 30 per lesson.
    let legacyXp = 0;
    for (const key of Object.keys(lessons)) {
      const imported = access.importedLessons[`lesson:${key}`];
      if (imported !== undefined && key.slice(0, key.lastIndexOf('#')) === unitId) legacyXp += 30 + (imported === 25 ? 15 : 0);
    }
    const coreLessons = nodes.filter((n) => n.node.kind === 'lesson' && !n.isDeepDive).length;
    legacyOpensNext = legacyOpensNext && legacyXp >= Math.min(Math.trunc(coreLessons * 30 * GATE_FRACTION), GATE_XP_CAP);
    out.push(section);
  }
  return out;
}

/** The `section:<id>` access keys a tree shows open, for preserving them as receipts. */
export const openSectionKeys = (tree: TreeSection[]) => tree.filter((s) => s.isUnlocked).map((s) => `section:${s.definition.id}`);

function buildNodes(
  def: SectionDefinition,
  unitId: string,
  unitWords: Word[],
  lessons: Record<string, LessonState>,
  isSectionUnlocked: boolean
): TreeNodeState[] {
  const planned = Array.from({ length: lessonCountFor(unitWords.length) }, (_, index) => {
    const range = wordIndicesFor(index, unitWords.length);
    return {
      ref: { unitId, lessonIndex: index },
      isComplete: lessons[lessonKey(unitId, index)]?.isComplete === true,
      words: range ? unitWords.slice(range[0], range[1]) : [],
    };
  });
  const open = openLessons(
    planned.map((p) => p.isComplete),
    CORE_LESSONS_PER_STAGE
  );

  const core: TreeNodeState[] = [];
  const deep: TreeNodeState[] = [];
  let currentMarked = false;
  planned.forEach((lesson, i) => {
    const position = i + 1;
    const isDeepDive = position > CORE_LESSONS_PER_STAGE;
    const isCurrent = isSectionUnlocked && !lesson.isComplete && !currentMarked && !isDeepDive;
    if (isCurrent) currentMarked = true;
    const state: TreeNodeState = {
      node: { kind: 'lesson', ref: lesson.ref, positionInSection: position },
      title: `Lesson ${position}`,
      mastery: nodeMastery(lesson.isComplete, lesson.words),
      isUnlocked: isSectionUnlocked && open[i],
      isCurrent,
      isDeepDive,
    };
    (isDeepDive ? deep : core).push(state);
  });

  const coreDone = core.length > 0 && core.every((n) => n.mastery >= Mastery.FAMILIAR);
  const checkpointDone = lessons[lessonKey(masteryUnitId(def.id), 0)]?.isComplete === true;
  const checkpoint: TreeNodeState = {
    node: { kind: 'mastery', sectionId: def.id },
    title: 'Mastery',
    mastery: checkpointDone ? Mastery.MASTERED : Mastery.NONE,
    isUnlocked: isSectionUnlocked && coreDone,
    isCurrent: isSectionUnlocked && coreDone && !checkpointDone && !currentMarked,
    isDeepDive: false,
  };
  return [...core, checkpoint, ...deep];
}

/** The words that define a lesson (stable; the Learn screen counts them). */
export function wordsFor(corpus: Corpus, ref: LessonRef): Word[] {
  const sectionId = sectionIdForMasteryUnit(ref.unitId);
  if (sectionId) return masteryWordsFor(corpus, sectionId);
  const words = wordsByUnit(corpus).get(ref.unitId);
  if (!words) return [];
  const range = wordIndicesFor(ref.lessonIndex, words.length);
  return range ? words.slice(range[0], range[1]) : [];
}

function masteryWordsFor(corpus: Corpus, sectionId: string): Word[] {
  const def = SECTIONS.find((s) => s.id === sectionId);
  if (!def) return [];
  const core = (wordsByUnit(corpus).get(unitIdFor(def)) ?? []).slice(0, CORE_LESSONS_PER_STAGE * WORDS_PER_LESSON);
  return masterySelection(core);
}

/** Interleaving.compose: a lesson's words plus up to two older ones from another field. */
export function composeInterleaved(lessonWords: Word[], leeches: Word[], due: Word[], limit = 2): Word[] {
  if (!lessonWords.length || limit <= 0) return lessonWords;
  const here = new Set(lessonWords.map((w) => w.id));
  const category = lessonWords[0].category.toLowerCase();
  const revisited: Word[] = [];
  for (const c of [...leeches, ...due]) {
    if (revisited.length >= limit) break;
    if (here.has(c.id)) continue;
    if (c.category.toLowerCase() === category) continue;
    revisited.push(c);
    here.add(c.id);
  }
  return [...lessonWords, ...revisited];
}

export function practiceWordsFor(corpus: Corpus, ref: LessonRef): Word[] {
  const lessonWords = wordsFor(corpus, ref);
  if (!lessonWords.length) return lessonWords;
  if (sectionIdForMasteryUnit(ref.unitId)) return lessonWords;
  return composeInterleaved(lessonWords, corpus.leeches(4), corpus.scheduledDue(8));
}

/** The first incomplete lesson in the first unit that has one, in tree order. */
export function nextLesson(corpus: Corpus, lessons: Record<string, LessonState>): LessonRef | null {
  for (const [unitId, words] of wordsByUnit(corpus)) {
    const count = lessonCountFor(words.length);
    for (let i = 0; i < count; i++) {
      if (!lessons[lessonKey(unitId, i)]?.isComplete) return { unitId, lessonIndex: i };
    }
  }
  return null;
}

// ── ExpandingRehearsal & Remediation ─────────────────────────────────────────

const GAPS = [2, 5, 10];
export const rehearsalGap = (missCount: number) => GAPS[Math.min(Math.max(missCount - 1, 0), GAPS.length - 1)];
export const rehearsalInsertIndex = (current: number, missCount: number, queueSize: number) =>
  Math.min(Math.max(current + rehearsalGap(missCount), 0), queueSize);

function glossOf(w: Word): string {
  if (w.tagalog && w.english && w.tagalog.toLowerCase() !== w.kasiguranin.toLowerCase()) return `${w.tagalog} · ${w.english}`;
  return w.english || w.tagalog;
}

export function contrastLine(chosen: string, chosenEntry: Word | undefined, target: Word): string | null {
  if (!chosenEntry || chosenEntry.id === target.id) return null;
  const meaning = glossOf(chosenEntry);
  const choseHeadword = chosen.trim().toLowerCase() === chosenEntry.kasiguranin.toLowerCase();
  if (choseHeadword && meaning) return `You chose ${chosenEntry.kasiguranin}, which means ${meaning}.`;
  if (!choseHeadword && chosenEntry.kasiguranin) return `You chose ${chosen.trim()}, which is ${chosenEntry.kasiguranin}.`;
  return null;
}

// ── SentenceBank ─────────────────────────────────────────────────────────────

export const SENTENCE_MIN_WORDS = 3;

export const SENTENCES: { kasiguranin: string[]; english: string }[] = [
  { kasiguranin: ['Magandang', 'aldaw', 'ha', 'iyo', "'ttanan!"], english: 'Good day to you all!' },
  { kasiguranin: ['Kumusta', 'na', 'ing', 'buhay', 'mo?'], english: 'How is your life?' },
  { kasiguranin: ['Tinumáknəg', 'ang', 'anák.'], english: 'The child stood up.' },
  { kasiguranin: ['Maglákad', 'akú', 'niiláw.'], english: 'I will leave tomorrow.' },
  { kasiguranin: ['Bəbbi', 'ang', 'anak', 'ni', 'Kendy.'], english: "Kendy's child is a girl." },
  { kasiguranin: ['Mabigsək', 'ang', 'parəs', 'kagibi.'], english: 'The wind was strong last night.' },
  { kasiguranin: ['Karon', 'na, ', 'kuman', 'tayo!'], english: "Let's go, let's eat!" },
  { kasiguranin: ['Me', 'tólay', 'sa', 'baláy.'], english: 'There is a person in the house.' },
  { kasiguranin: ['Walang', 'tólay', 'sa', 'baláy.'], english: 'There is no person in the house.' },
  { kasiguranin: ['Mag-uden', 'ngayon.'], english: 'It is raining now.' },
  { kasiguranin: ['Madisalad', 'ang', 'bulos.'], english: 'The river is deep.' },
  { kasiguranin: ['Saan', 'ka', 'umangay?'], english: 'Where are you going?' },
  { kasiguranin: ['Namúgtong', 'ang', 'anák', 'ng', 'mángga.'], english: 'I bought a mango.' },
  { kasiguranin: ['Kinumán', 'na', "ku'", 'ng', 'kanən.'], english: 'I already ate rice.' },
  { kasiguranin: ['Ang', 'sida', 'me', 'ay', 'manok.'], english: 'Our viand is chicken.' },
];

export const normaliseToken = (t: string) => t.toLowerCase().replace(/^[.,!?;:'" ]+|[.,!?;:'" ]+$/g, '');

export function sentenceUsing(word: string) {
  const needle = normaliseToken(word);
  if (!needle) return undefined;
  return SENTENCES.find((s) => s.kasiguranin.some((t) => normaliseToken(t) === needle));
}

// ── Exercises ────────────────────────────────────────────────────────────────

export type Exercise =
  | { type: 'choose'; word: Word; options: string[]; answer: string; promptIsKasiguranin: boolean }
  | { type: 'listen'; word: Word; options: string[]; answer: string }
  | { type: 'fill'; word: Word; options: string[]; answer: string; sentenceWithBlank: string; translation: string }
  | { type: 'aspect'; word: Word; options: string[]; answer: string; aspectLabel: string }
  | { type: 'type'; word: Word; options: string[]; answer: string; promptMeaning: string }
  | { type: 'sentence'; word: Word; options: string[]; answer: string; translation: string; correctOrder: string[] }
  | { type: 'match'; word: Word; options: string[]; answer: string; pairs: [Word, string][] };

export function exerciseInstruction(e: Exercise): string {
  switch (e.type) {
    case 'choose':
      return e.promptIsKasiguranin ? 'What does this mean?' : 'Say this in Kasiguranin';
    case 'listen':
      return 'Which word did you hear?';
    case 'fill':
      return 'Complete the sentence';
    case 'aspect':
      return `Choose the ${e.aspectLabel} form`;
    case 'type':
      return 'Type this in Kasiguranin';
    case 'sentence':
      return 'Build the sentence';
    case 'match':
      return 'Match each word to its meaning';
  }
}

export function choosePrompt(e: Extract<Exercise, { type: 'choose' }>): string {
  return e.promptIsKasiguranin ? e.word.kasiguranin : meaningFor(e.word.kasiguranin, e.word.tagalog, e.word.english) ?? e.word.tagalog;
}

/** How a meaning is written on an answer button: both glosses, unless the Tagalog is the headword. */
export function meaningOf(w: Word): string {
  const tagalogRepeats = w.tagalog.toLowerCase() === w.kasiguranin.toLowerCase();
  if (tagalogRepeats && w.english) return w.english;
  if (w.tagalog && w.english) return `${w.tagalog} · ${w.english}`;
  return w.tagalog || w.english;
}

const BLANK = '____';

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function aspectFormsOf(w: Word): [string, string][] {
  const out: [string, string][] = [];
  if (w.neutralForm) out.push(['neutral', w.neutralForm]);
  if (w.imperfectiveForm) out.push(['imperfective', w.imperfectiveForm]);
  if (w.perfectiveForm) out.push(['perfective', w.perfectiveForm]);
  if (w.contemplativeForm) out.push(['contemplative', w.contemplativeForm]);
  return out;
}

export function buildExercises(corpus: Corpus, words: Word[]): Exercise[] {
  if (!words.length) return [];
  const distractorStrings = (w: Word, kasiguraninAnswers: boolean) =>
    corpus
      .distractorsFor(w, 3)
      .map((d) => (kasiguraninAnswers ? d.kasiguranin : meaningOf(d)))
      .filter(Boolean);

  const exercises: Exercise[] = [];

  words.forEach((word, index) => {
    const hasUsableMeaning = meaningFor(word.kasiguranin, word.tagalog, word.english) != null;
    const kasPrompt = index % 2 === 0 || isLeech(word) || !hasUsableMeaning;
    const answer = kasPrompt ? meaningOf(word) : word.kasiguranin;
    const distractors = distractorStrings(word, !kasPrompt);
    exercises.push({
      type: 'choose',
      word,
      options: shuffled([...new Set([...distractors, answer])]),
      answer,
      promptIsKasiguranin: kasPrompt,
    });
  });

  const usable = distinctBy(
    words.filter((w) => meaningFor(w.kasiguranin, w.tagalog, w.english) != null),
    (w) => w.kasiguranin.toLowerCase()
  ).slice(0, 4);
  if (usable.length >= 3) {
    const pairs = usable.map((w) => [w, meaningOf(w)] as [Word, string]);
    exercises.push({
      type: 'match',
      word: usable[0],
      pairs,
      options: pairs.map((p) => p[1]),
      answer: pairs.map((p) => `${p[0].kasiguranin}=${p[1]}`).join('|'),
    });
  }

  let cursor = 0;
  while (exercises.length < EXERCISES_PER_LESSON && cursor < words.length) {
    const next = secondaryExercise(corpus, words[cursor], exercises, distractorStrings);
    if (next) exercises.push(next);
    cursor++;
  }
  return exercises;
}

function secondaryExercise(
  corpus: Corpus,
  word: Word,
  existing: Exercise[],
  distractorStrings: (w: Word, kas: boolean) => string[]
): Exercise | null {
  const used = new Set(existing.filter((e) => e.word.id === word.id).map((e) => e.type));

  if (word.audioFileName && !used.has('listen')) {
    return {
      type: 'listen',
      word,
      options: shuffled([...new Set([...distractorStrings(word, true), word.kasiguranin])]),
      answer: word.kasiguranin,
    };
  }

  if (!used.has('sentence')) {
    const s = sentenceBuild(corpus, word);
    if (s) return s;
  }

  const sentence = word.exampleSentence;
  if (sentence && sentence.toLowerCase().includes(word.kasiguranin.toLowerCase()) && !used.has('fill')) {
    return {
      type: 'fill',
      word,
      options: shuffled([...new Set([...distractorStrings(word, true), word.kasiguranin])]),
      answer: word.kasiguranin,
      sentenceWithBlank: sentence.replace(new RegExp(escapeRegExp(word.kasiguranin), 'gi'), BLANK),
      translation: word.exampleTranslation,
    };
  }

  const aspects = aspectFormsOf(word);
  if (aspects.length >= 3 && !used.has('aspect')) {
    const [label, correct] = randomOf(aspects);
    return {
      type: 'aspect',
      word,
      options: shuffled([...new Set(aspects.map((a) => a[1]))]),
      answer: correct,
      aspectLabel: label,
    };
  }

  const meaning = meaningFor(word.kasiguranin, word.tagalog, word.english);
  if (meaning != null && !isLeech(word) && !used.has('type')) {
    return { type: 'type', word, options: [], answer: word.kasiguranin, promptMeaning: meaning };
  }
  return null;
}

function sentenceBuild(corpus: Corpus, word: Word): Exercise | null {
  const own = word.exampleSentence.trim();
  let parts: string[];
  let translation: string;
  if (own && word.exampleTranslation) {
    parts = own.split(' ').filter((p) => p.trim());
    translation = word.exampleTranslation;
  } else {
    const authored = sentenceUsing(word.kasiguranin);
    if (!authored) return null;
    parts = authored.kasiguranin;
    translation = authored.english;
  }
  if (parts.length < SENTENCE_MIN_WORDS) return null;
  const inSentence = new Set(parts.map(normaliseToken));
  const intruders = corpus
    .distractorsFor(word, 4)
    .map((d) => d.kasiguranin)
    .filter((k) => k && !inSentence.has(normaliseToken(k)))
    .slice(0, 2);
  return {
    type: 'sentence',
    word,
    options: shuffled([...parts, ...intruders]),
    answer: parts.join(' '),
    translation,
    correctOrder: parts,
  };
}

