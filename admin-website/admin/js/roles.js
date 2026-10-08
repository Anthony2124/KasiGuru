// roles.js — who may use the portal, and as what.
//
// Two roles. An admin carries the `admin` custom claim (functions/set_admin_claim.js). A verifier is
// anyone whose verified email an admin listed on the Team page (admin_staff/{lower-case email}).
// Verifiers moderate everything except APK releases and blocking users; firestore.rules enforces the
// same split server-side, so hiding a control here is convenience, not security.
import { db, doc, getDoc, updateDoc } from './firebase-config.js';

// How stale a verifier's "last signed in" may get before it is stamped again: once an hour keeps the
// Team page truthful without a write on every page load.
const SEEN_EVERY_MS = 60 * 60 * 1000;

export const ROLE_LABEL = { admin: 'Admin', verifier: 'Verifier' };

/** 'admin', 'verifier', or null for an account with no portal access. */
export async function resolveRole(user, { forceRefresh = false } = {}) {
  if (!user) return null;
  const token = await user.getIdTokenResult(forceRefresh);
  if (token.claims && token.claims.admin === true) return 'admin';
  const email = (user.email || '').trim().toLowerCase();
  // An unverified email could be anyone's; the rules refuse it too.
  if (!email || !user.emailVerified) return null;
  try {
    const ref = doc(db, 'admin_staff', email);
    const snap = await getDoc(ref);
    if (!snap.exists() || snap.data().role !== 'verifier') return null;
    // Lets the Team page say "Active · last signed in …" instead of "Invited". The rules allow a
    // verifier to change only this one field on their own entry; a failure costs nothing.
    if (Date.now() - (Number(snap.data().lastSeenAt) || 0) > SEEN_EVERY_MS) {
      updateDoc(ref, { lastSeenAt: Date.now() }).catch(() => {});
    }
    return 'verifier';
  } catch (e) {
    console.warn('Could not read the verifier list:', e);
    return null;
  }
}
