/**
 * A small history router. Paths mirror the Android route names (Screen.kt) so a link reads the same
 * on both, and the browser Back button behaves like Android's system Back.
 */
import { useEffect, useState } from 'preact/hooks';

type Listener = () => void;
const listeners = new Set<Listener>();
/** Depth of in-app navigation, so Back can fall back to Home when the app was opened on a deep link. */
let depth = 0;

function notify() {
  listeners.forEach((l) => l());
}

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', (e) => {
    depth = (e.state && typeof e.state.depth === 'number' ? e.state.depth : 0) as number;
    notify();
  });
  history.replaceState({ depth: 0 }, '');
}

export function navigate(path: string, opts: { replace?: boolean; state?: Record<string, unknown> } = {}) {
  if (path === location.pathname + location.search && !opts.state) return;
  if (opts.replace) {
    history.replaceState({ depth, ...(opts.state ?? {}) }, '', path);
  } else {
    depth += 1;
    history.pushState({ depth, ...(opts.state ?? {}) }, '', path);
  }
  window.scrollTo(0, 0);
  notify();
}

/** Switches between the five tabs without piling them onto the history stack. */
export function switchTab(path: string) {
  if (location.pathname === path) {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  history.replaceState({ depth }, '', path);
  window.scrollTo(0, 0);
  notify();
}

export function back(fallback = '/') {
  if (depth > 0) history.back();
  else navigate(fallback, { replace: true });
}

export function routeState<T = Record<string, unknown>>(): T | undefined {
  return (history.state ?? undefined) as T | undefined;
}

export function useLocation() {
  const [, force] = useState(0);
  useEffect(() => {
    const l = () => force((n) => n + 1);
    listeners.add(l);
    return () => {
      listeners.delete(l);
    };
  }, []);
  return { path: location.pathname, query: new URLSearchParams(location.search) };
}

/** Matches `/games/:type/:level` style patterns; returns decoded params or null. */
export function match(pattern: string, path: string): Record<string, string> | null {
  const p = pattern.split('/').filter(Boolean);
  const s = path.split('/').filter(Boolean);
  if (p.length !== s.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < p.length; i++) {
    if (p[i].startsWith(':')) params[p[i].slice(1)] = decodeURIComponent(s[i]);
    else if (p[i] !== s[i]) return null;
  }
  return params;
}

export const enc = encodeURIComponent;
