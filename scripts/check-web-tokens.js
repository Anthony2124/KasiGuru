/**
 * Guards the brand core shared by KasiGuru's two web surfaces.
 *
 * The surfaces are deliberately separate stylesheets, not two copies of one. The
 * download site is Persuade mode and the admin portal is Operate mode, and their token
 * vocabularies reflect that: the admin defines a spacing and type scale (--s-1..--s-12,
 * --t-xs..--t-xl) and status colours it alone needs, while the download site defines
 * clay lips, bands and wrappers the admin has no use for. Of their 62 and 46 tokens,
 * only 17 names are common. An earlier attempt to unify them behind admin-website/shared
 * plus a sync script was reverted (db24c80); this does not reintroduce that machinery.
 *
 * What must never drift is the small identity core underneath: the brand colours and the
 * corner radii. Those are the product's face, and the two surfaces sit side by side in
 * front of the same audience. This asserts they still agree, and nothing else — it moves
 * no CSS and generates no files.
 *
 * KNOWN_DIVERGENT records the three shared names that legitimately differ per surface,
 * so their difference is a recorded decision instead of a silent trap for anyone copying
 * a rule from one stylesheet into the other. If one of them ever comes to agree on its
 * own, this says so rather than failing.
 *
 * Usage: node scripts/check-web-tokens.js
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SURFACES = {
  admin: path.join(ROOT, 'admin-website/admin/css/styles.css'),
  download: path.join(ROOT, 'admin-website/download/css/styles.css'),
};

// Identity, not layout: colours and radii. A token earns a place here by being something
// a user would notice differing between the two sites in the same browsing session.
const CORE = [
  '--violet', '--canopy-top', '--canopy-bottom',
  '--ink', '--muted', '--faint', '--ground', '--surface',
  '--gold', '--coral',
  '--r-chip', '--r-tile', '--r-panel', '--r-pill',
];

const KNOWN_DIVERGENT = {
  '--hair':
    'The download site draws hairlines over the violet band as well as over cards, so ' +
    'it needs a translucent rule; the admin only ever draws them on white.',
  '--sunken':
    'A recessed area is tinted against the surface it sits in, and the two surfaces do ' +
    'not share a ground.',
  '--ease-out':
    'Motion is tuned per surface: the download page overshoots for emphasis, the admin ' +
    'settles quickly because its animations run during data entry.',
};

// Only :root blocks — a token redefined inside a component or a media query is that
// component's business, not part of the shared contract.
function readRootTokens(file) {
  const css = fs.readFileSync(file, 'utf8');
  const tokens = new Map();
  const blockStart = /:root[^{]*\{/g;
  let match;
  while ((match = blockStart.exec(css)) !== null) {
    const from = match.index + match[0].length;
    const to = css.indexOf('}', from);
    if (to === -1) continue;
    const body = css.slice(from, to);
    for (const decl of body.split(';')) {
      const m = decl.match(/(--[A-Za-z0-9-]+)\s*:\s*(.+)/s);
      if (!m) continue;
      // Whitespace inside a value is not a difference: `cubic-bezier(0.16, 1, 0.3, 1)`
      // and `cubic-bezier(0.16,1,0.3,1)` are the same curve.
      if (!tokens.has(m[1])) tokens.set(m[1], m[2].replace(/\s+/g, '').toLowerCase());
    }
  }
  return tokens;
}

const tokens = Object.fromEntries(
  Object.entries(SURFACES).map(([name, file]) => {
    if (!fs.existsSync(file)) {
      console.error(`Missing stylesheet: ${path.relative(ROOT, file)}`);
      process.exit(1);
    }
    return [name, readRootTokens(file)];
  })
);

const problems = [];
const notes = [];

for (const token of CORE) {
  const a = tokens.admin.get(token);
  const d = tokens.download.get(token);
  if (a === undefined || d === undefined) {
    problems.push(
      `${token} is part of the shared brand core but is missing from ` +
      `${a === undefined ? 'admin' : 'download'}/css/styles.css.`
    );
  } else if (a !== d) {
    problems.push(`${token} has drifted — admin: ${a}, download: ${d}.`);
  }
}

for (const [token, reason] of Object.entries(KNOWN_DIVERGENT)) {
  const a = tokens.admin.get(token);
  const d = tokens.download.get(token);
  if (a !== undefined && d !== undefined && a === d) {
    notes.push(
      `${token} now agrees on both surfaces (${a}). If that was intended, move it into ` +
      `CORE so it stays that way. Recorded reason for the difference: ${reason}`
    );
  }
}

// A shared name nobody has classified is the actual trap: same token, different value,
// no decision behind it.
for (const [token, a] of tokens.admin) {
  if (CORE.includes(token) || token in KNOWN_DIVERGENT) continue;
  const d = tokens.download.get(token);
  if (d !== undefined && d !== a) {
    problems.push(
      `${token} is defined on both surfaces with different values (admin: ${a}, ` +
      `download: ${d}) and is neither in the brand core nor a recorded divergence. ` +
      `Decide which it is: add it to CORE and make the values agree, or add it to ` +
      `KNOWN_DIVERGENT with the reason.`
    );
  }
}

for (const note of notes) console.log(`note: ${note}`);

if (problems.length) {
  console.error('\nWeb token contract violated:\n');
  for (const p of problems) console.error(`  - ${p}`);
  console.error('');
  process.exit(1);
}

console.log(
  `Brand core intact: ${CORE.length} tokens agree across both surfaces, ` +
  `${Object.keys(KNOWN_DIVERGENT).length} recorded divergences.`
);
