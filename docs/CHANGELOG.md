# Changelog

User-facing entries only. One line per user-visible change.

## Unreleased

### Added

- Now-Reading hero on the library: jump straight back into your current book.
- Floating search island: the search bar lifts with color and elevation as you scroll.
- Quick-filter chips (All / Unread / In progress / Finished) at the top of the library grid.
- Bottom action island: link a folder, rescan, or index chapters from one floating dock.
- Long-press any book cover for quick actions: read, details, or remove with confirmation.
- Sort-and-display sheet: sort order, grid density, and card style in one place.
- Book cards with author/percentage subtitles, bookmark badges, and wavy reading progress.
- Refreshed book details: cover hero, Resume/Start button, chapter list, and bookmark/highlight management with delete.
- Remove-book confirmation dialog, so deletions are always deliberate.
- Reader settings, contents, and highlights sheets, plus a highlight-selection menu entry.
- Position pill: your percentage and location stay visible while the reader controls hide.
- Volume-key page turning and keep-screen-on while reading (opt-in crash reports stay on device).
- Open-source licenses screen listing every library the app is built with.
- Haptics toggle plus tactile feedback on navigation, scrubbing, toggles, and dialogs.
- Eleven color themes (Iridium, Ocean, Forest, Sunset, Catppuccin, Nord, Gruvbox, Dracula, Tokyo Night, Everforest, Monochrome) with true-black AMOLED mode and system-palette matching on Android 12+.
- Expressive component set: tactile buttons, dialogs, stat cards, section headers, filter pills, wavy loading rings, squiggly sliders, and setting rows with icon beds.
- Physics-based motion throughout: springy entrances, full-width screen transitions, morphing covers, and predictive-back support.
- Reading history grouped by day (Today / Yesterday / earlier) with per-book progress.

### Changed

- Reader controls now float over the page with backdrop blur instead of resizing the text; showing or hiding them never reflows your book.
- The book fills the whole screen edge-to-edge with stable safe insets for the camera and gesture areas.
- Library top bar collapses on scroll with an activity dot on the tune icon.
- Grid cards, detail header, and settings all use the new type scale and tonal surfaces.

### Fixed

- Fixed installs on devices with 16 KB memory pages (native library packaging).
- Fixed a crash opening the details of books whose chapters share the same file.
- Fixed the reader never attaching before its session exists, which left a blank page.
- Fixed chapter list crashes on books with repeated section links.
- Library scroll resets to the top when the filter or search text changes.

## Unreleased — reader chrome, dictionary, settings, history
- Added: reader scrubber island + EPUB action dock (flow/theme/contents/highlights/settings) with Haze blur chrome.
- Added: tap-zone inversion math + TapZoneOverlay preview component.
- Added: word lookup popup (long-press a word for Wiktionary definitions + copy); INTERNET permission for definitions only.
- Added: volume-keys paging respects hidden chrome + invert preference; new keep-screen-on/volume/counter rows in reader settings.
- Added: history stats bento (range, hero, activity, streak, top books) with collapsing header.
- Added: settings About hero (brand, creator, links, privacy dialog), haptics switch, icon segmented controls.
- Added: open-source licenses screen + About row.
- Added: reading-sessions log, storage-usage API, component preview catalog (developer-facing).
- Changed: reader chrome auto-hide restarts on interaction; locator jumps hide chrome.
- Fixed: dictionary no longer swallows CancellationException; parse/network/unknown-word errors mapped distinctly.

## Unreleased — full-storage library
- Changed: library now scans every EPUB on your device automatically (all-files access, granted once in onboarding). Folder linking removed.
- Added: onboarding Reading step (text size, line spacing, book colors); line-height slider in reader settings.
- Fixed: books open from direct file paths; legacy linked rows migrate on first scan.
