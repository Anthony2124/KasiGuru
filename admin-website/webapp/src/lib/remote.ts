/**
 * Everything else the app reads or writes in Firestore: the leaderboard, public profiles,
 * announcements, community submissions and issue reports. Shapes match the Android DTOs and
 * firestore.rules exactly - the rules reject any document carrying a key they do not list.
 */
import {
  collection,
  doc,
  getCount,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  setDoc,
  Timestamp,
  where,
  type DocumentData,
} from 'firebase/firestore/lite';
import { isoWeekId } from '../domain/dates';
import { parsePublicProfile, type PublicProfile } from '../domain/publicProfile';
import type { AnnouncementDto } from '../domain/types';
import { auth, db } from './firebase';
import { getState, setState } from './store';

export interface LeaderboardEntry {
  uid: string;
  name: string;
  totalXp: number;
  weeklyXp: number;
  currentStreak: number;
  avatarIconId: number;
  level: number;
  levelTitle: string;
  isCurrentUser: boolean;
  /** Competition rank: equal scores share a rank. */
  rank: number;
}

export type LeaderboardOrder = 'totalXp' | 'weeklyXp' | 'currentStreak';

const TOP_N = 50;

/** One read per row, so boards are cached per order for a few minutes rather than refetched on every visit. */
const boardCache = new Map<LeaderboardOrder, { at: number; rows: LeaderboardEntry[] }>();

const scoreOf = (e: LeaderboardEntry, order: LeaderboardOrder) =>
  order === 'weeklyXp' ? e.weeklyXp : order === 'currentStreak' ? e.currentStreak : e.totalXp;

function entry(id: string, x: DocumentData, rank: number): LeaderboardEntry | null {
  if (x.isAnonymous === true) return null;
  const name = typeof x.displayName === 'string' ? x.displayName.trim() : '';
  if (!name) return null;
  return {
    uid: id,
    name,
    totalXp: Number(x.totalXp) || 0,
    // A row last written in an earlier week has no XP this week.
    weeklyXp: x.weekStartDate === isoWeekId() ? Number(x.weeklyXp) || 0 : 0,
    currentStreak: Number(x.currentStreak) || 0,
    avatarIconId: Number(x.profileIconId) || 1,
    level: Number(x.level) || 1,
    levelTitle: typeof x.titleBadge === 'string' && x.titleBadge ? x.titleBadge : 'Learner',
    isCurrentUser: id === getState().account.uid,
    rank,
  };
}

/**
 * LeaderboardRepository: weekly (this ISO week's rows only), all-time and streak boards, each the
 * top 50. A signed-in learner outside the top 50 gets their own row appended with a counted rank.
 */
export async function fetchLeaderboard(order: LeaderboardOrder, force = false): Promise<LeaderboardEntry[]> {
  const cached = boardCache.get(order);
  if (!force && cached && Date.now() - cached.at < 3 * 60 * 1000) return cached.rows;
  const base =
    order === 'weeklyXp'
      ? query(collection(db, 'leaderboard_public'), where('weekStartDate', '==', isoWeekId()))
      : query(collection(db, 'leaderboard_public'));
  const snap = await getDocs(query(base, orderBy(order, 'desc'), limit(TOP_N)));
  const rows: LeaderboardEntry[] = [];
  let previous: number | null = null;
  let previousRank = 0;
  snap.docs
    .filter((d) => d.data().isAnonymous !== true)
    .forEach((d, i) => {
      const value = Number(d.data()[order]) || 0;
      const rank = value === previous ? previousRank : i + 1;
      previous = value;
      previousRank = rank;
      const e = entry(d.id, d.data(), rank);
      if (e) rows.push(e);
    });
  const user = auth.currentUser;
  if (user && !user.isAnonymous && !rows.some((r) => r.uid === user.uid)) {
    try {
      const mine = await getDoc(doc(db, 'leaderboard_public', user.uid));
      const e = mine.exists() ? entry(mine.id, mine.data(), 0) : null;
      if (e && (order !== 'weeklyXp' || mine.data()!.weekStartDate === isoWeekId())) {
        const ahead = await getCount(query(base, where(order, '>', scoreOf(e, order))));
        rows.push({ ...e, rank: ahead.data().count + 1 });
      }
    } catch {
      // The board is still useful without the learner's own row.
    }
  }
  boardCache.set(order, { at: Date.now(), rows });
  return rows;
}

const profileCache = new Map<string, PublicProfile>();

/** public_profiles/{uid}, or null when that learner has not shared one. */
export async function fetchPublicProfile(uid: string): Promise<PublicProfile | null> {
  const snap = await getDoc(doc(db, 'public_profiles', uid));
  if (!snap.exists()) {
    profileCache.delete(uid);
    return null;
  }
  const p = parsePublicProfile(snap.data());
  profileCache.set(uid, p);
  return p;
}

export const cachedPublicProfile = (uid: string) => profileCache.get(uid) ?? null;

const rankCache = new Map<string, { at: number; rank: number | null }>();

/** This week's rank on the weekly board, or null if they have no row this week. Cached briefly. */
export async function weeklyRank(uid: string): Promise<number | null> {
  const cached = rankCache.get(uid);
  if (cached && Date.now() - cached.at < 3 * 60 * 1000) return cached.rank;
  const week = isoWeekId();
  const row = await getDoc(doc(db, 'leaderboard_public', uid));
  let rank: number | null = null;
  if (row.exists() && row.data().weekStartDate === week && typeof row.data().weeklyXp === 'number') {
    const ahead = await getCount(
      query(collection(db, 'leaderboard_public'), where('weekStartDate', '==', week), where('weeklyXp', '>', row.data().weeklyXp))
    );
    rank = ahead.data().count + 1;
  }
  rankCache.set(uid, { at: Date.now(), rank });
  return rank;
}

const rarityCache = new Map<string, { at: number; count: number }>();

/** How many public profiles hold each badge, for choosing a showcase. Cached for an hour. */
export async function badgeRarity(ids: string[]): Promise<Record<string, number>> {
  const out: Record<string, number> = {};
  await Promise.all(
    [...new Set(ids)].map(async (id) => {
      const cached = rarityCache.get(id);
      if (cached && Date.now() - cached.at < 3_600_000) {
        out[id] = cached.count;
        return;
      }
      try {
        const n = (await getCount(query(collection(db, 'public_profiles'), where('badgeIds', 'array-contains', id)))).data().count;
        rarityCache.set(id, { at: Date.now(), count: n });
        out[id] = n;
      } catch {
        out[id] = Number.MAX_SAFE_INTEGER;
      }
    })
  );
  return out;
}

export async function fetchAnnouncements() {
  try {
    // Filter only, sorted here: active + orderBy(createdAt) needs a composite index the project does
    // not have (the Android app's identical query fails for that reason), and there are only ever a
    // handful of announcements.
    const snap = await getDocs(query(collection(db, 'announcements'), where('active', '==', true), limit(20)));
    const list: AnnouncementDto[] = snap.docs.map((d) => {
      const x = d.data();
      return {
        id: d.id,
        title: String(x.title ?? ''),
        message: String(x.message ?? ''),
        active: x.active !== false,
        createdAt: Number(x.createdAt) || 0,
      };
    });
    list.sort((a, b) => b.createdAt - a.createdAt);
    setState({ announcements: list.slice(0, 5) });
  } catch (e) {
    console.warn('announcements unavailable', e);
  }
}

/** The local calendar day of a moderation timestamp (millis or Firestore Timestamp), if any. */
function approvedDay(x: DocumentData): string | null {
  const t = x.approvedAt ?? x.reviewedAt;
  const ms = typeof t === 'number' ? t : t instanceof Timestamp ? t.toMillis() : null;
  if (ms == null) return null;
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * Approved submissions for this account (SubmissionRepository.getApprovedSubmissions), each worth a
 * one-time 10 XP receipt keyed `word:<id>` or `literature:<id>`. Null when the query failed.
 */
export async function approvedContributions(): Promise<{ id: string; approvedDay: string | null }[] | null> {
  // The live Auth user, not the store's copy: during a sign-in or sign-out the two briefly differ,
  // and a query naming another uid is refused by the owner-read rule.
  const uid = auth.currentUser?.uid;
  if (!uid) return [];
  try {
    const [w, l] = await Promise.all([
      getDocs(query(collection(db, 'word_submissions'), where('uid', '==', uid), where('status', '==', 'approved'))),
      getDocs(query(collection(db, 'literature_submissions'), where('uid', '==', uid), where('status', '==', 'approved'))),
    ]);
    return [
      ...w.docs.map((d) => ({ id: `word:${d.id}`, approvedDay: approvedDay(d.data()) })),
      ...l.docs.map((d) => ({ id: `literature:${d.id}`, approvedDay: approvedDay(d.data()) })),
    ];
  } catch {
    return null;
  }
}

export interface WordSubmission {
  kasiguranin: string;
  tagalog: string;
  english: string;
  rootForm: string;
  category: string;
  partOfSpeech: string;
  ipaNotation: string;
  exampleSentence: string;
  pastTense: string;
  presentTense: string;
  futureTense: string;
  contributorName: string;
}

export async function submitWord(s: WordSubmission) {
  const ref = doc(collection(db, 'word_submissions'));
  await setDoc(ref, {
    id: ref.id,
    ...s,
    status: 'pending',
    submittedAt: Date.now(),
    uid: getState().account.uid ?? '',
  });
}

export interface LiteratureSubmission {
  title: string;
  titleKasiguranin: string;
  pagesJson: string;
  contributorName: string;
  pdfBase64: string;
  pdfFileName: string;
}

export async function submitLiterature(s: LiteratureSubmission) {
  const ref = doc(collection(db, 'literature_submissions'));
  await setDoc(ref, {
    id: ref.id,
    ...s,
    status: 'pending',
    submittedAt: Date.now(),
    uid: getState().account.uid ?? '',
  });
}

export interface IssueReport {
  category: string;
  title: string;
  description: string;
  targetWord: string;
  targetScreen: string;
  photoBase64: string;
  reporterName: string;
  reporterEmail: string;
}

export const APP_VERSION = 'web 1.18.0';

export async function submitReport(r: IssueReport) {
  const ref = doc(collection(db, 'issue_reports'));
  await setDoc(ref, {
    id: ref.id,
    ...r,
    photoUrl: '',
    appVersion: APP_VERSION,
    deviceInfo: navigator.userAgent.slice(0, 200),
    status: 'pending',
    adminNotes: '',
    submittedAt: Date.now(),
    uid: getState().account.uid ?? '',
  });
  return ref.id;
}
