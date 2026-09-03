#!/usr/bin/env bash
# Prepares a release on the current feature/fix branch, following this repo's
# established convention: bump app/build.gradle's versionName, prepend a
# CHANGELOG.md entry, and create the single combined commit release PRs have
# historically carried ("Bump version to X.Y.Z and update changelog").
#
# This script does NOT tag or create a GitHub Release. The published SDK
# version is the git tag itself (JitPack builds straight from tags) — per
# repo convention, tagging/release happens automatically once the PR merges
# to dev (see .github/workflows/release.yml). Run this on your feature branch
# before opening the PR.
#
# Usage:
#   scripts/prepare_release.sh --bump <patch|minor|major> --category <category> "<bullet 1>" ["<bullet 2>" ...]
#   scripts/prepare_release.sh --version <X.Y.Z> --category <category> "<bullet 1>" ...
#
# --category accepts one of the established categories (or any custom text):
#   "New Update", "Bug Fix", "Security"
#
# Example:
#   scripts/prepare_release.sh --bump patch --category "Bug Fix" \
#     "Fixed the WebView JS bridge URL scheme validation."

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

APP_GRADLE="app/build.gradle"
CHANGELOG="CHANGELOG.md"
INTEGRATION_BRANCH="dev"

BUMP=""
NEW_VERSION=""
CATEGORY=""
BULLETS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bump) BUMP="$2"; shift 2 ;;
    --version) NEW_VERSION="$2"; shift 2 ;;
    --category) CATEGORY="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) BULLETS+=("$1"); shift ;;
  esac
done

if [[ -z "$CATEGORY" || ${#BULLETS[@]} -eq 0 ]]; then
  echo "error: --category and at least one changelog bullet are required" >&2
  exit 1
fi

if [[ -n "$BUMP" && -n "$NEW_VERSION" ]]; then
  echo "error: pass either --bump or --version, not both" >&2
  exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" == "$INTEGRATION_BRANCH" ]]; then
  echo "error: run this on a feature/fix branch, not $INTEGRATION_BRANCH (matches repo convention: the bump+changelog commit lives on the PR branch)" >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "error: working tree is not clean; commit or stash your changes first" >&2
  exit 1
fi

CURRENT_VERSION="$(sed -n 's/.*versionName[[:space:]]*"\([^"]*\)".*/\1/p' "$APP_GRADLE" | head -1)"
if [[ -z "$CURRENT_VERSION" ]]; then
  echo "error: could not read current versionName from $APP_GRADLE" >&2
  exit 1
fi

if [[ -n "$BUMP" ]]; then
  IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
  case "$BUMP" in
    major) NEW_VERSION="$((MAJOR + 1)).0.0" ;;
    minor) NEW_VERSION="${MAJOR}.$((MINOR + 1)).0" ;;
    patch) NEW_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))" ;;
    *) echo "error: --bump must be patch, minor, or major" >&2; exit 1 ;;
  esac
elif [[ -z "$NEW_VERSION" ]]; then
  echo "error: pass --bump <patch|minor|major> or --version <X.Y.Z>" >&2
  exit 1
fi

if [[ ! "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "error: version must look like X.Y.Z, got: $NEW_VERSION" >&2
  exit 1
fi

TAG="v${NEW_VERSION}"
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  echo "error: tag $TAG already exists" >&2
  exit 1
fi

case "$CATEGORY" in
  "New Update") CATEGORY_HEADING="New Update 🚀" ;;
  "Bug Fix") CATEGORY_HEADING="Bug Fix 🐛" ;;
  "Security") CATEGORY_HEADING="Security 🔒" ;;
  *) CATEGORY_HEADING="$CATEGORY" ;;
esac

echo "Bumping $CURRENT_VERSION -> $NEW_VERSION ($TAG)"

# --- 1 & 2. Bump versionName in app/build.gradle and prepend CHANGELOG entry, single commit ---
# (plain s/// — not the GNU-only "0,/regex/" address form, which BSD/macOS sed
# doesn't support; app/build.gradle has exactly one versionName occurrence)
sed -i.bak "s/versionName \"$CURRENT_VERSION\"/versionName \"$NEW_VERSION\"/" "$APP_GRADLE" && rm -f "$APP_GRADLE.bak"

if ! grep -q "versionName \"$NEW_VERSION\"" "$APP_GRADLE"; then
  echo "error: failed to update versionName in $APP_GRADLE" >&2
  exit 1
fi

TODAY="$(date +%Y-%m-%d)"
ENTRY_FILE="$(mktemp)"
{
  echo "## [${TAG}](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/${TAG}) (${TODAY})"
  echo ""
  echo "### ${CATEGORY_HEADING}"
  for b in "${BULLETS[@]}"; do
    echo "* ${b}"
  done
  echo ""
  echo "---"
  echo ""
} > "$ENTRY_FILE"

# Insert right after the two-line intro (title + "All notable changes..."),
# before the first existing entry (or at EOF if the changelog is empty).
awk -v entryfile="$ENTRY_FILE" '
  inserted { print; next }
  /^## \[v[0-9]/ {
    while ((getline line < entryfile) > 0) print line
    inserted = 1
    print
    next
  }
  { print }
  END {
    if (!inserted) {
      while ((getline line < entryfile) > 0) print line
    }
  }
' "$CHANGELOG" > "$CHANGELOG.new" && mv "$CHANGELOG.new" "$CHANGELOG"
rm -f "$ENTRY_FILE"

git add "$APP_GRADLE" "$CHANGELOG"
git commit -m "Bump version to ${NEW_VERSION} and update changelog"

echo ""
echo "Done. Push this branch and open the PR as usual — merging to $INTEGRATION_BRANCH will"
echo "automatically tag ${TAG} and publish the GitHub Release."
