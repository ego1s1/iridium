<div align="center">

# Iridium

### An EPUB reader that stays out of the way

Fast, native EPUB reading on your phone or tablet. No account, no ads, no tracking.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

</div>

> **Status:** 0.1.0 alpha — the core reader, library and search are in place; expect rough edges.

## Download

Grab the latest APK from [GitHub Releases](https://github.com/ego1s1/iridium/releases) (Android 7.0+). Alpha tags are published as prereleases.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](obtainium://app/%7B%22id%22%3A%20%22com.iridium.reader%22%2C%20%22url%22%3A%20%22https%3A%2F%2Fgithub.com%2Fego1s1%2Firidium%22%2C%20%22author%22%3A%20%22ego1s1%22%2C%20%22name%22%3A%20%22Iridium%22%7D)

## Why Iridium

* **Truly native UI.** Jetpack Compose with Material 3 Expressive: dynamic wallpaper color, AMOLED black, spring physics motion, a floating navigation pill, and layouts that adapt from phones to tablets.
* **A reader built for long sessions.** Paged and scrolled flow, tap zones, volume-key paging, a progress slider, per-book themes (light, sepia, grey, dark, pure black), text size and alignment, and an immersive chrome that hides itself.
* **Highlight and take notes.** Select text to highlight it in one of six colors, attach a note, jump back to it later, and delete what you do not need.
* **Search inside your books.** Iridium indexes chapter text on device and searches the prose, not just titles — results show the sentence around each match and open straight to that chapter.
* **A library that runs itself.** Point Iridium at a folder and your shelf fills in with covers, search, sorts, filters, and a continue-reading shelf. Your files stay exactly where they are — nothing is ever moved or duplicated.
* **Your reading stays yours.** Progress, highlights, bookmarks and preferences live on your device and work fully offline. No accounts, no sync, no telemetry.

## Storage model

Iridium links the folders you choose through Android's Storage Access Framework and reads EPUBs **in place**. The app never copies your books:

* Metadata, covers and the table of contents are indexed on demand, in bounded passes, so a large book costs the same memory as a small one.
* Only cover thumbnails and your reading state (progress, highlights, notes, bookmarks) are stored on device.
* Removing a book unlinks it — the original file is left untouched.

## Build it yourself

Requires JDK 17 and the Android SDK (with NDK 27.2 and CMake 3.22 for the native EPUB core):

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew test                   # unit tests
./gradlew detekt                 # static analysis
./gradlew :app:lintDebug         # Android lint

# full gate
./gradlew :app:assembleDebug :app:lintDebug test detekt
```

Releases are cut by tag; CI builds, signs and publishes them:

```bash
./scripts/new-release.sh 0.1.0   # verify, tag and push; CI publishes the release
```

The native core is optional: JVM unit tests compile a host build of the same C++
and skip themselves when no C++ toolchain is present, and the app always keeps a
pure-Kotlin EPUB engine as a fallback.

## Project layout

```
app/                 app shell: navigation, theming, main tabs
core/                model, database (Room), datastore, designsystem, data
epub-core/           EPUB parsing API + models (seekable source abstraction)
epub-engine/         high-performance JVM EPUB engine (central-directory ZIP + SAX)
epub-native/         native C++ EPUB core (NDK) with a JVM fallback
feature/             onboarding, library, history, detail, reader, settings
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Disclaimer

Iridium hosts zero content — it only reads EPUB files you already own.

## Credits

Built with Kotlin, Jetpack Compose and the [Readium Kotlin toolkit](https://github.com/readium/kotlin-toolkit) for rendering. Thanks to the Coil, Room and AndroidX projects.

## License

Apache 2.0 — see [LICENSE](LICENSE).
