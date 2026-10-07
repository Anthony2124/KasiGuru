import { beforeEach, describe, expect, it, vi } from 'vitest';

const fake = vi.hoisted(() => ({
  auth: { currentUser: null as { uid: string; isAnonymous: boolean } | null },
  week: '2026-W41',
  getDocs: vi.fn(), getDoc: vi.fn(), getCount: vi.fn(),
}));
vi.mock('../src/lib/firebase', () => ({ auth: fake.auth, db: {} }));
vi.mock('../src/lib/store', () => ({ getState: () => ({ account: {} }), setState: vi.fn() }));
vi.mock('../src/domain/dates', () => ({ isoWeekId: () => fake.week }));
vi.mock('firebase/firestore/lite', () => ({
  collection: vi.fn(), doc: vi.fn(), query: vi.fn(), where: vi.fn(), orderBy: vi.fn(), limit: vi.fn(),
  getDocs: fake.getDocs, getDoc: fake.getDoc, getCount: fake.getCount, setDoc: vi.fn(), Timestamp: class {},
}));

const row = (id: string, xp: number) => ({
  id,
  data: () => ({ displayName: id, totalXp: xp, weeklyXp: xp, weekStartDate: fake.week }),
});

beforeEach(() => {
  vi.resetModules();
  vi.clearAllMocks();
  vi.useRealTimers();
  fake.auth.currentUser = null;
  fake.week = '2026-W41';
  fake.getDocs.mockResolvedValue({ docs: [row('a', 100), row('b', 100), row('c', 50)] });
  fake.getDoc.mockResolvedValue({ exists: () => false });
});

describe('leaderboard read budget and account isolation', () => {
  it('reuses a fresh board, preserves tied ranks, and keeps each tab separate', async () => {
    const { fetchLeaderboard } = await import('../src/lib/remote');
    expect((await fetchLeaderboard('weeklyXp')).map((r) => r.rank)).toEqual([1, 1, 3]);
    await fetchLeaderboard('weeklyXp');
    expect(fake.getDocs).toHaveBeenCalledTimes(1);
    await fetchLeaderboard('totalXp');
    expect(fake.getDocs).toHaveBeenCalledTimes(2);
  });

  it('coalesces simultaneous requests for the same board', async () => {
    const { fetchLeaderboard } = await import('../src/lib/remote');
    await Promise.all([fetchLeaderboard('weeklyXp'), fetchLeaderboard('weeklyXp')]);
    expect(fake.getDocs).toHaveBeenCalledTimes(1);
  });

  it('does not show the previous account as you after signing in or out', async () => {
    const { fetchLeaderboard } = await import('../src/lib/remote');
    fake.auth.currentUser = { uid: 'a', isAnonymous: false };
    expect((await fetchLeaderboard('weeklyXp')).find((r) => r.isCurrentUser)?.uid).toBe('a');
    fake.auth.currentUser = { uid: 'b', isAnonymous: false };
    expect((await fetchLeaderboard('weeklyXp')).find((r) => r.isCurrentUser)?.uid).toBe('b');
    fake.auth.currentUser = null;
    expect((await fetchLeaderboard('weeklyXp')).some((r) => r.isCurrentUser)).toBe(false);
    expect(fake.getDocs).toHaveBeenCalledTimes(3);
  });

  it('refreshes at a week boundary even inside the three-minute cache window', async () => {
    const { fetchLeaderboard } = await import('../src/lib/remote');
    await fetchLeaderboard('weeklyXp');
    fake.week = '2026-W42';
    await fetchLeaderboard('weeklyXp');
    expect(fake.getDocs).toHaveBeenCalledTimes(2);
  });

  it('expires after three minutes and allows an explicit refresh', async () => {
    vi.useFakeTimers();
    const { fetchLeaderboard } = await import('../src/lib/remote');
    await fetchLeaderboard('weeklyXp');
    vi.advanceTimersByTime(180_000);
    await fetchLeaderboard('weeklyXp');
    await fetchLeaderboard('weeklyXp', true);
    expect(fake.getDocs).toHaveBeenCalledTimes(3);
    vi.useRealTimers();
  });

  it('retries a failed query instead of caching an error', async () => {
    fake.getDocs.mockRejectedValueOnce(Error('offline'));
    const { fetchLeaderboard } = await import('../src/lib/remote');
    await expect(fetchLeaderboard('weeklyXp')).rejects.toThrow('offline');
    await fetchLeaderboard('weeklyXp');
    expect(fake.getDocs).toHaveBeenCalledTimes(2);
  });

  it('appends the signed-in learner outside the top 50 with their counted rank', async () => {
    fake.auth.currentUser = { uid: 'mine', isAnonymous: false };
    fake.getDoc.mockResolvedValue({ ...row('mine', 5), exists: () => true });
    fake.getCount.mockResolvedValue({ data: () => ({ count: 75 }) });
    const { fetchLeaderboard } = await import('../src/lib/remote');
    expect((await fetchLeaderboard('weeklyXp')).at(-1)).toMatchObject({ uid: 'mine', rank: 76, isCurrentUser: true });
  });
});
