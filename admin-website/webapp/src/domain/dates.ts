/**
 * Calendar days in the learner's own time zone, as java.time.LocalDate.now() gives them on Android.
 *
 * Streaks, the daily goal and review dates are all day-granular strings (yyyy-MM-dd) shared with
 * the Android app through Firestore, so the web app must produce identical strings for the same day.
 * `toISOString()` would not: it is UTC, which moves the day boundary to 8 a.m. in the Philippines.
 */

const pad = (n: number) => String(n).padStart(2, '0');

export function isoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Overridable in tests, the way the Kotlin merge tests pin `today`. */
let clock: () => Date = () => new Date();
export function setClock(fn: () => Date) {
  clock = fn;
}
export const now = () => clock();

export function today(): string {
  return isoDate(clock());
}

/** Parses yyyy-MM-dd to a day count, or null. Day counts make DAYS.between exact across DST. */
export function epochDay(iso: string): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!m) return null;
  return Math.floor(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])) / 86_400_000);
}

/** ChronoUnit.DAYS.between(from, to). */
export function daysBetween(fromIso: string, toIso: string): number | null {
  const a = epochDay(fromIso);
  const b = epochDay(toIso);
  return a == null || b == null ? null : b - a;
}

export function plusDays(iso: string, days: number): string {
  const d = epochDay(iso);
  if (d == null) return iso;
  const date = new Date((d + days) * 86_400_000);
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

/** ISO calendar week id, e.g. "2026-W33", for the weekly leaderboard rollover. */
export function isoWeekId(d: Date = clock()): string {
  const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
  const day = date.getUTCDay() || 7;
  date.setUTCDate(date.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((date.getTime() - yearStart.getTime()) / 86_400_000 + 1) / 7);
  return `${date.getUTCFullYear()}-W${pad(week)}`;
}
