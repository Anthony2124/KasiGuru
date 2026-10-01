/**
 * Everything else the app reads or writes in Firestore: the leaderboard, announcements, community
 * submissions and issue reports. Shapes match the Android DTOs and firestore.rules exactly - the
 * rules reject any document carrying a key they do not list.
 */
import {
  collection,
  doc,
  getDocs,
  limit,
  orderBy,
  query,
  setDoc,
  where,
} from 'firebase/firestore/lite';
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
  levelTitle: string;
  isCurrentUser: boolean;
}

export type LeaderboardOrder = 'totalXp' | 'weeklyXp' | 'currentStreak';

const TOP_N = 50;

/** One read per row, so boards are cached per order for a few minutes rather than refetched on every visit. */
const boardCache = new Map<LeaderboardOrder, { at: number; rows: LeaderboardEntry[] }>();

export async function fetchLeaderboard(order: LeaderboardOrder, force = false): Promise<LeaderboardEntry[]> {
  const cached = boardCache.get(order);
  if (!force && cached && Date.now() - cached.at < 3 * 60 * 1000) return cached.rows;
  const uid = getState().account.uid;
  const snap = await getDocs(query(collection(db, 'leaderboard_public'), orderBy(order, 'desc'), limit(TOP_N)));
  const rows = snap.docs
    .filter((d) => d.data().isAnonymous !== true)
    .map((d) => {
      const x = d.data();
      return {
        uid: d.id,
        name: typeof x.displayName === 'string' ? x.displayName : '',
        totalXp: Number(x.totalXp) || 0,
        weeklyXp: Number(x.weeklyXp) || 0,
        currentStreak: Number(x.currentStreak) || 0,
        avatarIconId: Number(x.profileIconId) || 1,
        levelTitle: typeof x.titleBadge === 'string' ? x.titleBadge : 'Kasiguranin Apprentice',
        isCurrentUser: d.id === uid,
      };
    })
    .filter((r) => r.name.trim());
  boardCache.set(order, { at: Date.now(), rows });
  return rows;
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

/** Approved submissions for this account, for the Trusted Voice / Corpus Builder badges. */
export async function approvedSubmissionCount(): Promise<number | null> {
  // The live Auth user, not the store's copy: during a sign-in or sign-out the two briefly differ,
  // and a query naming another uid is refused by the owner-read rule.
  const uid = auth.currentUser?.uid;
  if (!uid) return 0;
  try {
    const [w, l] = await Promise.all([
      getDocs(query(collection(db, 'word_submissions'), where('uid', '==', uid), where('status', '==', 'approved'))),
      getDocs(query(collection(db, 'literature_submissions'), where('uid', '==', uid), where('status', '==', 'approved'))),
    ]);
    return w.size + l.size;
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

export const APP_VERSION = 'web 1.17.0';

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
