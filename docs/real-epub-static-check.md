# Real-EPUB host-side static parse check — `pg64317-images-3.epub`

Date: 2026-10-06 (UTC). Task t22. Scope: host-side only, no emulator, no app-code changes.
Source file (repo root, untracked): `pg64317-images-3.epub` — **353,457 bytes** on disk
(compressed entries total 351,847 + ZIP overhead).

Book identity (from OPF): **_The Great Gatsby_** — F. Scott Fitzgerald (Project Gutenberg #64317,
Ebookmaker 0.14.6). EPUB **3.0**, `en`.

## 1. ZIP listing (`unzip -l` / `unzip -lv`)

13 entries, 560,312 bytes uncompressed → 351,847 bytes compressed (37% saved).
`unzip -t`: **No errors detected**. `mimetype` is Stored (spec-correct first entry),
content `application/epub+zip`.

| # | Entry | Uncompressed | Compressed | Method | Notes |
|---|---|---|---|---|---|
| 1 | `mimetype` | 20 | 20 | Stored | must be first, uncompressed — OK |
| 2 | `META-INF/container.xml` | 252 | 178 | Defl:N | rootfile → `OEBPS/content.opf` — OK |
| 3 | `OEBPS/9184538700278761470_cover.jpg` | 230,606 | 230,606 | Stored | cover, 1108×1584 q=60 (per OPF comment) |
| 4 | `OEBPS/pgepub.css` | 986 | 413 | Defl:N | |
| 5 | `OEBPS/0.css` | 1,535 | 371 | Defl:N | |
| 6 | `OEBPS/1.css` | 1,828 | 558 | Defl:N | |
| 7 | `OEBPS/716005862216510784_64317-h-0.htm.xhtml` | 158,387 | 59,558 | Defl:N | body part 1 (front matter + ch. I–V) |
| 8 | `OEBPS/716005862216510784_64317-h-1.htm.xhtml` | 138,015 | 50,407 | Defl:N | body part 2 (ch. VI–IX) |
| 9 | `OEBPS/716005862216510784_64317-h-2.htm.xhtml` | 20,161 | 7,126 | Defl:N | PG license footer |
| 10 | `OEBPS/toc.xhtml` | 2,106 | 589 | Defl:N | EPUB3 nav (`properties="nav"`) |
| 11 | `OEBPS/toc.ncx` | 2,865 | 603 | Defl:N | EPUB2 fallback NCX |
| 12 | `OEBPS/wrap0000.xhtml` | 597 | 347 | Defl:N | SVG cover wrapper (`properties="svg"`) |
| 13 | `OEBPS/content.opf` | 2,954 | 1,071 | Defl:N | package doc, EPUB 3.0 |

## 2. OPF manifest + spine (`OEBPS/content.opf`)

Package `version="3.0"`, `unique-identifier="id"`. Metadata: `dc:title` The Great Gatsby,
`dc:creator` F. Scott Fitzgerald, `dc:language` en, `dc:identifier`
`http://www.gutenberg.org/64317`, `dcterms:modified` 2026-09-25. Cover declared twice
and consistently: `<meta name="cover" content="id-…"/>` (EPUB2-style) **and**
`properties="cover-image"` (EPUB3-style) on the same item.

### Manifest (10 items)

| id | href | media-type | properties |
|---|---|---|---|
| `id-1516049535276682277` | `9184538700278761470_cover.jpg` | `image/jpeg` | `cover-image` |
| `item1` | `pgepub.css` | `text/css` | — |
| `item2` | `0.css` | `text/css` | — |
| `item3` | `1.css` | `text/css` | — |
| `pg-header` | `716005862216510784_64317-h-0.htm.xhtml` | `application/xhtml+xml` | — |
| `chapter-6` | `716005862216510784_64317-h-1.htm.xhtml` | `application/xhtml+xml` | — |
| `pg-footer` | `716005862216510784_64317-h-2.htm.xhtml` | `application/xhtml+xml` | — |
| `ncx` | `toc.xhtml` | `application/xhtml+xml` | `nav` |
| `ncx2` | `toc.ncx` | `application/x-dtbncx+xml` | — |
| `coverpage-wrapper` | `wrap0000.xhtml` | `application/xhtml+xml` | `svg` |

Every manifest href resolves to an existing ZIP entry (no dangling refs);
every ZIP content entry is listed in the manifest (no orphans).

### Spine (4 itemrefs, `toc="ncx2"`)

| Order | idref | Resolves to |
|---|---|---|
| 1 | `coverpage-wrapper` | `OEBPS/wrap0000.xhtml` (SVG cover page) |
| 2 | `pg-header` | `OEBPS/716005862216510784_64317-h-0.htm.xhtml` |
| 3 | `chapter-6` | `OEBPS/716005862216510784_64317-h-1.htm.xhtml` |
| 4 | `pg-footer` | `OEBPS/716005862216510784_64317-h-2.htm.xhtml` |

All `idref`s resolve; spine order matches manifest chunk comments
(header → chapter-6 → footer).

## 3. ToC: nav + NCX (12 entries each, consistent)

The engine prefers the EPUB3 nav (`toc.xhtml`), falling back to NCX (`toc.ncx`).
Both list the **same 12 entries** (title page, ToC, chapters I–IX, PG license);
NCX `playOrder` 1–12 matches nav order; fragment targets (`#chapter-N`)
all sit inside the two body chunks.

| # | Title | Target |
|---|---|---|
| 1 | The Great Gatsby by F. Scott Fitzgerald | `…h-0.htm.xhtml#pgepubid00000` |
| 2 | Table of Contents | `…h-0.htm.xhtml#pgepubid00001` |
| 3–7 | I, II, III, IV, V | `…h-0.htm.xhtml#chapter-1..5` |
| 8–11 | VI, VII, VIII, IX | `…h-1.htm.xhtml#chapter-6..9` |
| 12 | THE FULL PROJECT GUTENBERG™ LICENSE | `…h-2.htm.xhtml#pg-footer-heading` |

ToC doc sizes: nav 2,106 bytes, NCX 2,865 bytes — trivially small, no parse-cost concern.

## 4. Images: count + bytes

| Image | Bytes (stored) | Referenced from |
|---|---|---|
| `OEBPS/9184538700278761470_cover.jpg` (JPEG 1108×1584) | 230,606 (65% of on-disk file) | `wrap0000.xhtml` via SVG `<image>` |

- ZIP image entries: **1** (cover only). No `<img>` / `<image>` tags in any of the
three chapter XHTML files — chapter bodies are text-only.
- The JPEG is Stored (0% deflate, expected for JPEG data) and at 225 KB sits far
below the engine's 16 MB cover cap (`MAX_COVER_BYTES`).
- Despite the `images-3` filename, this variant is **not** image-heavy: one cover,
zero inline illustrations.

## 5. Parse via epub-engine (JVM) + native-core fallback harness

Harness: host `java` program driving the real repo classes
(`com.iridium.epub.engine.IridiumEpubEngine` + `com.iridium.epub.nativecore.NativeEpub`),
no repo edits (harness lives in tmp, classes from the Gradle build). Raw output:

```text
file=/Users/priyanshu/git/Iridium/pg64317-images-3.epub bytes=353457
native.isAvailable=true
native.inspect=title=The Great Gatsby spine=4 chapters=12
jvm.inspectMs=236
jvm.title=The Great Gatsby
jvm.author=F. Scott Fitzgerald
jvm.coverMime=image/jpeg
jvm.coverBytes=230606
jvm.spineCount=4
jvm.chapters=12
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=The Great Gatsby by F. Scott Fitzgerald
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=Table of Contents
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=I
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=II
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=III
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=IV
jvm.chapter href=OEBPS/716005862216510784_64317-h-0.htm.xhtml title=V
jvm.chapter href=OEBPS/716005862216510784_64317-h-1.htm.xhtml title=VI
jvm.chapter href=OEBPS/716005862216510784_64317-h-1.htm.xhtml title=VII
jvm.chapter href=OEBPS/716005862216510784_64317-h-1.htm.xhtml title=VIII
jvm.chapter href=OEBPS/716005862216510784_64317-h-1.htm.xhtml title=IX
jvm.chapter href=OEBPS/716005862216510784_64317-h-2.htm.xhtml title=THE FULL PROJECT GUTENBERG™ LICENSE
jvm.chapterCount=4
jvm.body href=OEBPS/wrap0000.xhtml bytes=597
jvm.body href=OEBPS/716005862216510784_64317-h-0.htm.xhtml bytes=158387
jvm.body href=OEBPS/716005862216510784_64317-h-1.htm.xhtml bytes=138015
jvm.body href=OEBPS/716005862216510784_64317-h-2.htm.xhtml bytes=20161
RESULT=PASS title=[The Great Gatsby] spine=4 chapters=12
```

| Check | JVM engine | Native core (`libiridium_epub.dylib`, host build) |
|---|---|---|
| Title | The Great Gatsby | The Great Gatsby (agree) |
| Author | F. Scott Fitzgerald | (not printed; agreement covered by unit test below) |
| Spine count | 4 | 4 (agree) |
| ToC entries | 12 (nav doc) | 12 (agree) |
| Cover | 230,606 bytes, `image/jpeg` | n/a (coverFd path covered by unit tests) |
| Chapter pipeline (`open` + `chapterBytes`) | 4/4 bodies readable, sizes match ZIP | covered by `chapter pipeline reads a body by href` unit test |
| Inspect time | 236 ms (cold JVM, includes class init) | n/a |

Native was **available** on this host (the `compileHostNative` task built the dylib),
so the primary native path was exercised directly and agrees with the JVM engine —
the JVM-fallback contract was additionally proven by the `malformed archive returns
null` unit test. No exceptions, no fallback titles: clean parse on both engines.

## 6. Unit gate

`./gradlew :epub-engine:test :epub-native:test` (2026-10-06, Homebrew openjdk 17.0.20.1,
`ANDROID_HOME=~/Library/Android/sdk`) → **EXIT 0, BUILD SUCCESSFUL**, 0 failures/errors:

| Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `:epub-engine` `IridiumEpubEngineTest` | 10 | 0 | 0 | 0 |
| `:epub-engine` `HtmlTextTest` | 7 | 0 | 0 | 0 |
| `:epub-engine` `EpubBenchmarkTest` | 2 | 0 | 0 | 0 |
| `:epub-native` `NativeEpubTest` | 6 | 0 | 0 | 0 |
| `:epub-native` `NativeBenchmarkTest` | 1 | 0 | 0 | 0 |
| **Total** | **26** | **0** | **0** | **0** |

Notable: `native library loads on the host`, `native agrees with the jvm engine on
the same archive`, and `malformed archive returns null instead of crashing` all pass —
the exact contracts this EPUB relies on (native-first, JVM fallback, never-throw).

## 7. Verdict

**Well-formed: YES.** The file is a valid EPUB 3.0 container: intact ZIP, spec-correct
`mimetype`, resolvable `container.xml` → OPF, consistent dual cover declaration,
fully-resolving 10-item manifest, 4-item spine with no dangling `idref`s, and dual
nav/NCX ToCs that agree entry-for-entry (12). Both shipped parsers (JVM engine and
native core) inspect it cleanly and agree on title/spine/ToC; the chapter pipeline
reads all 4 spine bodies at their exact expected sizes.

**Image-heavy risks: NONE for this file.** Despite the `images-3` filename, the book
contains exactly one image — the 225 KB Stored cover JPEG (65% of the on-disk file),
far below the 16 MB cover cap — and zero inline chapter images (chapter XHTML is
text-only, largest spine body 158 KB uncompressed). No render/scan slowdown expected
beyond normal text layout; no WebView XML-error risk (all XHTML entries open under
the 64 MB resource cap by two orders of magnitude). Device tasks (t21/t23) can treat
this as a light, fast-loading book; the only watch item is the SVG-wrapped cover page
(`wrap0000.xhtml` first in spine), which is standard Gutenberg output and already
handled by the reader pipeline.
