/** Kotlin's shuffled() / random() and SQLite's ORDER BY RANDOM(). */
export function shuffled<T>(list: readonly T[]): T[] {
  const a = list.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

export function randomOf<T>(list: readonly T[]): T {
  return list[Math.floor(Math.random() * list.length)];
}

export function distinctBy<T>(list: readonly T[], key: (t: T) => unknown): T[] {
  const seen = new Set<unknown>();
  const out: T[] = [];
  for (const t of list) {
    const k = key(t);
    if (seen.has(k)) continue;
    seen.add(k);
    out.push(t);
  }
  return out;
}
