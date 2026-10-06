/**
 * The shared building blocks: the web counterparts of ui/components on Android (ClayButton,
 * GroundScaffold, Jepjep, JepjepAvatarPortrait, ProgressRing, dialogs). Screens compose these rather
 * than inventing their own surfaces, so the web app keeps one visual system.
 */
import type { ComponentChildren, JSX } from 'preact';
import { createPortal } from 'preact/compat';
import { useEffect, useRef, useState } from 'preact/hooks';
import { back } from '../lib/router';
import { ICONS, BRAND, type IconName } from './icons.generated';

// ── Icons ─────────────────────────────────────────────────────────────────────

export function Icon({ name, size = 22, color, class: cls, label }: { name: IconName; size?: number; color?: string; class?: string; label?: string }) {
  const icon = ICONS[name];
  return (
    <svg
      width={size}
      height={size}
      viewBox={icon.viewBox}
      class={cls}
      style={color ? { color } : undefined}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      focusable="false"
      dangerouslySetInnerHTML={{ __html: icon.body }}
    />
  );
}

export function Wordmark({ width = 162 }: { width?: number }) {
  const w = BRAND.wordmark;
  const [, , vw, vh] = w.viewBox.split(' ').map(Number);
  return (
    <svg width={width} height={(width * vh) / vw} viewBox={w.viewBox} role="img" aria-label="KasiGuru" dangerouslySetInnerHTML={{ __html: w.body }} />
  );
}

export function GoogleG({ size = 20 }: { size?: number }) {
  const g = BRAND.googleG;
  return <svg width={size} height={size} viewBox={g.viewBox} aria-hidden="true" dangerouslySetInnerHTML={{ __html: g.body }} />;
}

// ── Jepjep and the avatars ────────────────────────────────────────────────────

export type Pose =
  | 'waving' | 'sleeping' | 'celebrating' | 'with_backpack' | 'peeking' | 'reading' | 'listening'
  | 'playing_a_game' | 'pointing_a_lesson' | 'encouraging_approve' | 'thinking' | 'curious'
  | 'confused' | 'sad' | 'worried' | 'shocked' | 'sitting' | 'speaking';

const POSE_ALT: Record<Pose, string> = {
  waving: 'Jepjep waving', sleeping: 'Jepjep asleep', celebrating: 'Jepjep celebrating',
  with_backpack: 'Jepjep with a backpack', peeking: 'Jepjep peeking out', reading: 'Jepjep reading a book',
  listening: 'Jepjep listening', playing_a_game: 'Jepjep playing a game', pointing_a_lesson: 'Jepjep pointing at a lesson',
  encouraging_approve: 'Jepjep giving a thumbs up', thinking: 'Jepjep thinking', curious: 'Jepjep looking curious',
  confused: 'Jepjep looking puzzled', sad: 'Jepjep looking sad', worried: 'Jepjep looking worried',
  shocked: 'Jepjep amazed', sitting: 'Jepjep sitting on a leaf', speaking: 'Jepjep speaking',
};

export function Jepjep({ pose, height, breathe = false, decorative = false, style }: { pose: Pose; height: number; breathe?: boolean; decorative?: boolean; style?: JSX.CSSProperties }) {
  return (
    <img
      src={`/img/jepjep/${pose}.webp`}
      alt={decorative ? '' : POSE_ALT[pose]}
      class={`jepjep${breathe ? ' breathe' : ''}`}
      style={{ height, width: 'auto', ...style }}
      draggable={false}
      decoding="async"
    />
  );
}

/** JepjepAvatar: ids are stored in progress and on the leaderboard, so they never change. */
export const AVATARS = [
  { id: 1, file: 'adventurer', name: 'Adventurer' },
  { id: 2, file: 'scholar', name: 'Scholar' },
  { id: 3, file: 'king', name: 'King' },
  { id: 4, file: 'beach', name: 'Beach' },
  { id: 5, file: 'cool', name: 'Cool' },
  { id: 6, file: 'farmer', name: 'Farmer' },
  { id: 7, file: 'sunglass', name: 'Sunglasses' },
  { id: 8, file: 'sleepy', name: 'Sleepy' },
  { id: 9, file: 'ips', name: 'Heritage' },
];
export const avatarFor = (id: number | null | undefined) => AVATARS.find((a) => a.id === id) ?? AVATARS[0];

export function Avatar({ id, size = 56, level, selected, onClick, label }: { id: number; size?: number; level?: number; selected?: boolean; onClick?: () => void; label?: string }) {
  const a = avatarFor(id);
  const inner = (
    <>
      <img src={`/img/avatars/${a.file}.webp`} alt="" draggable={false} />
      {level != null && <span class="level">{level}</span>}
    </>
  );
  const style = { width: size, height: size };
  return onClick ? (
    <button class={`avatar${selected ? ' selected' : ''}`} style={style} onClick={onClick} aria-label={label ?? `${a.name} avatar`} aria-pressed={selected}>
      {inner}
    </button>
  ) : (
    <div class={`avatar${selected ? ' selected' : ''}`} style={style} role="img" aria-label={label ?? `${a.name} avatar`}>
      {inner}
    </div>
  );
}

// ── Buttons ───────────────────────────────────────────────────────────────────

type Tone = 'primary' | 'reward' | 'quiet' | 'danger' | 'coral';

export function ClayButton({
  label, onClick, tone = 'primary', disabled, block = true, icon, small, type = 'button', children,
}: { label?: string; onClick?: () => void; tone?: Tone; disabled?: boolean; block?: boolean; icon?: IconName; small?: boolean; type?: 'button' | 'submit'; children?: ComponentChildren }) {
  const cls = ['clay', tone !== 'primary' ? tone : '', block ? 'block' : '', small ? 'small' : ''].filter(Boolean).join(' ');
  return (
    <button type={type} class={cls} onClick={onClick} disabled={disabled}>
      {icon && <Icon name={icon} size={20} />}
      {label}
      {children}
    </button>
  );
}

// ── Ground scaffold ───────────────────────────────────────────────────────────

/**
 * The Ground shell: a fixed 56px bar with Back and actions, the page on night below it, and a
 * hairline that appears only once the page has scrolled.
 */
export function GroundScaffold({
  title, onBack, actions, children, nav = false, backLabel = 'Back', largeTitle, subtitle, wide,
}: {
  title: string;
  onBack?: (() => void) | false;
  actions?: ComponentChildren;
  children: ComponentChildren;
  nav?: boolean;
  backLabel?: string;
  largeTitle?: boolean;
  subtitle?: string;
  wide?: boolean;
}) {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const on = () => setScrolled(window.scrollY > (largeTitle ? 48 : 4));
    on();
    window.addEventListener('scroll', on, { passive: true });
    return () => window.removeEventListener('scroll', on);
  }, [largeTitle]);
  useEffect(() => {
    document.title = title === 'KasiGuru' ? 'KasiGuru' : `${title} · KasiGuru`;
  }, [title]);
  const showBack = onBack !== false;
  // Tab screens and content-heavy screens use the wider column on a computer (see .wide in CSS).
  const isWide = wide || nav;
  return (
    <>
      <header class={`topbar${scrolled ? ' scrolled' : ''}${isWide ? ' wide' : ''}`}>
        <div class="topbar-inner">
          {showBack ? (
            <button class="icon-btn" onClick={() => (onBack ? onBack() : back())} aria-label={backLabel}>
              <Icon name="arrowLeft" size={24} />
            </button>
          ) : null}
          <h1 class="title t-title-l" style={{ opacity: largeTitle && !scrolled ? 0 : 1, transition: 'opacity 150ms' }} aria-hidden={largeTitle && !scrolled}>
            {title}
          </h1>
          {actions}
        </div>
      </header>
      <main id="main-content" tabIndex={-1} class={`page${nav ? '' : ' no-nav'}${isWide ? ' wide' : ''}`} style={{ paddingTop: 'var(--s-xs)' }}>
        {largeTitle && (
          <div style={{ marginBottom: 'var(--s-md)' }}>
            <h2 class="t-headline">{title}</h2>
            {subtitle && <p class="t-body muted" style={{ marginTop: 4 }}>{subtitle}</p>}
          </div>
        )}
        {children}
      </main>
    </>
  );
}

export function SectionHeading({ text, action }: { text: string; action?: ComponentChildren }) {
  return (
    <div class="section-heading">
      <h3 class="t-title-l">{text}</h3>
      {action}
    </div>
  );
}

// ── Progress ──────────────────────────────────────────────────────────────────

export function ProgressRing({ value, size = 72, stroke = 7, color = 'var(--gold)', children, label }: { value: number; size?: number; stroke?: number; color?: string; children?: ComponentChildren; label?: string }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const v = Math.max(0, Math.min(1, value));
  return (
    <div style={{ position: 'relative', width: size, height: size }} role="img" aria-label={label}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }} aria-hidden="true">
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--track)" stroke-width={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={color}
          stroke-width={stroke}
          stroke-linecap="round"
          stroke-dasharray={c}
          stroke-dashoffset={c * (1 - v)}
          style={{ transition: 'stroke-dashoffset 600ms var(--ease-out)' }}
        />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center' }}>{children}</div>
    </div>
  );
}

export function ProgressBar({ value, color, height = 8, label }: { value: number; color?: string; height?: number; label?: string }) {
  const v = Math.max(0, Math.min(1, value));
  return (
    <div class="bar-track" style={{ height }} role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(v * 100)} aria-label={label}>
      <div class="bar-fill" style={{ width: `${v * 100}%`, background: color }} />
    </div>
  );
}

/** A number that counts up rather than appearing (Motion: XP counters, 600ms ease-out). */
export function CountUp({ to, duration = 600 }: { to: number; duration?: number }) {
  const [v, setV] = useState(0);
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setV(to);
      return;
    }
    let raf = 0;
    const start = performance.now();
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / duration);
      setV(Math.round(to * (1 - Math.pow(1 - p, 3))));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [to]);
  return <>{v}</>;
}

// ── States ────────────────────────────────────────────────────────────────────

export function Loading({ label = 'Loading' }: { label?: string }) {
  return (
    <div class="loading" role="status">
      <div class="spinner" />
      <p class="t-body">{label}</p>
    </div>
  );
}

export function EmptyState({ pose = 'curious', title, message, actionLabel, onAction }: { pose?: Pose; title: string; message?: string; actionLabel?: string; onAction?: () => void }) {
  return (
    <div class="center stack" style={{ padding: 'var(--s-xl) var(--s-md)' }}>
      <div style={{ display: 'grid', placeItems: 'center' }}>
        <Jepjep pose={pose} height={140} breathe />
      </div>
      <h2 class="t-headline-s">{title}</h2>
      {message && <p class="t-body-l muted">{message}</p>}
      {actionLabel && onAction && (
        <div class="readable">
          <ClayButton label={actionLabel} onClick={onAction} />
        </div>
      )}
    </div>
  );
}

// ── Dialogs & toasts ──────────────────────────────────────────────────────────

export function Dialog({ children, onClose, glow, label }: { children: ComponentChildren; onClose?: () => void; glow?: boolean; label: string }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const prev = document.activeElement as HTMLElement | null;
    ref.current?.focus();
    const key = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && onClose) onClose();
    };
    document.addEventListener('keydown', key);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', key);
      document.body.style.overflow = '';
      prev?.focus?.();
    };
  }, []);
  return createPortal(
    <div class="scrim" onClick={(e) => e.target === e.currentTarget && onClose?.()}>
      <div class={`dialog${glow ? ' glow' : ''}`} role="dialog" aria-modal="true" aria-label={label} tabIndex={-1} ref={ref}>
        {children}
      </div>
    </div>,
    document.body
  );
}

export function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel, danger }: { title: string; message: string; confirmLabel: string; onConfirm: () => void; onCancel: () => void; danger?: boolean }) {
  return (
    <Dialog onClose={onCancel} label={title}>
      <div class="stack">
        <h2 class="t-headline-s">{title}</h2>
        <p class="t-body-l muted">{message}</p>
        <div class="stack-sm">
          <ClayButton label={confirmLabel} tone={danger ? 'danger' : 'primary'} onClick={onConfirm} />
          <ClayButton label="Cancel" tone="quiet" onClick={onCancel} />
        </div>
      </div>
    </Dialog>
  );
}

let toastSetter: ((m: string | null) => void) | null = null;
let toastTimer: ReturnType<typeof setTimeout> | undefined;

export function toast(message: string) {
  clearTimeout(toastTimer);
  toastSetter?.(message);
  toastTimer = setTimeout(() => toastSetter?.(null), 3200);
}

export function ToastHost() {
  const [msg, setMsg] = useState<string | null>(null);
  useEffect(() => {
    toastSetter = setMsg;
    return () => {
      toastSetter = null;
    };
  }, []);
  return msg ? (
    <div class="toast" role="status" aria-live="polite">
      {msg}
    </div>
  ) : null;
}

export function Confetti({ pieces = 70 }: { pieces?: number }) {
  const [items] = useState(() =>
    Array.from({ length: pieces }, () => ({
      left: Math.random() * 100,
      delay: Math.random() * 0.8,
      duration: 2.2 + Math.random() * 1.8,
      color: ['var(--lime)', 'var(--gold)', 'var(--coral)', 'var(--cream)', 'var(--info)'][Math.floor(Math.random() * 5)],
      rotate: Math.random() * 360,
    }))
  );
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return null;
  return (
    <div class="confetti" aria-hidden="true">
      {items.map((p, i) => (
        <i
          key={i}
          style={{
            left: `${p.left}%`,
            background: p.color,
            animationDelay: `${p.delay}s`,
            animationDuration: `${p.duration}s`,
            transform: `rotate(${p.rotate}deg)`,
          }}
        />
      ))}
    </div>
  );
}

/** Text with one highlighted phrase in brand lime, as the onboarding headings do. */
export function Highlighted({ text, words }: { text: string; words: string[] }) {
  const ranges: [number, number][] = [];
  for (const w of words) {
    if (!w) continue;
    const i = text.lastIndexOf(w);
    if (i >= 0) ranges.push([i, i + w.length]);
  }
  ranges.sort((a, b) => a[0] - b[0]);
  const parts: ComponentChildren[] = [];
  let at = 0;
  for (const [s, e] of ranges) {
    if (s < at) continue;
    if (s > at) parts.push(text.slice(at, s));
    parts.push(<span class="hl">{text.slice(s, e)}</span>);
    at = e;
  }
  parts.push(text.slice(at));
  return <>{parts}</>;
}

/** Scenery: Adrian's seven illustrated Casiguran places. */
export const SCENES = {
  casapsapan: 'Casapsapan Beach at sunset',
  ermita_hill: 'The steps up Ermita Hill',
  farm: 'A farm in Casiguran',
  forest: 'The forest',
  ontok_lighthouse: 'Ontok Lighthouse',
  river: 'The river',
  tibu_tidal_pool: 'Tibu Tidal Pool',
} as const;
export type SceneId = keyof typeof SCENES;
const SCENE_ORDER = Object.keys(SCENES) as SceneId[];

const SCENE_BY_SECTION: Record<string, SceneId> = {
  pagbati: 'casapsapan', pamilya: 'farm', tahanan: 'farm', pagkain: 'farm', paglalakbay: 'ontok_lighthouse',
  katawan: 'river', kalikasan: 'forest', hayop: 'tibu_tidal_pool', bilang: 'ermita_hill', kabuhayan: 'casapsapan',
  kilos: 'river', paglalarawan: 'tibu_tidal_pool', araw_araw: 'forest',
};
const SCENE_BY_CATEGORY: Record<string, SceneId> = {
  'Animals & Wildlife': 'tibu_tidal_pool', 'Nature & Environment': 'forest', 'Body Parts & Health': 'river',
  'Food & Dining': 'farm', 'House & Daily Life': 'farm', 'Greetings & Essentials': 'casapsapan', 'Family & People': 'farm',
  'Occupations & Tools': 'casapsapan', 'Numbers & Time': 'ermita_hill', 'Emotions & Feelings': 'river',
  'Colors & Shapes': 'tibu_tidal_pool', 'Weather & Climate': 'ontok_lighthouse',
};
export const sceneForSection = (id: string): SceneId => SCENE_BY_SECTION[id] ?? 'forest';
export const sceneForCategory = (c: string): SceneId => SCENE_BY_CATEGORY[c] ?? 'forest';

/** CategoryRegistry's order, which is the order the Library lists its tiles in on Android. */
export const LIBRARY_CATEGORY_ORDER = [
  'Greetings & Essentials', 'Food & Dining', 'Animals & Wildlife', 'Body Parts & Health', 'Numbers & Time',
  'Weather & Climate', 'Emotions & Feelings', 'House & Daily Life', 'Nature & Environment', 'Family & People',
  'Colors & Shapes', 'Occupations & Tools',
];
const ART_BY_SHORT_NAME: Record<string, string> = {
  greetings: 'greetings', food: 'food', animals: 'animals', 'body parts': 'health', numbers: 'numbers',
  weather: 'weather', emotions: 'emotions', house: 'house', nature: 'nature', family: 'family',
  colors: 'colors', occupations: 'occupations',
};
/**
 * The category's illustrated icon (Android's CategoryRegistry.getMeta): matched on the part before
 * " &", so "Food & Drinks" added in the admin portal still gets the Food art; anything else, General's.
 */
export const categoryArt = (c: string) =>
  `/img/categories/${ART_BY_SHORT_NAME[c.split(' &')[0].trim().toLowerCase()] ?? 'general'}.webp`;
export const sceneForIndex = (i: number): SceneId => SCENE_ORDER[(((i - 1) % SCENE_ORDER.length) + SCENE_ORDER.length) % SCENE_ORDER.length];
export const sceneUrl = (id: SceneId) => `/img/scenes/${id}.webp`;

export function Scene({ id, locked, height, radius = 'var(--r-panel)', children, style }: { id: SceneId; locked?: boolean; height: number | string; radius?: string; children?: ComponentChildren; style?: JSX.CSSProperties }) {
  return (
    <div class={`scene${locked ? ' locked' : ''}`} style={{ height, borderRadius: radius, border: '1px solid var(--hair)', ...style }}>
      <img src={sceneUrl(id)} alt="" loading="lazy" decoding="async" />
      <div class="over" style={{ height: '100%' }}>
        {children}
      </div>
    </div>
  );
}
