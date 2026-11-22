#!/usr/bin/env bash
# Prints the newest loader version that can actually be downloaded.
#   ci/loader-version.sh forge 1.20.1 47.4.29          -> e.g. 47.4.29  (Forge for Minecraft 1.20.1)
#   ci/loader-version.sh neoforge 21.11 21.11.42       -> e.g. 21.11.48 (NeoForge versions starting with 21.11.)
#   ci/loader-version.sh neoforge-legacy 1.20.1 1.20.1-47.1.106
# Versions are sorted as versions (not in file order), and each candidate's installer is checked: a version can show
# up in maven-metadata.xml minutes before its files are uploaded. If nothing works, the last tested version (3rd
# argument) is used, so one bad upload upstream doesn't fail our build.
set -u
kind="$1"; mc="$2"; fallback="${3:-}"

fetch() { curl -fsS --retry 4 --retry-delay 5 --retry-all-errors "$1" 2>/dev/null; }
exists() { curl -fsSI --retry 2 --retry-delay 3 "$1" >/dev/null 2>&1; }

case "$kind" in
  forge)
    list=$(fetch https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml \
      | grep -o "<version>${mc}-[^<]*</version>" | sed 's/<[^>]*>//g' | sed "s/^${mc}-//" | sort -V -r | head -6)
    url() { echo "https://maven.minecraftforge.net/net/minecraftforge/forge/${mc}-$1/forge-${mc}-$1-installer.jar"; } ;;
  neoforge)
    list=$(fetch https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml \
      | grep -oE "<version>${mc//./\\.}\.[0-9]+(-beta)?</version>" | sed 's/<[^>]*>//g' | sort -V -r | head -6)
    url() { echo "https://maven.neoforged.net/releases/net/neoforged/neoforge/$1/neoforge-$1-installer.jar"; } ;;
  neoforge-legacy)
    list=$(fetch https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml \
      | grep -oE "<version>${mc//./\\.}-[^<]*</version>" | sed 's/<[^>]*>//g' | sort -V -r | head -6)
    url() { echo "https://maven.neoforged.net/releases/net/neoforged/forge/$1/forge-$1-installer.jar"; } ;;
  *) echo "unknown loader kind $kind" >&2; exit 2 ;;
esac

for v in $list; do
  if exists "$(url "$v")"; then
    echo "$v"
    exit 0
  fi
  echo "skipping $kind $v: files not downloadable yet" >&2
done
if [ -n "$fallback" ]; then
  echo "using the last tested $kind version $fallback" >&2
  echo "$fallback"
  exit 0
fi
echo "no downloadable $kind for $mc" >&2
exit 1
