#!/usr/bin/env node
/**
 * Regenerates the web app's icons and brand art from the Android sources, so the two apps draw
 * exactly the same shapes.
 *
 *   node scripts/build-icons.mjs [path-to-iconsax-res-drawable]
 *
 * - Iconsax: the Android app uses the io.eyram.iconsax library's Bulk (and one Linear) vectors. The
 *   library is not on npm, so the vectors are read out of its unpacked AAR in the Gradle cache and
 *   written to src/ui/icons.generated.ts with #292D32 swapped for currentColor. Found automatically
 *   under ~/.gradle; pass the drawable folder explicitly if it lives elsewhere.
 * - Brand vectors (wordmark, launcher K, Google G) come from app/src/main/res/drawable.
 * - Jepjep, the avatars and the scenes are copied as-is from res/drawable-nodpi (WebP already).
 *
 * The output is committed, so a normal build never needs the Gradle cache.
 */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const webapp = path.resolve(here, '..');
const repo = path.resolve(webapp, '..', '..');
const res = path.join(repo, 'app', 'src', 'main', 'res');

/** Web name -> Android drawable (without .xml). Mirrors ui/theme/Iconsax.kt. */
const ICONS = {
  home: 'bulk_home',
  book: 'bulk_book',
  repeat: 'bulk_repeat',
  element4: 'bulk_element_4',
  profile: 'bulk_profile_circle',
  arrowLeft: 'bulk_arrow_left',
  arrowRight: 'bulk_arrow_right',
  arrowUp: 'bulk_arrow_up',
  arrowDown: 'bulk_arrow_down',
  people: 'bulk_people',
  profile2user: 'bulk_profile_2user',
  sms: 'bulk_sms',
  notification: 'bulk_notification',
  document: 'bulk_document',
  hashtagDown: 'bulk_hashtag_down',
  teacher: 'bulk_teacher',
  cup: 'bulk_cup',
  medal: 'bulk_medal',
  medalStar: 'bulk_medal_star',
  star: 'bulk_star_1',
  flash: 'bulk_flash',
  volumeHigh: 'bulk_volume_high',
  volumeUp: 'bulk_volume_up',
  play: 'bulk_play',
  playCircle: 'bulk_play_circle',
  edit: 'bulk_edit',
  keyboard: 'bulk_keyboard',
  search: 'linear_search_normal',
  setting: 'bulk_setting_2',
  refresh: 'bulk_refresh',
  logout: 'bulk_logout',
  lock: 'bulk_lock',
  tickCircle: 'bulk_tick_circle',
  tickSquare: 'bulk_tick_square',
  closeCircle: 'bulk_close_circle',
  infoCircle: 'bulk_info_circle',
  addCircle: 'bulk_add_circle',
  calendar: 'bulk_calendar',
  global: 'bulk_global',
  game: 'bulk_game',
  location: 'bulk_location',
  courthouse: 'bulk_courthouse',
  moon: 'bulk_moon',
  trash: 'bulk_trush_square',
  eye: 'linear_eye',
  eyeSlash: 'linear_eye_slash',
  share: 'bulk_export',
  mobile: 'bulk_mobile',
  shieldTick: 'bulk_shield_tick',
  message: 'bulk_message_question',
  lampCharge: 'bulk_lamp_charge',
  lampOn: 'bulk_lamp_on',
  danger: 'bulk_danger',
  ranking: 'bulk_ranking',
  crown: 'bulk_crown_1',
  volumeSlash: 'bulk_volume_slash',
  translate: 'bulk_translate',
  tree: 'bulk_tree',
  closeSquare: 'bulk_close_square',
  gallery: 'bulk_gallery',
  messageText: 'bulk_message_text',
  shuffle: 'bulk_shuffle',
};

/** Brand vectors kept in their own colours. */
const BRAND = {
  wordmark: 'kasiguru_wordmark',
  googleG: 'ic_google_g',
};

function findIconsaxDrawables(explicit) {
  if (explicit) return explicit;
  const transforms = path.join(os.homedir(), '.gradle', 'caches');
  const stack = [transforms];
  // Shallow, targeted walk: caches/<ver>/transforms/<hash>/transformed/iconsax-android-*/res/drawable
  for (const ver of fs.existsSync(transforms) ? fs.readdirSync(transforms) : []) {
    const t = path.join(transforms, ver, 'transforms');
    if (!fs.existsSync(t)) continue;
    for (const hash of fs.readdirSync(t)) {
      const dir = path.join(t, hash, 'transformed');
      if (!fs.existsSync(dir)) continue;
      for (const name of fs.readdirSync(dir)) {
        if (!name.startsWith('iconsax-android')) continue;
        const drawable = path.join(dir, name, 'res', 'drawable');
        if (fs.existsSync(path.join(drawable, 'bulk_home.xml'))) return drawable;
      }
    }
  }
  void stack;
  return null;
}

const attr = (el, name) => {
  const m = el.match(new RegExp(`android:${name}="([^"]*)"`));
  return m ? m[1] : null;
};

/** Android #AARRGGBB / #RRGGBB -> { color, alpha }. */
function color(value, iconMode) {
  if (!value) return null;
  let hex = value.replace('#', '');
  let alpha = 1;
  if (hex.length === 8) {
    alpha = parseInt(hex.slice(0, 2), 16) / 255;
    hex = hex.slice(2);
  }
  if (alpha === 0) return { color: 'none', alpha: 1 };
  const c = `#${hex}`;
  return { color: iconMode && c.toUpperCase() === '#292D32' ? 'currentColor' : c, alpha };
}

const round = (n) => Math.round(n * 1000) / 1000;

/** Converts one VectorDrawable document to { viewBox, body } SVG markup. */
function convert(xml, iconMode) {
  const vw = attr(xml, 'viewportWidth');
  const vh = attr(xml, 'viewportHeight');
  const out = [];
  // Tokenise into <group ...>, </group>, <path .../>
  const tokens = xml.match(/<group[^>]*?>|<\/group>|<path[\s\S]*?\/>/g) || [];
  for (const tok of tokens) {
    if (tok.startsWith('</group')) {
      out.push('</g>');
    } else if (tok.startsWith('<group')) {
      const tx = parseFloat(attr(tok, 'translateX') || '0');
      const ty = parseFloat(attr(tok, 'translateY') || '0');
      const sx = parseFloat(attr(tok, 'scaleX') || '1');
      const sy = parseFloat(attr(tok, 'scaleY') || '1');
      const rot = parseFloat(attr(tok, 'rotation') || '0');
      const px = parseFloat(attr(tok, 'pivotX') || '0');
      const py = parseFloat(attr(tok, 'pivotY') || '0');
      const t = [];
      if (tx || ty) t.push(`translate(${round(tx)} ${round(ty)})`);
      if (rot) t.push(`rotate(${round(rot)} ${round(px)} ${round(py)})`);
      if (sx !== 1 || sy !== 1) t.push(`translate(${round(px)} ${round(py)}) scale(${round(sx)} ${round(sy)}) translate(${round(-px)} ${round(-py)})`);
      out.push(t.length ? `<g transform="${t.join(' ')}">` : '<g>');
    } else {
      const d = attr(tok, 'pathData');
      if (!d) continue;
      const fill = color(attr(tok, 'fillColor'), iconMode);
      const stroke = color(attr(tok, 'strokeColor'), iconMode);
      const fillAlpha = parseFloat(attr(tok, 'fillAlpha') || '1') * (fill?.alpha ?? 1);
      const strokeAlpha = parseFloat(attr(tok, 'strokeAlpha') || '1') * (stroke?.alpha ?? 1);
      const parts = [`d="${d}"`];
      parts.push(`fill="${fill ? fill.color : 'none'}"`);
      if (fill && fill.color !== 'none' && fillAlpha < 1) parts.push(`fill-opacity="${round(fillAlpha)}"`);
      if (attr(tok, 'fillType') === 'evenOdd') parts.push('fill-rule="evenodd"');
      if (stroke && stroke.color !== 'none') {
        parts.push(`stroke="${stroke.color}"`);
        parts.push(`stroke-width="${attr(tok, 'strokeWidth') || '1'}"`);
        if (strokeAlpha < 1) parts.push(`stroke-opacity="${round(strokeAlpha)}"`);
        const cap = attr(tok, 'strokeLineCap');
        const join = attr(tok, 'strokeLineJoin');
        if (cap) parts.push(`stroke-linecap="${cap}"`);
        if (join) parts.push(`stroke-linejoin="${join}"`);
      }
      out.push(`<path ${parts.join(' ')}/>`);
    }
  }
  return { viewBox: `0 0 ${vw} ${vh}`, body: out.join('') };
}

async function main() {
  const drawable = findIconsaxDrawables(process.argv[2]);
  if (!drawable) {
    console.error('Iconsax drawables not found. Build the Android app once, or pass the path.');
    process.exit(1);
  }

  const icons = {};
  for (const [name, file] of Object.entries(ICONS)) {
    const p = path.join(drawable, `${file}.xml`);
    if (!fs.existsSync(p)) {
      console.warn(`missing ${file}.xml, skipped ${name}`);
      continue;
    }
    icons[name] = convert(fs.readFileSync(p, 'utf8'), true);
  }

  const brand = {};
  for (const [name, file] of Object.entries(BRAND)) {
    brand[name] = convert(fs.readFileSync(path.join(res, 'drawable', `${file}.xml`), 'utf8'), false);
  }

  const banner = '// Generated by scripts/build-icons.mjs from the Android vectors. Do not edit by hand.\n';
  const ts =
    banner +
    `export const ICONS = ${JSON.stringify(icons, null, 1)} as const;\n\n` +
    `export type IconName = keyof typeof ICONS;\n\n` +
    `export const BRAND = ${JSON.stringify(brand, null, 1)} as const;\n`;
  fs.writeFileSync(path.join(webapp, 'src', 'ui', 'icons.generated.ts'), ts);

  await appIcons();
  fs.writeFileSync(
    path.join(webapp, 'public', 'icons', 'wordmark.svg'),
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${brand.wordmark.viewBox}">${brand.wordmark.body}</svg>`
  );

  // Raster art, copied untouched.
  const nodpi = path.join(res, 'drawable-nodpi');
  let copied = 0;
  for (const file of fs.readdirSync(nodpi)) {
    const dest = file.startsWith('jepjep_') ? 'jepjep' : file.startsWith('avatar_') ? 'avatars' : file.startsWith('scene_') ? 'scenes' : null;
    if (!dest) continue;
    const name = file.replace(/^(jepjep_|avatar_|scene_)/, '');
    fs.copyFileSync(path.join(nodpi, file), path.join(webapp, 'public', 'img', dest, name));
    copied++;
  }

  // Lesson sounds (UiFeedbackSounds), the same files the APK ships.
  fs.mkdirSync(path.join(webapp, 'public', 'sounds'), { recursive: true });
  for (const [from, to] of [['ui_correct.wav', 'correct.wav'], ['ui_wrong.wav', 'wrong.wav'], ['ui_level_up.wav', 'level-up.wav']]) {
    fs.copyFileSync(path.join(res, 'raw', from), path.join(webapp, 'public', 'sounds', to));
  }

  // Bundled fonts, the same files the APK ships.
  for (const f of ['fredoka_semibold.ttf', 'fredoka_bold.ttf', 'dm_sans_regular.ttf', 'dm_sans_medium.ttf', 'dm_sans_bold.ttf']) {
    fs.copyFileSync(path.join(res, 'font', f), path.join(webapp, 'public', 'fonts', f));
  }

  console.log(`icons: ${Object.keys(icons).length}, brand: ${Object.keys(brand).length}, images: ${copied}, fonts: 5`);
}

/**
 * The home-screen and tab icons, cut from Adrian's Jepjep launcher art (mipmap ic_launcher_foreground,
 * on its #067000 backing). Android shows the 72dp safe zone of the 108dp foreground, so the "any"
 * icons are that centre square; the maskable one puts it inside the 80% circle browsers keep.
 * Needs sharp, which is not a dependency of the app: `npm i --no-save sharp` first.
 */
async function appIcons() {
  let sharp;
  try {
    sharp = (await import('sharp')).default;
  } catch {
    console.warn('sharp not installed: app icons skipped (npm i --no-save sharp, then run again).');
    return;
  }
  const foreground = path.join(res, 'mipmap-xxxhdpi', 'ic_launcher_foreground.png');
  const size = (await sharp(foreground).metadata()).width;
  const inset = Math.round((size * 18) / 108);
  const safe = () => sharp(foreground).extract({ left: inset, top: inset, width: size - 2 * inset, height: size - 2 * inset });
  const out = (name) => path.join(webapp, 'public', 'icons', name);
  for (const [name, px] of [['icon-512.png', 512], ['icon-192.png', 192], ['apple-touch-icon.png', 180], ['favicon-32.png', 32]]) {
    await safe().resize(px, px, { kernel: 'lanczos3' }).png({ compressionLevel: 9 }).toFile(out(name));
  }
  const inner = Math.round(512 * 0.8);
  await sharp({ create: { width: 512, height: 512, channels: 3, background: '#067000' } })
    .composite([{ input: await safe().resize(inner, inner, { kernel: 'lanczos3' }).png().toBuffer(), gravity: 'centre' }])
    .png({ compressionLevel: 9 })
    .toFile(out('icon-maskable-512.png'));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
