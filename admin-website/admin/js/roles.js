// roles.js — who may use the portal, and as what.
//
// Two roles. An admin carries the `admin` custom claim (functions/set_admin_claim.js). A verifier is
// anyone whose verified email an admin listed on the Team page (admin_staff/{lower-case email}).
// Verifiers moderate everything except APK releases and blocking users (they flag a user for an
// admin instead) and cannot back up or restore; firestore.rules enforces the
// same split server-side, so hiding a control here is convenience, not security.
import { db, doc, getDocFromServer, updateDoc, runTransaction } from './firebase-config.js';

// How stale a verifier's "last signed in" may get before it is stamped again: once an hour keeps the
// Team page truthful without a write on every page load.
const SEEN_EVERY_MS = 60 * 60 * 1000;

export const ROLE_LABEL = { admin: 'Admin', verifier: 'Verifier' };

/** 'admin', 'verifier', or null for an account with no portal access. */
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
  await runTransaction(db, async (transaction) => {
    if ((await transaction.get(ref)).exists()) {
      throw Object.assign(new Error(`${email} is already on the team.`), { code: 'staff/already-exists' });
    }
    transaction.set(ref, { role: 'verifier', email, addedBy, addedAt: Date.now() });
  });
  return email;
}
