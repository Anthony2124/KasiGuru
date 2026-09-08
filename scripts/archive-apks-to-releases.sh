#!/usr/bin/env bash
#
# Uploads the archived APKs in admin-website/download/ to their GitHub Releases.
#
# Why this exists: *.apk is gitignored, so those binaries live in exactly one place —
# whichever working copy happens to hold them. Every release before v1.14.0 was hosted
# on the Vercel download site, which is redeployed from a clean checkout and therefore
# carries only the APK CI just built. The older files survive only because they were
# never deleted from a local folder. This moves them somewhere durable, and is the
# prerequisite for functions/backfill_app_releases.js repointing app_releases at them.
#
# Safe to re-run: existing releases are reused and assets are replaced with --clobber.
#
# Usage (from the repo root):
#   scripts/archive-apks-to-releases.sh            # dry run — prints what it would do
#   scripts/archive-apks-to-releases.sh --apply
#
# Requires the gh CLI, authenticated as someone with push access to the repo.

set -euo pipefail

APPLY=0
[ "${1:-}" = "--apply" ] && APPLY=1

DOWNLOAD_DIR="admin-website/download"
REPO="${KASIGURU_REPO:-Anthony2124/KasiGuru}"

run() {
  if [ "$APPLY" = "1" ]; then
    "$@"
  else
    echo "  would run: $*"
  fi
}

test -d "$DOWNLOAD_DIR" || { echo "Run this from the repo root ($DOWNLOAD_DIR not found)." >&2; exit 1; }
command -v gh >/dev/null || { echo "The gh CLI is required." >&2; exit 1; }

[ "$APPLY" = "1" ] || echo "DRY RUN — pass --apply to actually upload."
echo "Repo: $REPO"

# Sorted so the highest version is processed last and can be marked "Latest" at the end.
mapfile -t APKS < <(ls "$DOWNLOAD_DIR"/kasiguru-v*.apk 2>/dev/null | sort -V)
[ "${#APKS[@]}" -gt 0 ] || { echo "No versioned APKs in $DOWNLOAD_DIR — nothing to archive."; exit 0; }

NEWEST_TAG=""
NEWEST_APK=""

for APK in "${APKS[@]}"; do
  BASE=$(basename "$APK")
  NAME=${BASE#kasiguru-v}
  NAME=${NAME%.apk}
  TAG="v$NAME"
  NEWEST_TAG="$TAG"
  NEWEST_APK="$APK"

  echo
  echo "$BASE -> $TAG"

  if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
    echo "  release $TAG already exists"
  else
    # Several shipped versions were never tagged (1.11.0-1.13.0 among them), so the
    # release has to create its own tag. --target main is the honest approximation:
    # it records that this build existed without pretending to know which commit
    # produced it, and the notes say so rather than leaving a mystery tag behind.
    # --latest=false keeps GitHub's "Latest release" pointer from jumping to whichever
    # archival release happened to be created most recently.
    echo "  creating release $TAG (archival)"
    run gh release create "$TAG" \
      --repo "$REPO" \
      --title "KasiGuru $TAG" \
      --notes "Archived build. Uploaded retroactively so this version keeps a permanent download URL; the tag may not correspond to the exact commit this APK was built from." \
      --target main \
      --latest=false
  fi

  echo "  uploading $BASE"
  run gh release upload "$TAG" "$APK" --repo "$REPO" --clobber
done

# The newest archived build also gets the fixed asset name the download page's static
# href depends on: /releases/latest/download/kasiguru-latest.apk only resolves if some
# release is marked Latest and carries an asset by that exact name.
echo
echo "Marking $NEWEST_TAG as Latest and attaching kasiguru-latest.apk"
LATEST_COPY="$(dirname "$NEWEST_APK")/kasiguru-latest.apk"
if [ -f "$LATEST_COPY" ]; then
  run gh release upload "$NEWEST_TAG" "$LATEST_COPY" --repo "$REPO" --clobber
else
  echo "  ! $LATEST_COPY is missing; the static download href will 404 until a release ships one."
fi
run gh release edit "$NEWEST_TAG" --repo "$REPO" --latest

echo
echo "Done. Next: from functions/, run"
echo "  node backfill_app_releases.js <service-account.json>          # dry run"
echo "  node backfill_app_releases.js <service-account.json> --apply"
