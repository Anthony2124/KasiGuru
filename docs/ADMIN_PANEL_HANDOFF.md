# Admin panel handoff to Anthony

Anthony is continuing the admin panel work from this checkpoint (2026-10-09).
The portal source is `admin-website/admin/`; the learner web app is separate in
`admin-website/webapp/`.

## Changes in this checkpoint

- `js/auth.js`: guards overlapping sign-in attempts, clears timeout timers,
  explains blocked Google pop-ups and conflicting sign-in methods, and reports
  failed access checks without treating them as a confirmed access denial.
- `js/roles.js`: resolves verifier access from the refreshed token identity and
  a server read of `admin_staff`, limits access checks to ten seconds, and adds
  verifiers through a transaction so an existing invitation cannot be overwritten.
- `js/app.js` and `dashboard.html`: offer a retry after an access-check failure,
  clarify how Google accounts join the team, and hide a removed member's invite.
- `js/firebase-config.js`: exports the server-read and transaction helpers.
- `firestore.rules`: requires verified email for reading a member's own entry,
  enforces lowercase invitation keys, and restricts last-seen updates to verifiers.

## Invite codes (2026-10-10)

A new verifier now gets two emails from the admin's own mail app, in order: the console
link, then a one-time 6-digit code. After "Continue with Google" they see "Enter your invite
code"; nothing else opens until the code is accepted.

- `admin_staff/{email}` carries `pending`, `attempts`, `tried` and `codeExpiresAt` while the
  invite waits. `isVerifier()` refuses a pending entry; entries from before codes have no
  `pending` field and keep their access.
- The code itself is in `admin_staff_codes/{email}`, readable only by admins, because an
  invitee can read their own `admin_staff` entry.
- Redeeming is two writes: the try is recorded (attempts + 1, at most five), then the entry
  opens only if the recorded try equals the unexpired code. One write could not count wrong
  guesses, since a denied write leaves no trace.
- "New code" on the Team page replaces the code and resets the tries. On a verifier added
  before codes who has not signed in yet, it also starts requiring a code.

## Continue here

1. Run the local rules suite with Java 21 and the Firebase CLI:
   `firebase emulators:exec --only firestore --project demo-kasiguru-ui "node scripts/tests/staff-roles-rules.cjs"`.
2. Check admin and invited-verifier sign-in in a browser, including blocked
   pop-ups, an unavailable access check, retry, and switching accounts.
3. Check duplicate invitations and removal on the Team page. Confirm a removed
   verifier cannot read or change protected data, including from an open session.
4. Review and deploy the Firestore rules alongside the portal changes when ready.

The existing rules suite needs Java 21; this workstation's default Java is 17.
Browser checks and rules deployment are still pending for this handoff.

## Checkpoint verification

Project structure, shared web tokens, Android/web content sync, and JavaScript
syntax checks passed. The existing Node suite passed all 14 tests; the learner
web app passed all 150 tests and its TypeScript check. Android unit tests and
the debug APK build also passed.

## Hosting context

`HANDOFF.md` records that the admin Vercel project is linked to `main`, so a push
can deploy the portal. Verify current project settings before publishing. This
checkpoint is a local Git commit; it does not push or deploy the rules.

Read `AGENTS.md`, `docs/CODE_MAP.md`, `PRODUCT.md`, and `DESIGN.md` before further
changes. Keep service-account credentials and signing material outside source.
