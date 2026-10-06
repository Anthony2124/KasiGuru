/**
 * Light, Dark or System, as Android's Appearance screen offers them (AppearanceScreen, ThemeMode).
 * System is the default there and here: it follows the device and changes with it.
 *
 * The choice is kept in Prefs like every other setting, and copied to localStorage so
 * public/theme-boot.js can apply it before the first paint.
 */
export type ThemeChoice = 'system' | 'light' | 'dark';

const BOOT_KEY = 'kg-theme';
const GROUND = { light: '#F6F4EA', dark: '#0A0E0D' };
const lightQuery = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia('(prefers-color-scheme: light)') : null;
let current: ThemeChoice = 'system';

function paint() {
  const light = current === 'light' || (current === 'system' && !!lightQuery?.matches);
  const mode = light ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', mode);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', GROUND[mode]);
}

export function applyTheme(choice: ThemeChoice) {
  current = choice;
  try {
    localStorage.setItem(BOOT_KEY, choice);
  } catch {
    // Private mode or storage blocked: the theme still applies for this visit.
  }
  paint();
}

// A device switching between light and dark moves a System page with it.
lightQuery?.addEventListener?.('change', () => {
  if (current === 'system') paint();
});
