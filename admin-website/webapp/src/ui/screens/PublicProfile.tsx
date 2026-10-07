/**
 * A learner's public profile (ui/screens/profile/PublicProfileScreen), opened from the leaderboard:
 * scenery, stats, a three-badge showcase, this week against you, every tier, and path progress.
 * Only public_profiles is read; nothing personal is ever part of it. On the web the 66 tiers are
 * grouped into one row per badge, so the page does not become a wall of small medals.
 */
import { useEffect, useState } from 'preact/hooks';
import { BADGE_FAMILIES, BADGE_TIERS, badgeId, chooseShowcase, showcaseCandidates, type BadgeRow } from '../../domain/badges';
import { isoWeekId } from '../../domain/dates';
import { weeklyXp } from '../../domain/learner';
import { SECTIONS } from '../../domain/lesson';
import { plural } from '../../domain/plural';
import type { PublicProfile } from '../../domain/publicProfile';
import { badgeRarity, cachedPublicProfile, fetchPublicProfile, weeklyRank } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { useLearner } from '../../lib/store';
import { BadgeMedal } from '../badges';
import type { IconName } from '../icons.generated';
import { Avatar, EmptyState, GroundScaffold, Icon, Loading, ProgressBar, SectionHeading, sceneForSection, sceneUrl, type SceneId } from '../kit';

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

  const stats: { icon: IconName; tint: string; value: number; label: string }[] = [
    { icon: 'flash', tint: 'var(--coral)', value: profile.currentStreak, label: 'day streak' },
    { icon: 'star', tint: 'var(--gold)', value: profile.totalXp, label: 'total XP' },
    { icon: 'book', tint: 'var(--lime)', value: profile.wordsLearned, label: 'words learned' },
    { icon: 'teacher', tint: 'var(--info)', value: profile.lessonsCompleted, label: 'lessons done' },
  ];

  return (
    <GroundScaffold title={title} actions={actions} wide>
      <div class="cols">
        <div class="stack-lg">
        <section>
          <div class="profile-banner">
            <img src={sceneUrl(profile.profileBackgroundId as SceneId)} alt="" />
            <span class="profile-rank row-xs t-label">
              <Icon name="cup" size={16} color="var(--gold)" />
              {rank ? `#${rank} this week` : 'Learning profile'}
            </span>
          </div>
          <div class="profile-id">
            <Avatar id={profile.profileIconId} size={96} level={profile.level} />
            <div class="grow">
              <h2 class="t-headline">{profile.displayName}</h2>
              <p class="t-title" style={{ color: 'var(--lime)' }}>Level {profile.level}</p>
              <p class="t-body muted">{[profile.createdAt > 0 ? `Joined ${monthYear(profile.createdAt)}` : null, activeLine].filter(Boolean).join(' · ')}</p>
            </div>
          </div>
        </section>

        {error && (
          <button class="text-btn" style={{ color: 'var(--muted)' }} onClick={() => setAttempt((n) => n + 1)}>
            {error}
          </button>
        )}

        <div class="stat-grid">
          {stats.map((s) => (
            <div key={s.label} class="card stat-tile">
              <span class="ico" style={{ color: s.tint }}>
                <Icon name={s.icon} size={24} color={s.tint} />
              </span>
              <div class="grow">
                <p class="t-headline-s">{s.value.toLocaleString()}</p>
                <p class="t-label muted">{s.label}</p>
              </div>
            </div>
          ))}
        </div>

        {showcase.length > 0 && (
          <section class="stack-sm">
            <SectionHeading text="Showcase" />
            <div class="card showcase">
              {showcase.map((b) => (
                <div key={b.id}>
                  <BadgeMedal tier={b.tier} earned size={96} family={b.family.id} />
                  <p class="t-title-s">{b.family.name}</p>
                  <p class="t-body muted">{b.tier.label}</p>
                </div>
              ))}
            </div>
          </section>
        )}

        <section class="stack-sm">
          <SectionHeading text={`You vs ${profile.displayName} · this week`} />
          <div class="card stack">
            {[
              { name: profile.displayName, xp: theirs, color: 'var(--gold)' },
              { name: 'You', xp: mine, color: 'var(--lime)' },
            ].map((r) => (
              <div key={r.name} class="stack-sm">
                <div class="row">
                  <p class="t-title-s grow">{r.name}</p>
                  <p class="t-title-s" style={{ color: r.color }}>{r.xp.toLocaleString()} XP</p>
                </div>
                <ProgressBar value={r.xp / max} color={r.color} height={10} label={`${r.name}, ${r.xp} XP this week`} />
              </div>
            ))}
          </div>
        </section>

        </div>

        <div class="stack-lg">
        <section class="stack-sm">
          <SectionHeading text={`Badges · ${profile.badgeIds.length} of 66 tiers`} />
          {/* One medal per family at its highest earned tier, three to a row, as PublicProfileScreen. */}
          <div class="badge-grid" style={{ paddingTop: 'var(--s-xs)' }}>
            {BADGE_FAMILIES.map((f) => {
              const tiers = BADGE_TIERS.map((t) => profile.badgeIds.includes(badgeId(f.id, t.index)));
              const count = tiers.filter(Boolean).length;
              const top = BADGE_TIERS[tiers.lastIndexOf(true)];
              return (
                <div key={f.id} class="badge-cell" role="group" aria-label={`${f.name}: ${top ? top.label : 'locked'}, ${count} of 6 tiers`}>
                  <BadgeMedal tier={top} earned={!!top} size={84} family={f.id} />
                  <span class={`t-label-s name${top ? '' : ' muted'}`}>{f.name}</span>
                  <span class="t-label-s" style={{ color: top ? 'var(--lime)' : 'var(--faint)' }}>{top ? `${top.label} · ${count} of 6` : 'Locked'}</span>
                </div>
              );
            })}
          </div>
        </section>

        <section class="stack-sm">
          <SectionHeading text="Path progress" />
          <div class="list">
            {sections.map((s) => {
              const mastered = profile.masteredSections.includes(s.id);
              const unlocked = profile.unlockedSections.includes(s.id);
              const done = profile.sections[s.id] ?? 0;
              const total = profile.sectionTotals[s.id] ?? 0;
              return (
                <div key={s.id} class="list-row">
                  <img class="section-thumb" src={sceneUrl(sceneForSection(s.id))} alt="" style={unlocked ? undefined : { filter: 'grayscale(1)', opacity: 0.5 }} />
                  <div class="grow stack-sm">
                    <p class="t-title">{s.title}</p>
                    {mastered ? (
                      <p class="row-xs t-label" style={{ color: 'var(--lime)' }}>
                        <Icon name="tickCircle" size={16} color="var(--lime)" /> Mastered
                      </p>
                    ) : !unlocked ? (
                      <p class="row-xs t-body faint">
                        <Icon name="lock" size={16} color="var(--faint)" /> Locked
                      </p>
                    ) : (
                      <>
                        <p class="t-body muted">{done} / {plural(total, 'lesson')}</p>
                        <ProgressBar value={total ? done / total : 0} height={6} label={`${s.title}, ${done} of ${total} lessons`} />
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <p class="t-body muted">Only your display name and game stats are public. Personal details stay private.</p>
        </div>
      </div>
    </GroundScaffold>
  );
}
