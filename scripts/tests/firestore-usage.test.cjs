const { test } = require('node:test');
const assert = require('node:assert/strict');
const { hourlySeries, dayIn, quotaDayLabels } = require('../diagnostics/firestore-usage');

test('quota dates follow Pacific midnight, including the daylight-saving change', () => {
  assert.equal(dayIn('America/Los_Angeles', new Date('2026-10-07T06:59:00Z')), '2026-10-06');
  assert.equal(dayIn('America/Los_Angeles', new Date('2026-10-07T07:00:00Z')), '2026-10-07');
  assert.equal(dayIn('America/Los_Angeles', new Date('2026-11-02T07:59:00Z')), '2026-11-01');
  assert.equal(dayIn('America/Los_Angeles', new Date('2026-11-02T08:00:00Z')), '2026-11-02');
});

test('the report includes zero-usage calendar days across a DST boundary', () => {
  assert.deepEqual(quotaDayLabels(new Date('2026-11-02T08:01:00Z'), 3), ['2026-10-31', '2026-11-01', '2026-11-02']);
});

test('pagination does not silently lose reads from later Monitoring pages', async () => {
  let calls = 0;
  const fetcher = async (url) => {
    const params = new URL(url).searchParams;
    assert.equal(params.get('aggregation.perSeriesAligner'), 'ALIGN_SUM');
    assert.equal(params.get('aggregation.crossSeriesReducer'), 'REDUCE_SUM');
    assert.equal(params.get('pageToken'), calls === 0 ? null : 'next');
    calls++;
    return { ok: true, json: async () => ({
      timeSeries: [{ points: [{ interval: { startTime: '2026-10-07T07:00:00Z' }, value: { int64Value: String(calls * 100) } }] }],
      ...(calls === 1 ? { nextPageToken: 'next' } : {}),
    }) };
  };
  const points = await hourlySeries('fixture', 'reads', new Date('2026-10-07T07:00:00Z'), new Date('2026-10-07T09:00:00Z'), fetcher);
  assert.equal(calls, 2);
  assert.equal(points.reduce((sum, [, value]) => sum + value, 0), 300);
});
