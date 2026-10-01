/**
 * Account suspended (ui/screens/banned): shown when an admin has banned this account. The learner
 * can read the reason, appeal, or sign out to continue as a guest.
 */
import { useState } from 'preact/hooks';
import { signOutToGuest, submitAppeal } from '../../lib/auth';
import { useApp } from '../../lib/store';
import { ClayButton, Icon } from '../kit';

export function SuspendedScreen() {
  const ban = useApp((s) => s.ban)!;
  const [text, setText] = useState('');
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const date = (ms: number) => (ms ? new Date(ms).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) : '');
  const pending = ban.appealStatus === 'pending' && !editing;
  const declined = ban.appealStatus === 'rejected' || ban.appealStatus === 'declined';

  return (
    <main class="page no-nav" style={{ paddingTop: 'calc(var(--safe-top) + var(--s-xl))' }}>
      <div class="readable stack">
        <div style={{ display: 'grid', placeItems: 'center' }}>
          <div style={{ width: 88, height: 88, borderRadius: '50%', background: 'var(--red-tint)', display: 'grid', placeItems: 'center' }}>
            <Icon name="lock" size={44} color="var(--red)" label="Account suspended" />
          </div>
        </div>
        <h1 class="t-headline center">Account suspended</h1>
        <p class="t-body-l muted center">Your KasiGuru account has been suspended by an administrator.</p>
        <div class="card stack-sm">
          <p class="t-label muted">Reason for suspension</p>
          <p class="t-body-l">{ban.reason}</p>
          {ban.bannedAt > 0 && <p class="t-body-s faint">Suspended on {date(ban.bannedAt)}</p>}
        </div>
        {pending ? (
          <div class="card stack-sm">
            <p class="t-title-s">Appeal under review</p>
            <p class="t-body muted">Our moderation team is reviewing your appeal. Your account will automatically reactivate if approved.</p>
            <button class="text-btn lime" style={{ alignSelf: 'flex-start', padding: 0 }} onClick={() => { setText(ban.appealText ?? ''); setEditing(true); }}>Update appeal</button>
          </div>
        ) : (
          <div class="card stack-sm">
            {declined && (
              <>
                <p class="t-title-s" style={{ color: 'var(--red)' }}>Appeal declined</p>
                {ban.appealReviewNotes && <p class="t-body"><b>Moderator feedback:</b> {ban.appealReviewNotes}</p>}
                <p class="t-body muted">If you have additional clarification or new context, you may submit a revised appeal.</p>
              </>
            )}
            {!declined && <p class="t-title-s">Believe this is a mistake?</p>}
            <textarea class="input" placeholder="Explain why the suspension should be lifted (at least 5 characters)" value={text} maxLength={2000} onInput={(e) => setText((e.target as HTMLTextAreaElement).value)} />
            {error && <p class="t-body" style={{ color: 'var(--red)' }}>{error}</p>}
            <ClayButton
              label={busy ? 'Sending…' : declined ? 'Submit new appeal' : 'Submit appeal'}
              disabled={busy || text.trim().length < 5}
              onClick={async () => {
                setBusy(true);
                setError(null);
                try {
                  await submitAppeal(text);
                  setEditing(false);
                } catch {
                  setError('Could not send the appeal. Check your connection and try again.');
                } finally {
                  setBusy(false);
                }
              }}
            />
          </div>
        )}
        <ClayButton label="Sign out" tone="quiet" onClick={() => void signOutToGuest()} />
      </div>
    </main>
  );
}
