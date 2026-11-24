#!/usr/bin/env bash
# Downloads the open-licence (SIL OFL) fonts used by the themes into res/font.
# Idempotent: existing files are kept. Run from the project root.
set -u
DIR=app/src/main/res/font
LIC=licenses/fonts
BASE=https://raw.githubusercontent.com/google/fonts/main
mkdir -p "$DIR" "$LIC"

fetch() { # fetch <res_name> <path-in-google/fonts>...
  local out="$DIR/$1.ttf"; shift
  if [ -s "$out" ]; then echo "have $out"; return 0; fi
  for p in "$@"; do
    if curl -fsSL --retry 2 "$BASE/$p" -o "$out"; then
      echo "got  $out <- $p"
      local d; d=$(dirname "$p"); curl -fsSL "$BASE/$d/OFL.txt" -o "$LIC/$(basename "$d")-OFL.txt" || true
      return 0
    fi
  done
  rm -f "$out"; echo "::warning::Could not fetch font $1 (theme will use the default font)"
}

# Symbian  -> Fira Sans (same designer as Nokia Sans)
fetch fira_sans_regular ofl/firasans/FiraSans-Regular.ttf
fetch fira_sans_bold    ofl/firasans/FiraSans-Bold.ttf
# Windows XP -> PT Sans (Tahoma-like humanist)
fetch pt_sans_regular   ofl/ptsans/PT_Sans-Web-Regular.ttf ofl/ptsans/PTSans-Regular.ttf
fetch pt_sans_bold      ofl/ptsans/PT_Sans-Web-Bold.ttf ofl/ptsans/PTSans-Bold.ttf
# Gingerbread -> Open Sans (Droid Sans' successor, same designer)
fetch open_sans         "ofl/opensans/OpenSans%5Bwdth,wght%5D.ttf"
# Vista / 7 -> Noto Sans (Segoe UI-like)
fetch noto_sans         "ofl/notosans/NotoSans%5Bwdth,wght%5D.ttf"
# iPhone OS 1 -> Arimo (Helvetica/Arial metrics)
fetch arimo             "ofl/arimo/Arimo%5Bwght%5D.ttf" "apache/arimo/Arimo%5Bwght%5D.ttf"
# iOS -> Inter (SF-like)
fetch inter             "ofl/inter/Inter%5Bopsz,wght%5D.ttf" "ofl/inter/Inter%5Bslnt,wght%5D.ttf"
# BlackBerry -> Source Sans 3 (Slate-like)
fetch source_sans_3     "ofl/sourcesans3/SourceSans3%5Bwght%5D.ttf"
# Snaptu -> Nunito (rounded, friendly)
fetch nunito            "ofl/nunito/Nunito%5Bwght%5D.ttf"
# X-plore -> Roboto Condensed (compact lists)
fetch roboto_condensed  "ofl/robotocondensed/RobotoCondensed%5Bwght%5D.ttf" "apache/robotocondensed/RobotoCondensed%5Bwght%5D.ttf"
ls -la "$DIR"
