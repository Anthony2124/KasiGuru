#!/usr/bin/env node
/**
 * Firestore reads, writes and deletes per quota day, against the Spark plan's free limits.
 *
 * Read-only: asks Cloud Monitoring for the project's `document/*_ops_count` metrics with the
 * `gcloud` account already signed in on this machine. Writes nothing and reads no documents.
 *
 * Spark quotas reset at midnight Pacific time (3 or 4 PM in the Philippines), so the days below
 * are Pacific days; today is the day in progress. When a limit is reached, every client's reads
 * (or writes) fail until the reset: Android keeps working offline, but sync, leaderboards and
 * the web app's content refresh stop.
 *
 *   node scripts/diagnostics/firestore-usage.js            last 7 days
 *   node scripts/diagnostics/firestore-usage.js --days 14
 *   node scripts/diagnostics/firestore-usage.js --hours    today, hour by hour (Philippine time)
 */
const { execSync } = require('child_process');

const PROJECT = 'kasiguru-86042';
const QUOTA_TZ = 'America/Los_Angeles';
const LOCAL_TZ = 'Asia/Manila';
const METRICS = [
  { key: 'reads', type: 'firestore.googleapis.com/document/read_ops_count', limit: 50000 },
  { key: 'writes', type: 'firestore.googleapis.com/document/write_ops_count', limit: 20000 },
  { key: 'deletes', type: 'firestore.googleapis.com/document/delete_ops_count', limit: 20000 },
];
/** Above this share of a limit a day is flagged, so there is time to act before the cut-off. */
const WARN_AT = 0.7;

const args = process.argv.slice(2);
const hourly = args.includes('--hours');
const days = Math.max(1, Math.min(42, Number(args[args.indexOf('--days') + 1]) || 7));

function token() {
  try {
    return execSync('gcloud auth print-access-token', { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    console.error('Could not get a token from gcloud. Sign in with: gcloud auth login');
    process.exit(1);
  }
}

const dayIn = (tz, date) => new Intl.DateTimeFormat('en-CA', { timeZone: tz }).format(date);
const hourIn = (tz, date) => new Intl.DateTimeFormat('en-GB', { timeZone: tz, hour: '2-digit', hour12: false }).format(date);

/** Hourly sums of one metric, as [startTime, value] pairs. */
async function hourlySeries(auth, type, start, end) {
  const params = new URLSearchParams({
    filter: `metric.type="${type}"`,
    'interval.startTime': start.toISOString(),
    'interval.endTime': end.toISOString(),
    'aggregation.alignmentPeriod': '3600s',
    'aggregation.perSeriesAligner': 'ALIGN_SUM',
    'aggregation.crossSeriesReducer': 'REDUCE_SUM',
  });
  const res = await fetch(`https://monitoring.googleapis.com/v3/projects/${PROJECT}/timeSeries?${params}`, {
    headers: { Authorization: `Bearer ${auth}` },
  });
  if (!res.ok) throw new Error(`Cloud Monitoring answered ${res.status}: ${(await res.text()).slice(0, 300)}`);
  const body = await res.json();
  return (body.timeSeries ?? []).flatMap((s) => s.points.map((p) => [new Date(p.interval.startTime), Number(p.value.int64Value ?? 0)]));
}

const pct = (n, limit) => `${Math.round((n / limit) * 100)}%`.padStart(5);
const num = (n) => n.toLocaleString('en-US').padStart(8);

async function main() {
  const auth = token();
  // Whole hours, so each point is one clock hour.
  const end = new Date(Math.ceil(Date.now() / 3600000) * 3600000);
  const start = new Date(end.getTime() - (hourly ? 30 : (days + 1) * 24) * 3600000);
  const series = {};
  for (const m of METRICS) series[m.key] = await hourlySeries(auth, m.type, start, end);

  if (hourly) {
    const today = dayIn(QUOTA_TZ, new Date());
    console.log(`Firestore today, by hour (Philippine time) - quota day ${today} Pacific\n`);
    console.log('hour      reads   writes  deletes');
    const rows = new Map();
    for (const m of METRICS)
      for (const [t, v] of series[m.key]) {
        if (dayIn(QUOTA_TZ, t) !== today) continue;
        const k = t.getTime();
        rows.set(k, { ...(rows.get(k) ?? {}), [m.key]: v });
      }
    for (const [k, r] of [...rows].sort((a, b) => a[0] - b[0]))
      console.log(`${hourIn(LOCAL_TZ, new Date(k))}:00 ${num(r.reads ?? 0)} ${num(r.writes ?? 0)} ${num(r.deletes ?? 0)}`);
    return;
  }

  const totals = new Map();
  for (const m of METRICS)
    for (const [t, v] of series[m.key]) {
      const d = dayIn(QUOTA_TZ, t);
      const row = totals.get(d) ?? { reads: 0, writes: 0, deletes: 0 };
      row[m.key] += v;
      totals.set(d, row);
    }
  const today = dayIn(QUOTA_TZ, new Date());
  const shown = [...totals.keys()].sort().slice(-days);
  console.log(`Firestore usage for ${PROJECT}, per quota day (resets midnight Pacific)\n`);
  console.log('day (Pacific)      reads           writes          deletes');
  let worst = 0;
  for (const d of shown) {
    const r = totals.get(d);
    const share = Math.max(...METRICS.map((m) => r[m.key] / m.limit));
    if (d !== today) worst = Math.max(worst, share);
    const flag = share >= 1 ? '  OVER LIMIT' : share >= WARN_AT ? '  near limit' : '';
    console.log(
      `${d}${d === today ? '*' : ' '}  ` +
        METRICS.map((m) => `${num(r[m.key])} ${pct(r[m.key], m.limit)}`).join('  ') +
        flag
    );
  }
  console.log(`\n* today, still counting. Limits: ${METRICS.map((m) => `${m.limit.toLocaleString('en-US')} ${m.key}`).join(', ')} a day.`);
  if (worst >= WARN_AT)
    console.log('A recent day passed 70% of a free limit. See docs/WEB_APP.md, "Content and the free-plan read budget".');
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
