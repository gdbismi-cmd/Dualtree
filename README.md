# DualTree – an X-plore-style file manager for Android

Kotlin + Jetpack Compose. Min Android 8.0 (API 26), targets Android 15 (API 35).

## Build
1. Open this folder in **Android Studio** (Ladybug or newer) and let Gradle sync.
2. Press **Run** to install on a device/emulator, or build an APK with
   `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
3. On first launch tap **Grant access** and enable *Allow access to manage all files*.

## How it works (X-plore style)
- **Two panes** — on phones one pane fills the screen and you **slide left/right** (or tap the
  1 / 2 tabs) to switch; on tablets and in landscape they sit side by side. Each pane is a **collapsible tree**: tap a folder to expand it inline.
  The highlighted pane header is the *active* pane.
- **Tap** a file to open it (images and text open in the built-in viewer, others via "Open with").
- **Long-press** to start multi-selection; tap more items to add them. Back clears it.
- **Copy → / Move →** send the selection (or highlighted item) to the folder open in the *other* pane.
- Zip / Unzip also output into the other pane's folder.

## Features
- Internal storage, SD cards/USB and `/` as tree roots, with free-space bars
- Copy, move (instant rename on same volume), delete, rename, new folder — with progress + cancel
- Create ZIP, extract ZIP (zip-slip protected); tap a .zip to extract
- Recursive search with wildcards (`*.jpg`, `IMG_20??*`), tap a result to reveal it in the tree
- Image viewer: swipe through the folder, pinch / double-tap to zoom
- Text viewer with line numbers (up to 2 MB)
- Image thumbnails, real app icons for .apk files (plus app name/package/version in Details), file-type icons, hidden-file toggle, sort by name/size/date/type
- Details: size (recursive), item counts, permissions, free space
- Share files, install APKs, Material You colors
- Touch-to-glow feedback on every row and button, haptic buzz on long-press
- **15 themes** (⋮ → Theme…), each with its own font and touch-glow:
  Android 2.3 Gingerbread (orange-gradient select, Open Sans ≈ Droid Sans), Android 4.0 ICS (Holo blue, Roboto),
  Material You (system / light / dark / pure black – soft bloom), X-plore (blue bloom + yellow edge),
  Windows XP (Luna bars, orange hot-track), Windows Vista (black Aero glass, aqua glass glow),
  Windows 7 (light glass, sky-blue Aero glow), iPhone OS 1 (pinstripes, glossy blue select),
  iOS (flat grey press), BlackBerry OS (glowing blue focus bar), Symbian (S60 blue, light sweep),
  Snaptu (navy, orange neon ripple ring)
- **Disk map** (toolbar or ⋮): treemap of what's using space; tap to select, double-tap to zoom in,
  long-press or "Show in pane" to jump to it
- Symlinks are never followed when copying/deleting (safe on `/`)

## Code map
```
app/src/main/java/com/example/dualtree/
  MainActivity.kt          permission flow + entry point
  ExplorerViewModel.kt     pane/tree state, all user actions
  data/FileOps.kt          copy/move/delete/zip/unzip/search (cancellable, IO thread)
  data/Fs.kt               storage roots, listing/sorting, file types, formatting
  model/Models.kt          Node, StorageRoot, DialogState…
  ui/ExplorerScreen.kt     dual-pane tree UI, toolbar, progress strip
  ui/Dialogs.kt            confirm / input / details / search dialogs
  ui/Viewers.kt            image pager + text viewer
```

## Ideas for next steps
FTP/SMB/cloud, app manager, hex viewer, bookmarks/favourites, built-in media player,
7z/RAR support, Play Store–compliant SAF mode (Google Play restricts `MANAGE_EXTERNAL_STORAGE`
to file-manager apps, which this qualifies as, but requires a declaration form).

## Download the APK
Every push to `main` is built by GitHub Actions. Grab the latest APK from the
repository's **Releases** page (or the *Actions* tab → latest run → *Artifacts*).

## Fonts
Each theme uses an open-licence stand-in for its OS font (the originals can't be redistributed):
PT Sans (XP/Tahoma), Noto Sans (Vista & 7/Segoe UI), Arimo (iPhone OS 1/Helvetica), Inter (iOS),
Source Sans 3 (BlackBerry), Fira Sans (Symbian/Nokia Sans), Nunito (Snaptu), Roboto Condensed (X-plore),
Open Sans (Gingerbread/Droid Sans). They're fetched into `app/src/main/res/font` by
`scripts/fetch-fonts.sh` (CI does this automatically); licences are in `licenses/fonts`.
