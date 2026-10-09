import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { clipPlan, type BundledClip } from '../src/domain/audioClips';

const take: BundledClip = { v: 1790749180406, file: 'dalaga__young_woman.1790749180406.m4a', bytes: 20000 };

describe('clipPlan', () => {
  it('plays the site copy when it is the take the word points at', () => {
    expect(clipPlan(take, take.v)).toEqual({ site: take, fallback: null });
  });

  it('fetches a re-recording, keeping the earlier site copy as the fallback', () => {
    expect(clipPlan(take, take.v + 1)).toEqual({ site: null, fallback: take });
  });

  it('fetches a clip the site does not serve, with nothing to fall back on', () => {
    expect(clipPlan(undefined, take.v)).toEqual({ site: null, fallback: null });
  });
});

describe('content/audio.json', () => {
  const pub = path.resolve(__dirname, '..', 'public');
  const manifest = path.join(pub, 'content', 'audio.json');

  it.runIf(fs.existsSync(manifest))('lists only files the site serves', () => {
    const { clips } = JSON.parse(fs.readFileSync(manifest, 'utf8')) as { clips: Record<string, BundledClip> };
    const missing = Object.values(clips).filter((c) => !fs.existsSync(path.join(pub, 'audio', c.file)));
    expect(missing).toEqual([]);
  });
});
