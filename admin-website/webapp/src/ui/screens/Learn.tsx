/**
 * Learn: the learning path, section by section (ui/screens/learn and learn/tree/LearningPath.kt).
 * Each section opens on its Casiguran place; Jepjep waits beside the node that is the learner's to
 * take next, and the tab opens scrolled to it.
 */
import { useEffect, useMemo, useRef } from 'preact/hooks';
import {
  buildTree,
  deepDiveCount,
  Mastery,
  masteryUnitId,
  nodeKey,
  openSectionKeys,
  sectionGateFraction,
  sectionIsComplete,
  type TreeNodeState,
  type TreeSection,
} from '../../domain/lesson';
import { navigate, enc } from '../../lib/router';
import { act, useCorpus, useLearner } from '../../lib/store';
import { treeAccess } from '../../domain/learner';
import { EmptyState, GroundScaffold, Icon, Jepjep, Scene, sceneForSection } from '../kit';

const WIND = [0, 0.55, 0.85, 0.55, 0, -0.55, -0.85, -0.55];
const AMPLITUDE = 64;
const NODE = 60;
const CURRENT = 72;
const RING_INSET = 10;

type Look = 'locked' | 'current' | 'done' | 'open' | 'test' | 'testPassed';

function lookOf(n: TreeNodeState): Look {
  const isTest = n.node.kind === 'mastery';
  if (!n.isUnlocked) return 'locked';
  if (isTest && n.mastery >= Mastery.FAMILIAR) return 'testPassed';
  if (isTest) return 'test';
  if (n.isCurrent) return 'current';
  if (n.mastery >= Mastery.FAMILIAR) return 'done';
  return 'open';
}

const FACE: Record<Look, [string, string, string]> = {
  current: ['var(--lime)', 'var(--lime-lip)', 'var(--on-lime)'],
  done: ['var(--olive)', 'var(--olive-deep)', 'var(--cream)'],
  open: ['var(--sunken)', 'var(--hair)', 'var(--ink)'],
  test: ['var(--gold)', 'var(--gold-deep)', 'var(--reward-ink)'],
  testPassed: ['var(--gold)', 'var(--gold-deep)', 'var(--reward-ink)'],
  locked: ['var(--surface)', 'var(--surface)', 'var(--faint)'],
};

function spoken(n: TreeNodeState): string {
  const what = n.node.kind === 'lesson' ? `Lesson ${n.node.positionInSection}` : 'Section mastery test';
  if (!n.isUnlocked) return n.node.kind === 'mastery' ? `${what}, locked. Finish the lessons above to unlock` : `${what}, locked`;
  const tier = ['not started', 'familiar', 'practising', 'mastered'][n.mastery];
  return n.isCurrent ? `${what}, ${tier}, start here` : `${what}, ${tier}`;
}

function MasteryRing({ mastery, size }: { mastery: Mastery; size: number }) {
  if (mastery === Mastery.NONE) return null;
  const color = mastery === Mastery.MASTERED ? 'var(--gold)' : 'var(--brand-lime)';
  const stroke = 4;
  const r = (size - stroke) / 2;
  const c = size / 2;
  const arc = (i: number) => {
    const sweep = 100;
    const start = -90 + i * 120 + (120 - sweep) / 2;
    const rad = (d: number) => (d * Math.PI) / 180;
    const x1 = c + r * Math.cos(rad(start));
    const y1 = c + r * Math.sin(rad(start));
    const x2 = c + r * Math.cos(rad(start + sweep));
    const y2 = c + r * Math.sin(rad(start + sweep));
    return `M ${x1} ${y1} A ${r} ${r} 0 0 1 ${x2} ${y2}`;
  };
  return (
    <svg width={size} height={size} style={{ position: 'absolute', inset: 0 }} aria-hidden="true">
      {[0, 1, 2].map((i) => (
        <path key={i} d={arc(i)} fill="none" stroke={color} stroke-opacity={i < mastery ? 1 : 0.22} stroke-width={stroke} stroke-linecap="round" />
      ))}
    </svg>
  );
}

function PathNode({ node, index, showGuide, previous }: { node: TreeNodeState; index: number; showGuide: boolean; previous?: TreeNodeState }) {
  const lean = WIND[index % WIND.length];
  const look = lookOf(node);
  const size = node.isCurrent ? CURRENT : NODE;
  const ring = size + RING_INSET * 2;
  const offset = AMPLITUDE * lean;
  const [face, lip, ink] = FACE[look];
  const open = () => {
    if (!node.isUnlocked) return;
    if (node.node.kind === 'lesson') navigate(`/lesson/${enc(node.node.ref.unitId)}/${node.node.ref.lessonIndex}`);
    else navigate(`/lesson/${enc(masteryUnitId(node.node.sectionId))}/0`);
  };
  const caption =
    look === 'current' ? 'Start' : look === 'test' && node.isCurrent ? 'Take the test' : look === 'locked' && node.node.kind === 'mastery' ? 'Finish the lessons above to unlock' : null;

  return (
    <div style={{ display: 'grid', justifyItems: 'center' }} data-current={node.isCurrent && node.isUnlocked ? 'true' : undefined}>
      {previous && (
        <svg width={320} height={26} aria-hidden="true" style={{ display: "block", overflow: "visible" }} viewBox="-160 0 320 26">
          <line
            x1={WIND[(index - 1) % WIND.length] * AMPLITUDE}
            y1={0}
            x2={lean * AMPLITUDE}
            y2={26}
            stroke={previous.mastery >= Mastery.FAMILIAR ? 'var(--brand-lime)' : 'var(--faint)'}
            stroke-opacity={previous.mastery >= Mastery.FAMILIAR ? 0.7 : 0.45}
            stroke-width={3}
            stroke-linecap="round"
            stroke-dasharray={previous.mastery >= Mastery.FAMILIAR ? undefined : '4 6'}
          />
        </svg>
      )}
      <div style={{ position: 'relative', transform: `translateX(${offset}px)`, width: ring, height: ring + 5 }}>
        {node.isCurrent && node.isUnlocked && <span class="halo" style={{ width: ring, height: ring }} />}
        <MasteryRing mastery={node.mastery} size={ring} />
        <button
          class="path-node"
          disabled={!node.isUnlocked}
          aria-label={spoken(node)}
          onClick={open}
          style={{
            position: 'absolute',
            left: RING_INSET,
            top: RING_INSET,
            width: size,
            height: size,
            background: look === 'locked' ? 'var(--surface)' : `linear-gradient(to bottom, rgba(255,255,255,.22), rgba(255,255,255,0) 45%), ${face}`,
            color: ink,
            border: look === 'locked' ? '1px solid var(--hair)' : 0,
            boxShadow: look === 'locked' ? 'none' : `0 5px 0 ${lip}, 0 8px 16px rgba(0,0,0,.45)`,
          }}
        >
          {look === 'locked' ? (
            <Icon name="lock" size={20} />
          ) : node.node.kind === 'mastery' ? (
            <Icon name="medalStar" size={26} />
          ) : look === 'done' ? (
            <Icon name="tickCircle" size={26} />
          ) : (
            <span class="t-title-l">{node.node.positionInSection}</span>
          )}
        </button>
        {showGuide && (
          <div style={{ position: 'absolute', top: ring / 2 - 40, width: 80, display: 'flex', justifyContent: 'center', [lean >= 0 ? 'right' : 'left']: ring + 4, pointerEvents: 'none' } as never}>
            <Jepjep pose="with_backpack" height={80} decorative />
          </div>
        )}
      </div>
      {caption && (
        <p class="t-label" aria-hidden="true" style={{ marginTop: 4, transform: `translateX(${offset}px)`, color: look === 'locked' ? 'var(--faint)' : 'var(--ink)', textAlign: 'center', maxWidth: 220 }}>
          {caption}
        </p>
      )}
    </div>
  );
}

function SectionBlock({ section, previous, guideKey, first }: { section: TreeSection; previous?: TreeSection; guideKey: string | null; first: boolean }) {
  const locked = !section.isUnlocked;
  const complete = sectionIsComplete(section);
  const remaining = Math.max(0, section.requiredXp - section.earnedXp);
  const opener = previous?.definition.title ?? 'the section before this one';
  return (
    <section style={{ marginTop: first ? 'var(--s-xs)' : 'var(--s-xl)' }} aria-label={section.definition.title}>
      <Scene id={sceneForSection(section.definition.id)} locked={locked} height={136}>
        <div style={{ position: 'relative', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 'var(--s-sm) var(--s-md)' }}>
          {(locked || complete) && (
            <span class="chip" style={{ position: 'absolute', top: 12, right: 12, minHeight: 28, background: 'rgba(10,14,13,.78)', border: 0 }}>
              <Icon name={locked ? 'lock' : 'tickCircle'} size={14} color={locked ? 'var(--muted)' : 'var(--brand-lime)'} />
              {locked ? 'Locked' : 'Complete'}
            </span>
          )}
          <h2 class="t-headline-s" style={{ color: locked ? 'var(--muted)' : 'var(--ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{section.definition.title}</h2>
          <p class="t-body muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{section.definition.gloss}</p>
        </div>
      </Scene>
      <div style={{ marginTop: 'var(--s-sm)' }}>
        <p class="t-body muted">{section.definition.journeyLine}</p>
        {section.isUnlocked ? (
          <div class="row" style={{ marginTop: 'var(--s-xs)' }}>
            <div class="bar-track grow" style={{ height: 6 }}>
              <div class="bar-fill" style={{ width: `${sectionGateFraction(section) * 100}%` }} />
            </div>
            <span class="t-label muted">{section.earnedXp} / {section.requiredXp} XP</span>
          </div>
        ) : (
          <p class="row-xs t-body muted" style={{ marginTop: 'var(--s-xs)' }}>
            <Icon name="lock" size={16} />
            {remaining > 0 ? `Earn ${remaining} more XP in ${opener} to open this` : `Finish ${opener} to open this`}
          </p>
        )}
      </div>
      {section.isUnlocked && (
        <div style={{ marginTop: 'var(--s-md)' }}>
          {section.nodes.map((n, i) => (
            <div key={nodeKey(n)}>
              {n.isDeepDive && !section.nodes[i - 1]?.isDeepDive && (
                <div style={{ padding: 'var(--s-lg) 0 var(--s-sm)' }}>
                  <h3 class="t-title">Going deeper</h3>
                  <p class="t-body muted" style={{ marginTop: 4 }}>
                    {deepDiveCount(section) === 1
                      ? 'One more lesson in this stage, whenever you want it. The next stage is already open.'
                      : `${deepDiveCount(section)} more lessons in this stage, whenever you want them. The next stage is already open.`}
                  </p>
                </div>
              )}
              <PathNode node={n} index={i} showGuide={nodeKey(n) === guideKey} previous={i > 0 ? section.nodes[i - 1] : undefined} />
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export function LearnScreen() {
  const corpus = useCorpus();
  const learner = useLearner();
  const access = treeAccess(learner);
  const tree = useMemo(() => buildTree(corpus, learner.lessons, access), [corpus, learner.lessons, access]);
  const arrived = useRef(false);

  // LessonRepository.treeSections: a section seen open is saved as an access receipt, so it stays
  // open (here and on Android) even if the XP gate it passed is later recalculated.
  useEffect(() => {
    const missing = openSectionKeys(tree).filter((k) => !access.savedAccess.has(k));
    if (missing.length) act((d) => d.preserveAccess(missing), { celebrate: false });
  }, [tree]);

  const guide = tree.flatMap((s) => s.nodes).find((n) => n.isCurrent && n.isUnlocked);
  const guideKey = guide ? nodeKey(guide) : null;
  const currentSection = tree.find((s) => s.isUnlocked && s.nodes.some((n) => n.isCurrent));
  const subtitle = currentSection
    ? `You are in ${currentSection.definition.title}`
    : tree.length && tree.every(sectionIsComplete)
      ? 'Every section walked. Keep them sharp with review.'
      : 'Section by section, from greetings to everyday talk';

  useEffect(() => {
    if (arrived.current || !tree.length) return;
    arrived.current = true;
    // Only when the next node is below the fold: early on it sits right under the first banner,
    // and scrolling would hide the section it belongs to.
    const el = document.querySelector('[data-current="true"]') as HTMLElement | null;
    if (el) {
      const top = el.getBoundingClientRect().top;
      if (top > window.innerHeight * 0.6) window.scrollTo(0, Math.max(0, top + window.scrollY - window.innerHeight * 0.4));
    }
  }, [tree.length]);

  return (
    <GroundScaffold title="Learn" onBack={false} nav>
      {!tree.length ? (
        <EmptyState pose="sleeping" title="Nothing on the path yet" message="The lessons appear once the dictionary has finished loading." />
      ) : (
        <div style={{ maxWidth: 520, margin: '0 auto' }}>
          <p class="t-body muted" style={{ marginBottom: 'var(--s-xs)' }}>{subtitle}</p>
          {tree.map((s, i) => (
            <SectionBlock key={s.definition.id} section={s} previous={tree[i - 1]} guideKey={guideKey} first={i === 0} />
          ))}
        </div>
      )}
    </GroundScaffold>
  );
}
