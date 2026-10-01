/**
 * XP policy version 2: what each activity is worth, the 30 account levels, and the reward ledger
 * that turns receipts into totals. Port of domain/gamification/XpPolicy.kt (XpPolicy, RewardRecord,
 * RewardLedger) and data/remote/RewardReceiptCodec.kt. See docs/design/XP_AND_TIERED_BADGES_PLAN.md.
 *
 * XP is no longer a counter that each screen adds to. Every reward is a receipt with a stable id,
 * receipts sync one document each, and totals are recomputed from the union, so two devices can
 * never pay the same lesson twice and a review cap applies after combining them.
 */
import { BADGE_PREFIX, familyFor, tierFor } from './badges';

export const XP_POLICY_VERSION = 2;

/** minimumXp(level) = roundHalfUpToNearest10(100 * (level - 1)^1.5); checked against the JVM. */
export const LEVEL_THRESHOLDS: readonly number[] = Array.from(
  { length: 30 },
  (_, i) => Math.floor((100 * Math.pow(i, 1.5)) / 10 + 0.5) * 10
);
export const MAX_LEVEL = LEVEL_THRESHOLDS.length;

export function levelFor(xp: number): number {
  let index = -1;
  for (let i = 0; i < LEVEL_THRESHOLDS.length; i++) if (xp >= LEVEL_THRESHOLDS[i]) index = i;
  return Math.max(1, index + 1);
}

export const lessonXp = (perfect: boolean) => 20 + (perfect ? 5 : 0);

/** Recognition 2 per correct answer, recall 3, board words 1; never speed. Capped at 40. */
export function gameXp(mode: string, correct: number, total: number, perfect: boolean): number {
  if (!(total > 0 && correct >= 0 && correct <= total)) throw new Error('Game correctness must be a count, not XP.');
  const weight = ['audio_quiz', 'recall', 'aspect_builder', 'sentence_order'].includes(mode)
    ? 3
    : mode === 'word_search' || mode === 'word_wheel'
      ? 1
      : 2;
  return Math.min(40, 5 + correct * weight + (perfect ? 5 : 0));
}

export type ReceiptKind =
  | 'lesson'
  | 'game'
  | 'replay'
  | 'review'
  | 'story'
  | 'mastery'
  | 'approved'
  | 'badge'
  | 'perfect'
  | 'retrieval'
  | 'category'
  | 'access';

export interface RewardRecord {
  id: string;
  kind: ReceiptKind;
  source: string;
  /** ISO date the reward counts toward; empty for imported or timeless evidence. */
  day: string;
  xp: number;
  value: number;
  imported: boolean;
}

export const receipt = (
  id: string,
  kind: ReceiptKind,
  source: string,
  day: string,
  xp: number,
  value = 1,
  imported = false
): RewardRecord => ({ id, kind, source, day, xp, value, imported });

export interface XpTotals {
  activity: number;
  bonus: number;
  byDay: Record<string, number>;
  total: number;
}

const ONE_TIME = new Set(['badge', 'mastery', 'story', 'approved']);
const CONTENT = new Set(['lesson', 'game', 'replay']);

/** Two copies of one receipt: the earliest day and the strongest evidence win. Idempotent and commutative. */
export function mergeRecord(a: RewardRecord, b: RewardRecord): RewardRecord {
  if (a.id !== b.id || a.kind !== b.kind || a.source !== b.source) throw new Error('Different receipts');
  if (a.day !== b.day && !ONE_TIME.has(a.kind)) throw new Error('A dated receipt cannot change its day');
  return {
    ...a,
    day: a.day < b.day ? a.day : b.day,
    xp: Math.max(a.xp, b.xp),
    value: Math.max(a.value, b.value),
    imported: a.imported || b.imported,
  };
}

const groupBy = <T>(rows: T[], key: (r: T) => string) => {
  const out = new Map<string, T[]>();
  for (const r of rows) {
    const k = key(r);
    const list = out.get(k);
    if (list) list.push(r);
    else out.set(k, [r]);
  }
  return out;
};

/** Cumulative lesson/game targets plus daily replay offers settle to max(improvement, replay). */
export function totals(records: RewardRecord[]): XpTotals {
  let activity = 0;
  let bonus = 0;
  const days: Record<string, number> = {};
  const addDay = (day: string, xp: number) => {
    if (day.trim()) days[day] = (days[day] ?? 0) + xp;
  };
  for (const rows of groupBy(records.filter((r) => CONTENT.has(r.kind)), (r) => r.source).values()) {
    let best = Math.max(0, ...rows.filter((r) => r.imported && r.kind !== 'replay').map((r) => r.xp));
    activity += best;
    const daily = groupBy(rows.filter((r) => !r.imported), (r) => r.day);
    for (const day of [...daily.keys()].sort()) {
      const list = daily.get(day)!;
      const targets = list.filter((r) => r.kind !== 'replay').map((r) => r.xp);
      const target = targets.length ? Math.max(...targets) : best;
      const delta = Math.max(0, target - best);
      const repeat = Math.max(0, ...list.filter((r) => r.kind === 'replay').map((r) => r.xp));
      const earned = Math.max(delta, repeat);
      best = Math.max(best, target);
      activity += earned;
      addDay(day, earned);
    }
  }
  // The review cap applies after a union of devices, not once on each device.
  for (const [day, rows] of groupBy(records.filter((r) => r.kind === 'review'), (r) => r.day)) {
    activity += Math.min(60, rows.reduce((s, r) => s + r.xp, 0));
    addDay(day, Math.min(60, rows.filter((r) => !r.imported).reduce((s, r) => s + r.xp, 0)));
  }
  for (const r of records) {
    if (CONTENT.has(r.kind) || r.kind === 'review') continue;
    if (r.kind === 'badge') bonus += r.xp;
    else {
      activity += r.xp;
      if (!r.imported) addDay(r.day, r.xp);
    }
  }
  return { activity, bonus, byDay: days, total: activity + bonus };
}

// ── Firestore codec (users/{uid}/rewardReceipts/{sha256(id)}) ─────────────────

export const RECEIPTS_COLLECTION = 'rewardReceipts';

/** Receipt ids carry slashes and spaces; the document id is their SHA-256, lowercase hex. */
export async function receiptDocumentId(id: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(id));
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
}

export function encodeReceipt(r: RewardRecord): Record<string, unknown> {
  return {
    policyVersion: XP_POLICY_VERSION,
    id: r.id,
    kind: r.kind,
    source: r.source,
    day: r.day,
    xp: r.xp,
    value: r.value,
    imported: r.imported,
  };
}

const isIsoDate = (day: string) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) return false;
  const d = new Date(`${day}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === day;
};

const intAfter = (s: string, sep: string) => {
  const tail = s.slice(s.lastIndexOf(sep) + 1);
  return /^-?\d+$/.test(tail) ? Number(tail) : null;
};

/** Validates a receipt the way RewardReceiptCodec.decode does: anything unexpected mints nothing. */
export function decodeReceipt(data: Record<string, unknown>): RewardRecord | null {
  if (data.policyVersion !== XP_POLICY_VERSION) return null;
  const { id, kind, source, day, xp, value, imported } = data;
  if (typeof id !== 'string' || typeof kind !== 'string' || typeof source !== 'string' || typeof day !== 'string') return null;
  if (typeof xp !== 'number' || typeof value !== 'number' || typeof imported !== 'boolean') return null;
  if (!Number.isInteger(xp) || !Number.isInteger(value)) return null;
  if (!id.trim() || id.length > 512 || !source.trim() || source.length > 256 || value < 0 || value > 1) return null;
  if (day && !isIsoDate(day)) return null;
  const family = familyFor(id);
  let validXp: boolean;
  switch (kind) {
    case 'lesson': validXp = xp === 20 || xp === 25; break;
    case 'game': validXp = xp >= 5 && xp <= 40; break;
    case 'replay': validXp = xp >= 1 && xp <= 10 && !!day; break;
    case 'review': validXp = xp >= 1 && xp <= 3 && !!day; break;
    case 'story': validXp = xp === 20; break;
    case 'mastery': validXp = xp === 5; break;
    case 'approved': validXp = xp === 10; break;
    case 'badge': validXp = !!family && xp === (family.bonus ? tierFor(id)!.bonus : 0); break;
    case 'perfect':
    case 'retrieval':
    case 'category':
    case 'access': validXp = xp === 0; break;
    default: return null;
  }
  let canonical: string;
  switch (kind) {
    case 'lesson':
    case 'game': canonical = imported && !day ? `${source}:import` : `${source}:${day}`; break;
    case 'story':
    case 'badge': canonical = source; break;
    case 'replay':
    case 'review':
    case 'retrieval': canonical = `${kind}:${source}:${day}`; break;
    default: canonical = `${kind}:${source}`;
  }
  let validSource: boolean;
  switch (kind) {
    case 'lesson': validSource = source.startsWith('lesson:') && (intAfter(source, '#') ?? -1) >= 0; break;
    case 'game':
    case 'perfect': validSource = source.startsWith('game:') && (intAfter(source, '#') ?? 0) > 0; break;
    case 'replay': validSource = source.startsWith('lesson:') || source.startsWith('game:'); break;
    case 'review':
    case 'retrieval':
    case 'mastery': validSource = source.startsWith('word:') && source.length > 5; break;
    case 'story': {
      const n = source.startsWith('story:') && /^-?\d+$/.test(source.slice(6)) ? Number(source.slice(6)) : 0;
      validSource = n > 0;
      break;
    }
    case 'approved': validSource = source.startsWith('word:') || source.startsWith('literature:'); break;
    case 'badge': validSource = source.startsWith(BADGE_PREFIX) && !!familyFor(source); break;
    case 'access': validSource = source.startsWith('story:') || source.startsWith('section:'); break;
    default: validSource = true; // category
  }
  if (!validXp || !validSource || id !== canonical) return null;
  return { id, kind: kind as ReceiptKind, source, day, xp, value, imported };
}
