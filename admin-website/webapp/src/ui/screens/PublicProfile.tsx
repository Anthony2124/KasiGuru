/**
 * A learner's public profile (ui/screens/profile/PublicProfileScreen), opened from the leaderboard:
 * scenery, stats, a three-badge showcase, this week against you, every tier, and path progress.
 * Only public_profiles is read; nothing personal is ever part of it.
 */
import { useEffect, useState } from 'preact/hooks';
import { BADGE_ROWS, chooseShowcase, showcaseCandidates, type BadgeRow } from '../../domain/badges';
import { isoWeekId } from '../../domain/dates';
import { weeklyXp } from '../../domain/learner';
import { SECTIONS } from '../../domain/lesson';
import type { PublicProfile } from '../../domain/publicProfile';
import { badgeRarity, cachedPublicProfile, fetchPublicProfile, weeklyRank } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { useLearner } from '../../lib/store';
import { BadgeMedal } from '../badges';
import { Avatar, EmptyState, GroundScaffold, Loading, ProgressBar, SectionHeading, sceneForSection, sceneUrl, type SceneId } from '../kit';

const monthYear = (ms: number) => new Date(ms).toLocaleDateString(undefined, { month: 'short', year: 'numeric' });

export function PublicProfileScreen({ uid }: { uid: string }) {
  const learner = useLearner();
  const [profile, setProfile] = useState<PublicProfile | null>(() => cachedPublicProfile(uid));
  const [loading, setLoading] = useState(!profile);
  const [error, setError] = useState<string | null>(null);
  const [rank, setRank] = useState<number | null>(null);
  const [showcase, setShowcase] = useState<BadgeRow[]>(() => chooseShowcase(profile?.badgeIds ?? [], {}));
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    setError(null);
    fetchPublicProfile(uid)
      .then(async (p) => {
        if (!live) return;
        setProfile(p);
        setLoading(false);
        if (!p) return;
        setShowcase(chooseShowcase(p.badgeIds, {}));
        const [rarity, r] = await Promise.all([
          badgeRarity(showcaseCandidates(p.badgeIds).map((b) => b.id)),
          p.weekId === isoWeekId() ? weeklyRank(uid).catch(() => null) : Promise.resolve(null),
        ]);
        if (!live) return;
        setShowcase(chooseShowcase(p.badgeIds, rarity));
        setRank(r);
      })
      .catch(() => {
        if (!live) return;
        setLoading(false);
        setError("Couldn't refresh this profile. You can try again when you're online.");
      });
    return () => {
      live = false;
    };
  }, [uid, attempt]);

  const report = () => navigate(`/report?category=Other&screen=${encodeURIComponent(`Public player profile: ${uid}`)}`);
  const title = profile?.displayName ?? 'Player profile';
  const actions = (
    <button class="text-btn" style={{ color: 'var(--ink)' }} onClick={report}>
      Report
    </button>
  );

  if (loading) {
    return (
      <GroundScaffold title={title} actions={actions}>
        <Loading label="Loading player profile" />
      </GroundScaffold>
    );
  }
  if (!profile) {
    return (
      <GroundScaffold title={title} actions={actions}>
        <EmptyState
          pose="curious"
          title="Profile unavailable"
          message={error ?? "This learner hasn't shared a public profile yet."}
          actionLabel="Try again"
          onAction={() => {
            setLoading(true);
            setAttempt((n) => n + 1);
          }}
        />
      </GroundScaffold>
    );
  }

  const theirs = profile.weekId === isoWeekId() ? profile.weeklyXp : 0;
  const mine = weeklyXp(learner);
  const max = Math.max(theirs, mine, 1);
  const active = profile.updatedAt > 0 ? new Date(profile.updatedAt) : null;
  const activeLine = active ? (active.toDateString() === new Date().toDateString() ? 'active today' : `active ${active.toLocaleDateString()}`) : null;
  const sections = SECTIONS.filter((s) => s.id in profile.sectionTotals);

  return (
    <GroundScaffold title={title} actions={actions}>
      <div class="stack-lg">
        <section>
          <div style={{ position: 'relative', height: 184, borderRadius: 'var(--r-panel)', overflow: 'hidden', border: '1px solid var(--hair)' }}>
            <img src={sceneUrl(profile.profileBackgroundId as SceneId)} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to bottom, transparent, rgba(0,0,0,.65))' }} />
            <p class="t-label" style={{ position: 'absolute', right: 16, bottom: 12, color: 'var(--cream)' }}>
              {rank ? `#${rank} this week` : 'Learning profile'}
            </p>
          </div>
          <div class="row" style={{ marginTop: 'var(--s-sm)' }}>
            <Avatar id={profile.profileIconId} size={88} level={profile.level} />
            <div class="grow" style={{ minWidth: 0 }}>
              <h2 class="t-headline" style={{ overflowWrap: 'anywhere' }}>{profile.displayName}</h2>
              <p class="t-title" style={{ color: 'var(--lime)' }}>Level {profile.level}</p>
              <p class="t-body-s muted">{[profile.createdAt > 0 ? `Joined ${monthYear(profile.createdAt)}` : null, activeLine].filter(Boolean).join(' · ')}</p>
            </div>
          </div>
        </section>

        {error && (
          <button class="text-btn" style={{ color: 'var(--muted)' }} onClick={() => setAttempt((n) => n + 1)}>
            {error}
          </button>
        )}

        <div class="grid-2">
          {(
            [
              ['day streak', profile.currentStreak],
              ['total XP', profile.totalXp],
              ['words learned', profile.wordsLearned],
              ['lessons done', profile.lessonsCompleted],
            ] as const
          ).map(([label, value]) => (
            <div key={label} class="card">
              <p class="t-headline">{value}</p>
              <p class="t-label-s muted">{label}</p>
            </div>
          ))}
        </div>

        {showcase.length > 0 && (
          <section class="stack-sm">
            <SectionHeading text="Showcase" />
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-sm)' }}>
              {showcase.map((b) => (
                <div key={b.id} class="center" style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
                  <BadgeMedal tier={b.tier} earned size={56} />
                  <p class="t-label">{b.family.name}</p>
                  <p class="t-label-s muted">{b.tier.label}</p>
                </div>
              ))}
            </div>
          </section>
        )}

        <section class="stack-sm">
          <SectionHeading text={`You vs ${profile.displayName} · this week`} />
          {[
            [profile.displayName, theirs],
            ['You', mine],
          ].map(([name, xp]) => (
            <div key={String(name)} class="stack-sm">
              <p class="t-body">
                {name} · {xp} XP
              </p>
              <ProgressBar value={Number(xp) / max} height={8} />
            </div>
          ))}
        </section>

        <section class="stack-sm">
          <SectionHeading text={`Badges · ${profile.badgeIds.length} of 66 tiers`} />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-sm)' }}>
            {BADGE_ROWS.map((b) => (
              <div key={b.id} class="center" style={{ display: 'grid', justifyItems: 'center', gap: 2 }}>
                <BadgeMedal tier={b.tier} earned={profile.badgeIds.includes(b.id)} size={44} />
                <p class="t-label-s">{b.family.name}</p>
                <p class="t-label-s muted">{b.tier.label}</p>
              </div>
            ))}
          </div>
        </section>

        <section class="stack-sm">
          <SectionHeading text="Path progress" />
          <div class="list">
            {sections.map((s) => (
              <div key={s.id} class="list-row">
                <img src={sceneUrl(sceneForSection(s.id))} alt="" style={{ width: 56, height: 56, borderRadius: 'var(--r-tile)', objectFit: 'cover', flex: 'none' }} />
                <div class="grow">
                  <p class="t-title-s">{s.title}</p>
                  <p class="t-body-s muted">
                    {profile.masteredSections.includes(s.id)
                      ? 'Mastered'
                      : !profile.unlockedSections.includes(s.id)
                        ? 'Locked'
                        : `${profile.sections[s.id] ?? 0} / ${profile.sectionTotals[s.id] ?? 0} lessons`}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <p class="t-body-s muted">Only your display name and game stats are public. Personal details stay private.</p>
      </div>
    </GroundScaffold>
  );
}
