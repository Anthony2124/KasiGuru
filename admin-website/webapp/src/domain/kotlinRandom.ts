/**
 * kotlin.random.Random(seed): the XorWow generator Kotlin uses, reproduced bit for bit, so a seeded
 * puzzle (Word Wheel's wheel order) comes out the same on the web as on Android.
 */
export class KotlinRandom {
  private x: number;
  private y: number;
  private z: number;
  private w: number;
  private v: number;
  private addend: number;

  /** Random(seed: Long) = XorWowRandom(seed.toInt(), seed.shr(32).toInt()). */
  constructor(seed: number | bigint) {
    const big = BigInt.asIntN(64, BigInt(seed));
    const seed1 = Number(BigInt.asIntN(32, big));
    const seed2 = Number(BigInt.asIntN(32, big >> 32n));
    this.x = seed1 | 0;
    this.y = seed2 | 0;
    this.z = 0;
    this.w = 0;
    this.v = ~seed1 | 0;
    this.addend = ((seed1 << 10) ^ (seed2 >>> 4)) | 0;
    for (let i = 0; i < 64; i++) this.nextInt();
  }

  nextInt(): number {
    let t = this.x;
    t = t ^ (t >>> 2);
    this.x = this.y;
    this.y = this.z;
    this.z = this.w;
    const v0 = this.v;
    this.w = v0;
    t = t ^ (t << 1) ^ v0 ^ (v0 << 4);
    this.v = t | 0;
    this.addend = (this.addend + 362437) | 0;
    return (t + this.addend) | 0;
  }

  private nextBits(bitCount: number): number {
    // Int.takeUpperBits(bitCount) = ushr(32 - bitCount) and (-bitCount).shr(31)
    return (this.nextInt() >>> (32 - bitCount)) & (-bitCount >> 31);
  }

  /** nextInt(until): uniform in [0, until). */
  nextIntUntil(until: number): number {
    const n = until | 0;
    if (n <= 0) throw new Error('bound must be positive');
    if ((n & -n) === n) {
      const bitCount = 31 - Math.clz32(n);
      return this.nextBits(bitCount);
    }
    let bits: number;
    let v: number;
    do {
      bits = this.nextInt() >>> 1;
      v = bits % n;
    } while (((bits - v + (n - 1)) | 0) < 0);
    return v;
  }

  /** List.shuffled(random). */
  shuffled<T>(list: readonly T[]): T[] {
    const a = list.slice();
    for (let i = a.length - 1; i >= 1; i--) {
      const j = this.nextIntUntil(i + 1);
      const tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }
    return a;
  }
}

/** String.hashCode() on the JVM. */
export function javaHashCode(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}
