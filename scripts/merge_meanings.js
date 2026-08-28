/**
 * Merges an authored batch into scripts/meanings.json.
 *
 *   node scripts/merge_meanings.js <batch.json>
 *
 * Reports collisions where a key already had a different definition, so a later batch cannot
 * quietly overwrite an earlier one without it being visible.
 */
const fs = require('fs');
const path = require('path');
const target = path.join(__dirname, 'meanings.json');
const batchPath = process.argv[2];
if (!batchPath) { console.error('Usage: node scripts/merge_meanings.js <batch.json>'); process.exit(2); }

const base = fs.existsSync(target) ? JSON.parse(fs.readFileSync(target, 'utf8')) : {};
const batch = JSON.parse(fs.readFileSync(batchPath, 'utf8'));
const collisions = [];
let added = 0;

for (const [k, v] of Object.entries(batch)) {
  const key = k.trim().toLowerCase();
  if (base[key] && JSON.stringify(base[key]) !== JSON.stringify(v)) collisions.push(key);
  if (!base[key]) added++;
  base[key] = v;
}

const sorted = {};
Object.keys(base).sort().forEach((k) => { sorted[k] = base[k]; });
fs.writeFileSync(target, JSON.stringify(sorted, null, 2) + '\n', 'utf8');
console.log('Batch entries: ' + Object.keys(batch).length + '   new: ' + added + '   total now: ' + Object.keys(sorted).length);
if (collisions.length) { console.log('Redefined ' + collisions.length + ' existing keys:'); collisions.forEach((c) => console.log('  ' + c)); }
