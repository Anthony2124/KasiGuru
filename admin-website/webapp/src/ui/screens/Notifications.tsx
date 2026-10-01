/**
 * The notification inbox: live announcements from the admin portal (the `announcements` collection),
 * the same free-plan channel the Android app reads while it is open.
 */
import { useEffect } from 'preact/hooks';
import { fetchAnnouncements } from '../../lib/remote';
import { setPrefs, useApp } from '../../lib/store';
import { EmptyState, GroundScaffold, Icon } from '../kit';

export function NotificationsScreen() {
  const list = useApp((s) => s.announcements);
  const read = useApp((s) => s.prefs.readAnnouncements);
  useEffect(() => {
    void fetchAnnouncements();
  }, []);
  useEffect(() => {
    if (list.some((a) => !read.includes(a.id))) setPrefs({ readAnnouncements: [...new Set([...read, ...list.map((a) => a.id)])].slice(-50) });
  }, [list]);
  return (
    <GroundScaffold title="Notifications" largeTitle>
      {list.length === 0 ? (
        <EmptyState pose="sleeping" title="Nothing new" message="Announcements from the KasiGuru team will appear here." />
      ) : (
        <div class="stack-sm">
          {list.map((a) => (
            <div key={a.id} class="card row" style={{ alignItems: 'flex-start' }}>
              <span class="ico" style={{ width: 40, height: 40, borderRadius: '50%', background: 'rgba(79,179,232,.14)', display: 'grid', placeItems: 'center', flex: 'none' }}>
                <Icon name="notification" size={20} color="var(--info)" />
              </span>
              <div class="grow">
                <p class="t-title-s">{a.title}</p>
                <p class="t-body muted" style={{ whiteSpace: 'pre-line' }}>{a.message}</p>
                {a.createdAt > 0 && <p class="t-body-s faint" style={{ marginTop: 4 }}>{new Date(a.createdAt).toLocaleDateString()}</p>}
              </div>
            </div>
          ))}
        </div>
      )}
    </GroundScaffold>
  );
}
