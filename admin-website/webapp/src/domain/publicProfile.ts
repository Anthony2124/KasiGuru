/**
 * public_profiles/{uid}: the allowlisted projection other learners may read. Port of
 * PublicProfileDto. It deliberately cannot carry anything from Edit profile (full name, age,
 * address, email); reading one back drops unknown badges, sections and keys.
 */
import { BADGE_ROWS, normalizeBackground } from './badges';
import { SECTIONS } from './lesson';
import { isPlaceholderName, PLACEHOLDER_NAME } from './types';

export interface PublicProfile {
  displayName: string;
  profileIconId: number;
  profileBackgroundId: string;
  level: number;
  totalXp: number;
  currentStreak: number;
  wordsLearned: number;
  lessonsCompleted: number;
  weeklyXp: number;
  weekId: string;
  createdAt: number;
  updatedAt: number;
  badgeIds: string[];
  sections: Record<string, number>;
  sectionTotals: Record<string, number>;
  masteredSections: string[];
  unlockedSections: string[];
}

/**
 * The name every other learner sees: the nickname, or the full name when no nickname was ever set.
 * Never an email, never blank. Port of PublicProfileDto.displayName.
 *
 * Someone who signs in from the first onboarding screen skips the nickname step, so theirs stays
 * the placeholder; leaving that step blank saves "Kasiguranin Learner". Publishing either ranked
 * them under a default with no way out, since Edit profile changes the full name, not the nickname.
 */
export function publicDisplayName(userName: string, fullName = ''): string {
  const name = [userName, fullName]
    .map((n) => n.trim().slice(0, 40))
    .find((n) => n && !n.includes('@') && !isPlaceholderName(n));
  return name ?? PLACEHOLDER_NAME;
}

const BADGE_IDS = new Set(BADGE_ROWS.map((r) => r.id));
const SECTION_IDS = new Set(SECTIONS.map((s) => s.id));

const count = (v: unknown) => (typeof v === 'number' && Number.isFinite(v) ? Math.max(0, Math.trunc(v)) : 0);
const strings = (v: unknown) => (Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : []);
const counts = (v: unknown) =>
  v && typeof v === 'object' && !Array.isArray(v)
    ? Object.fromEntries(
        Object.entries(v as Record<string, unknown>)
          .filter(([k, n]) => SECTION_IDS.has(k) && typeof n === 'number')
          .map(([k, n]) => [k, count(n)])
      )
    : {};

export function parsePublicProfile(data: Record<string, unknown>): PublicProfile {
  return {
    displayName: publicDisplayName(typeof data.displayName === 'string' ? data.displayName : ''),
    profileIconId: count(data.profileIconId),
    profileBackgroundId: normalizeBackground(typeof data.profileBackgroundId === 'string' ? data.profileBackgroundId : ''),
    level: Math.min(30, Math.max(1, count(data.level))),
    totalXp: count(data.totalXp),
    currentStreak: count(data.currentStreak),
    wordsLearned: count(data.wordsLearned),
    lessonsCompleted: count(data.lessonsCompleted),
    weeklyXp: count(data.weeklyXp),
    weekId: typeof data.weekId === 'string' ? data.weekId : '',
    createdAt: typeof data.createdAt === 'number' ? data.createdAt : 0,
    updatedAt: typeof data.updatedAt === 'number' ? data.updatedAt : 0,
    badgeIds: [...new Set(strings(data.badgeIds).filter((id) => BADGE_IDS.has(id)))],
    sections: counts(data.sections),
    sectionTotals: counts(data.sectionTotals),
    masteredSections: strings(data.masteredSections).filter((id) => SECTION_IDS.has(id)),
    unlockedSections: strings(data.unlockedSections).filter((id) => SECTION_IDS.has(id)),
  };
}
