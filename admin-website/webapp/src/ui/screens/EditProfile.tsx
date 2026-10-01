/**
 * Edit profile: avatar, name, age and address. The public leaderboard shows the full name (or the
 * onboarding name when there is none), so it is capped at the rules' 100 characters.
 */
import { useState } from 'preact/hooks';
import { back } from '../../lib/router';
import { act, useApp } from '../../lib/store';
import { AVATARS, Avatar, ClayButton, GroundScaffold, toast } from '../kit';

export function EditProfileScreen() {
  const p = useApp((s) => s.learner.progress);
  const [avatar, setAvatar] = useState(p.profileIconId);
  const [name, setName] = useState(p.fullName || p.userName);
  const [age, setAge] = useState(p.age != null ? String(p.age) : '');
  const [address, setAddress] = useState(p.address);
  const [error, setError] = useState<string | null>(null);

  const save = (e: Event) => {
    e.preventDefault();
    const n = name.trim();
    const a = age.trim() ? Number(age) : null;
    if (!n) return setError('Enter your name.');
    if (n.length > 100) return setError('Your name is too long.');
    if (a != null && (!Number.isInteger(a) || a < 3 || a > 120)) return setError('Enter an age between 3 and 120.');
    if (address.trim().length > 200) return setError('Your address is too long.');
    act((d) => {
      d.d.progress.fullName = n;
      d.d.progress.age = a;
      d.d.progress.address = address.trim();
      d.d.progress.profileIconId = avatar;
      d.d.progress.updatedAt = Date.now();
    });
    toast('Profile saved.');
    back('/me');
  };

  return (
    <GroundScaffold title="Edit profile">
      <form class="readable stack-lg" onSubmit={save}>
        <section class="stack-sm">
          <h2 class="t-title">Your avatar</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-md)', justifyItems: 'center' }} role="radiogroup">
            {AVATARS.map((a) => (
              <div key={a.id} style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
                <Avatar id={a.id} size={84} selected={avatar === a.id} onClick={() => setAvatar(a.id)} label={a.name} />
                <span class="t-label-s" style={{ color: avatar === a.id ? 'var(--lime)' : 'var(--muted)' }}>{a.name}</span>
              </div>
            ))}
          </div>
        </section>
        <section class="stack">
          <label class="field">
            <span>Full name</span>
            <input class="input" value={name} maxLength={100} autoComplete="name" onInput={(e) => setName((e.target as HTMLInputElement).value)} />
          </label>
          <label class="field">
            <span>Age</span>
            <input class="input" value={age} inputMode="numeric" maxLength={3} onInput={(e) => setAge((e.target as HTMLInputElement).value.replace(/\D/g, ''))} />
          </label>
          <label class="field">
            <span>Address</span>
            <input class="input" value={address} maxLength={200} placeholder="e.g. Barangay, Casiguran, Aurora" onInput={(e) => setAddress((e.target as HTMLInputElement).value)} />
          </label>
        </section>
        {error && <p class="t-body" style={{ color: 'var(--red)' }} role="alert">{error}</p>}
        <ClayButton label="Save changes" type="submit" />
      </form>
    </GroundScaffold>
  );
}
