#!/usr/bin/env bash
# Cut a release by tagging. The tag triggers .github/workflows/release.yml,
# which builds, signs and publishes the GitHub Release. Tags containing
# alpha/beta/rc publish as prereleases.
#
# Usage:
#   ./scripts/new-release.sh 0.1.0            # first alpha
#   ./scripts/new-release.sh 0.1.1            # next patch
#   ./scripts/new-release.sh 0.1.1-alpha.1    # explicit prerelease tag
#   ./scripts/new-release.sh 0.2.0 hotfix     # moniker appended to tag message
set -euo pipefail

MONIKER="${2:-}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ $# -ge 1 ]; then
  VERSION="$1"
else
  LATEST="$(git tag --list | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -n 1 || true)"
  if [ -z "$LATEST" ]; then
    echo "No vX.Y.Z tags found; pass an explicit version." >&2
    exit 1
  fi
  BASE="${LATEST#v}"
  MAJOR="${BASE%%.*}"
  REST="${BASE#*.}"
  MINOR="${REST%%.*}"
  PATCH="${REST##*.}"
  VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
fi

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$ ]]; then
  echo "Invalid version '$VERSION'; expected X.Y.Z or X.Y.Z-suffix." >&2
  exit 1
fi
TAG="v$VERSION"

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree has uncommitted changes; commit or stash first." >&2
  exit 1
fi
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists." >&2
  exit 1
fi

git fetch --tags origin >/dev/null 2>&1 || true
if git ls-remote --tags origin | grep -q "refs/tags/$TAG$"; then
  echo "Tag $TAG already exists on origin." >&2
  exit 1
fi

echo "Verifying the release build for $TAG (this compiles the native core too)..."
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
"$ROOT/gradlew" -p "$ROOT" :app:assembleRelease -PappVersionName="$TAG" >/dev/null
echo "Release build verified."

git tag -a "$TAG" -m "Iridium $TAG${MONIKER:+ ($MONIKER)}"
git push origin "$TAG"

echo "Pushed $TAG."
echo "CI now builds, signs, and publishes the GitHub Release:"
REPO="$(git remote get-url origin | sed -E 's#.*[:/]([^/]+/[^/.]+)(\.git)?$#\1#')"
echo "  https://github.com/$REPO/releases/tag/$TAG"
