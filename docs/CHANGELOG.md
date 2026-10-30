# Changelog

User-facing entries only. One line per user-visible change.

## Unreleased

_Nothing yet._

## 0.1.1

### Added

- Reading preview in onboarding that updates live as you change text size, spacing and book colour.
- Extra dim: lowers the reading text itself for dark rooms, without lighting up a black page.
- Selection bar with Dictionary and Web search, so a definition only opens when you ask for it.
- Theme-aware text selection that uses your palette instead of a fixed pale blue.
- Web search from a selection.
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
- Volume-key page turning and keep-screen-on while reading (opt-in crash reports stay on device).
- Open-source licenses screen listing every library the app is built with.
- Haptics toggle plus tactile feedback on navigation, scrubbing, toggles, and dialogs.
- Eleven color themes (Iridium, Ocean, Forest, Sunset, Catppuccin, Nord, Gruvbox, Dracula, Tokyo Night, Everforest, Monochrome) with true-black AMOLED mode and system-palette matching on Android 12+.
- Expressive component set: tactile buttons, dialogs, stat cards, section headers, filter pills, wavy loading rings, squiggly sliders, and setting rows with icon beds.
- Physics-based motion throughout: springy entrances, full-width screen transitions, morphing covers, and predictive-back support.
- Reading history grouped by day (Today / Yesterday / earlier) with per-book progress.

### Changed

- Reading is always paged; the Auto/Paged/Scrolled control is gone because two of those behaved identically.
- Grid columns are now a straight 2 / 3 / 4 choice, defaulting to two.
- Book colour now defaults to pure black (AMOLED) on a fresh install.
- Default reading typography is larger and airier, matched to a printed page.
- Page turns slide a full page and are capped at four per second so rapid tapping never tears the animation.
- The page counter follows the real rendered layout, so it changes with the font and reflow and advances instantly on a tap.
- Chrome stays up while you turn pages and hides only once you stop, and its timer restarts on every tap.
- The scrubber is a static progress track with the current chapter shown beside it.
- Reader controls now float over the page with backdrop blur instead of resizing the text; showing or hiding them never reflows your book.
- The book fills the whole screen edge-to-edge with stable safe insets for the camera and gesture areas.
- Library top bar collapses on scroll with an activity dot on the tune icon.
- Grid cards, detail header, and settings all use the new type scale and tonal surfaces.

### Fixed

- Fixed back and home gestures being swallowed when the reader chrome was hidden.
- Fixed the status bar and gesture pill reappearing after a cancelled back gesture.
- Fixed the reader showing a blank page between the spinner and the text.
- Fixed dictionary lookups failing on longer entries with "couldn't understand the response".
- Fixed the page counter freezing or jumping by two or three during fast page turns.
- Fixed selection handles being torn down mid-drag, which made extending a selection impossible.
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
