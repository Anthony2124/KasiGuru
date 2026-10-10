// roles.js — who may use the portal, and as what.
//
// Two roles. An admin carries the `admin` custom claim (functions/set_admin_claim.js). A verifier is
// anyone whose verified email an admin listed on the Team page (admin_staff/{lower-case email}).
// Verifiers moderate everything except APK releases and blocking users (they flag a user for an
// admin instead) and cannot back up or restore; firestore.rules enforces the
// same split server-side, so hiding a control here is convenience, not security.
import { db, doc, getDocFromServer, updateDoc, runTransaction, writeBatch } from './firebase-config.js';

// How stale a verifier's "last signed in" may get before it is stamped again: once an hour keeps the
// Team page truthful without a write on every page load.
const SEEN_EVERY_MS = 60 * 60 * 1000;

// A new verifier also needs a one-time code, sent by the admin in a second email after the link.
// firestore.rules holds the same limits: five tries per code, and the code's own expiry.
export const CODE_TRIES = 5;
export const CODE_LIFETIME_MS = 48 * 60 * 60 * 1000;

export const ROLE_LABEL = { admin: 'Admin', verifier: 'Verifier' };

/**
 * 'admin', 'verifier', 'invited' for a verifier who has not entered their one-time code yet, or
 * null for an account with no portal access.
 */
export async function resolveRole(user, { forceRefresh = false } = {}) {
  if (!user) return null;
  let timer;
  try {
    return await Promise.race([
      (async () => {
        const { claims } = await user.getIdTokenResult(forceRefresh);
        if (claims.admin === true) return 'admin';
        // Use the token's identity, as the rules do, including after a forced refresh.
        const email = (claims.email || '').trim().toLowerCase();
        if (!email || claims.email_verified !== true) return null;
        const ref = doc(db, 'admin_staff', email);
        // A cached invitation could have been removed. Access must be checked with the server.
        const snap = await getDocFromServer(ref);
        if (!snap.exists() || snap.data().role !== 'verifier') return null;
        if (snap.data().pending === true) return 'invited';
        if (Date.now() - (Number(snap.data().lastSeenAt) || 0) > SEEN_EVERY_MS) {
          updateDoc(ref, { lastSeenAt: Date.now() }).catch(() => {});
        }
        return 'verifier';
      })(),
      new Promise((_, reject) => {
        timer = setTimeout(() => reject(Object.assign(new Error('Portal access check timed out'), {
          code: 'portal/access-check-timeout'
        })), 10000);
      })
    ]);
  } finally {
    clearTimeout(timer);
  }
}

/** A failed check is different from an account that was checked and has no access. */
export function accessCheckMessage(error) {
  return error?.code === 'permission-denied'
    ? "Couldn't check this account's access. Ask the project owner to check the console's database rules, then try again."
    : "Couldn't check this account's access. Check your connection and try again.";
}

/** Create once, even if the Team list is still loading or another admin adds the same email. */
export async function addVerifier(email, addedBy) {
  email = email.trim().toLowerCase();
  if (email.length > 254 || !/^[^\s@/]+@[^\s@/]+\.[^\s@/]+$/.test(email)) {
    throw Object.assign(new Error('Enter a full email address, like name@gmail.com.'), { code: 'staff/invalid-email' });
  }
  if (email === (addedBy || '').trim().toLowerCase()) {
    throw Object.assign(new Error("That's you; admins already have full access."), { code: 'staff/own-email' });
  }
  const ref = doc(db, 'admin_staff', email);
  const invite = newInvite();
  await runTransaction(db, async (transaction) => {
    if ((await transaction.get(ref)).exists()) {
      throw Object.assign(new Error(`${email} is already on the team.`), { code: 'staff/already-exists' });
    }
    transaction.set(ref, { role: 'verifier', email, addedBy, addedAt: Date.now(), ...invite.entry });
    transaction.set(doc(db, 'admin_staff_codes', email), invite.secret);
  });
  return { email, code: invite.secret.code, expiresAt: invite.secret.expiresAt };
}

/** A fresh code and its expiry: the secret for admin_staff_codes, the rest for the invitee's entry. */
function newInvite() {
  // Six digits from the browser's cryptographic source. The modulo bias is a few parts in a million.
  const [n] = crypto.getRandomValues(new Uint32Array(1));
  const code = String(n % 1_000_000).padStart(6, '0');
  const expiresAt = Date.now() + CODE_LIFETIME_MS;
  return {
    secret: { code, expiresAt },
    entry: { pending: true, attempts: 0, tried: '', codeExpiresAt: expiresAt },
  };
}

/**
 * Replace a verifier's code with a new one and reset their tries. This also asks for a code from a
 * verifier added before invite codes, who could otherwise sign in without one.
 */
export async function renewInviteCode(email) {
  const invite = newInvite();
  const batch = writeBatch(db);
  batch.set(doc(db, 'admin_staff_codes', email), invite.secret);
  batch.update(doc(db, 'admin_staff', email), invite.entry);
  await batch.commit();
  return { code: invite.secret.code, expiresAt: invite.secret.expiresAt };
}

/** Where an invited verifier stands: tries used and when their code expires. */
export async function inviteStatus(user) {
  const { claims } = await user.getIdTokenResult();
  const email = (claims.email || '').trim().toLowerCase();
  const snap = await getDocFromServer(doc(db, 'admin_staff', email));
  const data = snap.data() || {};
  return {
    email,
    pending: data.pending === true,
    triesLeft: Math.max(0, CODE_TRIES - (Number(data.attempts) || 0)),
    expiresAt: Number(data.codeExpiresAt) || 0,
  };
}

/**
 * An invited verifier's try at their code. The try is recorded first, then the entry opened; the
 * rules allow the second write only when the recorded try matches the unexpired code, so a wrong
 * code comes back as permission-denied with one try fewer left.
 */
export async function redeemInviteCode(user, code) {
  const status = await inviteStatus(user);
  if (!status.pending) return status;
  if (status.expiresAt <= Date.now()) {
    throw Object.assign(new Error('This code has expired.'), { code: 'invite/expired', status });
  }
  if (status.triesLeft === 0) {
    throw Object.assign(new Error('No tries left.'), { code: 'invite/no-tries', status });
  }
  const ref = doc(db, 'admin_staff', status.email);
  await updateDoc(ref, { attempts: CODE_TRIES - status.triesLeft + 1, tried: code });
  const left = { ...status, triesLeft: status.triesLeft - 1 };
  try {
    const now = Date.now();
    await updateDoc(ref, { pending: false, activatedAt: now, lastSeenAt: now });
  } catch (err) {
    if (err.code !== 'permission-denied') throw err;
    throw Object.assign(new Error('That code is not right.'), { code: 'invite/wrong-code', status: left });
  }
  return { ...left, pending: false };
}
