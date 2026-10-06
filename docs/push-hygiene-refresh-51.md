# Push-hygiene refresh, 51-ahead (t25) — secrets, binaries, remote

Date: 2026-10-06 · Worktree branch: `agentcraft/wren/t25-refresh-push-hygiene` (from `main`) · Range audited: `origin/main..main` (`bef432c`, 51 commits ahead, 95 files +6625/-27)
Verdict: **DO NOT PUSH without explicit Priyanshu approval.** Push remote is intentionally blocked (see §1). Secrets scan is clean; payload carries 2 files > 1 MiB plus 27 further PNG evidence files. Untracked `pg64317-images-3.epub` is **not** in the push and must stay untracked (§5).

Read-only audit. Zero pushes, zero merges, zero remote changes performed.

## 1. Remote table — push is blocked by design

`git remote -v` (verbatim):

| name | direction | URL |
|------|-----------|-----|
| origin | fetch | `git@github.com:ego1s1/iridium.git` |
| origin | push | `agentcraft-push-blocked:///git@github.com:ego1s1/iridium.git` |

- Push URL scheme `agentcraft-push-blocked://` is **not a real transport** — any `git push` to `origin` fails until Priyanshu unblocks/approves. Confirmed: **agentcraft-push-blocked push remote**.
- No other remotes configured. No push, fetch, pull, or remote-URL change was made during this task (read-only audit).
- Rule restated: **no push and no merge without explicit Priyanshu approval** (report-only task; sibling tasks t26 preview + t27 gate own the remaining verdicts).

## 2. Secrets scan — clean (no live credentials in the push)

Patterns grepped (case-insensitive; working tree excluding `.git/`, `build/`, `.gradle/`; plus `git grep` over tracked files):

```
api[-_]?key, keystore, password, \.pem, \.jks,
PRIVATE KEY, AKIA[0-9A-Z]{16}, AIza[0-9A-Za-z_-]{35},
ghp_, xox[bap]-, sk-live-, aws_secret, github_token, secret[-_]?key
```

High-signal credential patterns (`PRIVATE KEY`, `AKIA…`, `AIza…`, `ghp_`, `xox-`, `sk-live`):

| command | result |
|---------|--------|
| `git grep -n -i -E 'PRIVATE KEY\|AKIA[0-9A-Z]{16}\|AIza…\|ghp_\|xox[bap]-\|sk-live' -- .` | **no live hits** — only self-references inside `docs/push-hygiene-check.md:26,40` (the t8 audit's own pattern list and results table) |

Broader `api-key / keystore / password / pem / jks` refs (after filtering 4,813 benign `password="false"` UI-dump attributes in `docs/device-test/*.xml`):

| file:line | hit | disposition |
|-----------|-----|-------------|
| `.github/workflows/release.yml:8-9,11` | `KEYSTORE_BASE64 / KEY_ALIAS / KEY_PASSWORD / STORE_PASSWORD` comments | CI secret **names only**, no values |
| `.github/workflows/release.yml:65,67,69` | `secrets.KEYSTORE_BASE64 \|\| vars.KEYSTORE_BASE64`, "Using the configured release keystore." | name reference only |
| `.github/workflows/release.yml:83-84,89-91` | `ci-keystore.jks`, `ci-keystore.pass` cache keys | filename references; files themselves untracked + gitignored (`*.jks`) |
| `.github/workflows/release.yml:93-99` | `SECRET_KEYSTORE / SECRET_KEY_PASS / SECRET_STORE_PASS` from secrets/vars | name references only |
| `.github/workflows/release.yml:105,110-120,125` | `base64 -d > release.keystore`, keytool gen, `chmod 600 release.keystore .store.pass .key.pass` | runtime handling, masked; no credential value in repo |
| `.github/workflows/release.yml:192-196,213-223,250` | apksigner `--ks release.keystore`, cleanup `rm -f release.keystore …`, alpha-build notice | no values |
| `scripts/build-release.sh:24` | `--ks "$HOME/.android/debug.keystore"` | local debug-keystore path, not a secret |
| `.gitignore:21-22` | `*.jks`, `*.keystore` | allowlist entries, not credentials |
| `docs/push-hygiene-check.md:24-25,31-34,40-41` | t8 audit's own pattern list + findings | documentation, not credentials |

Result: **no live secrets found.** Caveat: grep is heuristic, not a guarantee — a rotated-key review before the eventual push is still recommended, but nothing in `origin/main..main` looks like a credential.

Tracked-file checks (all clean):

| check | command | result |
|-------|---------|--------|
| Private keys / keystores / certs tracked | `git ls-files \| grep -Ei '\.(pem\|jks\|keystore\|p12\|pfx\|key)$'` | **(none)** |
| `google-services.json` / `local.properties` / `.pass` tracked | `git ls-files \| grep -Ei 'google-services\|local\.properties\|\.pass$'` | **(none)** |
| Keystore filename refs tracked | `git ls-files \| grep -i keystore` | **(none tracked)** |
| Built APK/AAB/dex/class tracked | `git ls-files \| grep -Ei '\.(apk\|aab\|dex\|class)$'` | **(none tracked)** |
| Binary `.epub` tracked | `git ls-files \| grep -i '\.epub$'` filtered to actual binaries | **(none)** — hits are only `Epub*.kt`, `epub-*/…`, `*.md`, `*.xml`, `*.png`, `*.txt` path substrings |

## 3. Large-file / binary table — what the push would carry

Scope: files changed in `origin/main..main` (95 files), sizes from working tree. Working-tree sum of all changed files: **15,667,685 bytes (~14.94 MiB)**.

### 3a. Files > 1 MiB (strict) — exactly two

| size | file | keep / prune |
|------|------|--------------|
| 4,155,832 bytes (3.96 MiB) | `core/designsystem/src/main/res/font/google_sans_flex.ttf` | **KEEP (confirm intentional)** — variable font backing the Expressive design system. Pruning breaks typography. Flag: 4 MB in git history is permanent bloat; consider font subsetting or hosted-font follow-up later — not as part of this push. |
| 1,381,048 bytes (1.32 MiB) | `docs/device-test/real-epub-reader-retry-02-detail.png` | **KEEP** — t24 real-EPUB reader device evidence (detail screenshot). Largest of the new evidence PNGs; single file over the 1 MiB line is documentation, not app payload. |

### 3b. Further PNGs in the push (all < 1 MiB each)

27 further PNGs under `docs/device-test/` (t5 smoke-test evidence + Mori baselines + t21/t24 real-EPUB library/reader shots). None individually exceeds 1 MiB:

| size | file | keep / prune |
|------|------|--------------|
| 682,215 bytes (666 KiB) | `docs/device-test/real-epub-reader-retry-10-library-after.png` | KEEP — reader-retry evidence |
| 599,547 bytes (585 KiB) | `docs/device-test/real-epub-lib-01-library.png` | KEEP — library-load evidence |
| 598,619 bytes (584 KiB) | `docs/device-test/real-epub-reader-retry-09-progress.png` | KEEP — reader-retry evidence |
| 516,116 bytes (504 KiB) | `docs/device-test/real-epub-reader-retry-01-library.png` | KEEP — reader-retry evidence |
| 487,560 bytes (476 KiB) | `docs/device-test/iridium-07-reader-chrome.png` | KEEP — smoke-test evidence |
| 448,739 bytes (438 KiB) | `docs/device-test/real-epub-reader-retry-05-chrome.png` | KEEP — reader-retry evidence |
| 421,424 bytes (411 KiB) | `docs/device-test/iridium-06-reader-chapter.png` | KEEP — smoke-test evidence |
| 411,043 bytes (401 KiB) | `docs/device-test/iridium-05-reader-malformed-epub.png` | KEEP — smoke-test evidence |
| 396,919 bytes (387 KiB) | `docs/device-test/real-epub-reader-retry-03-reader.png` | KEEP — reader-retry evidence |
| 361,199 bytes (352 KiB) | `docs/device-test/iridium-09-reader-toc.png` | KEEP — smoke-test evidence |
| 285,144 bytes (278 KiB) | `docs/device-test/iridium-08-reader-settings-sheet.png` | KEEP — smoke-test evidence |
| 178,575 bytes (174 KiB) | `docs/device-test/real-epub-reader-retry-08-toc.png` | KEEP — reader-retry evidence |
| 178,492 bytes (174 KiB) | `docs/device-test/mori-02-settings.png` | KEEP — Mori baseline reference |
| 176,793 bytes (172 KiB) | `docs/device-test/real-epub-reader-retry-06-chrome.png` | KEEP — reader-retry evidence |
| 174,506 bytes (170 KiB) | `docs/device-test/iridium-12-settings-appearance.png` | KEEP — smoke-test evidence |
| 169,655 bytes (165 KiB) | `docs/device-test/iridium-13-settings-reading.png` | KEEP — smoke-test evidence |
| 157,051 bytes (153 KiB) | `docs/device-test/real-epub-reader-retry-11-history.png` | KEEP — reader-retry evidence |
| 152,575 bytes (149 KiB) | `docs/device-test/real-epub-reader-retry-04-paging.png` | KEEP — reader-retry evidence |
| 151,167 bytes (147 KiB) | `docs/device-test/mori-01-library.png` | KEEP — Mori baseline reference |
| 147,884 bytes (144 KiB) | `docs/device-test/mori-03-stats.png` | KEEP — Mori baseline reference |
| 138,930 bytes (135 KiB) | `docs/device-test/real-epub-lib-02-crash.png` | KEEP — library-load evidence |
| 131,685 bytes (128 KiB) | `docs/device-test/real-epub-reader-retry-07-settings.png` | KEEP — reader-retry evidence |
| 111,180 bytes (108 KiB) | `docs/device-test/iridium-02-onboarding-folder.png` | KEEP — smoke-test evidence |
| 90,396 bytes (88 KiB) | `docs/device-test/iridium-01-onboarding-welcome.png` | KEEP — smoke-test evidence |
| 85,968 bytes (83 KiB) | `docs/device-test/iridium-10-detail-progress.png` | KEEP — smoke-test evidence |
| 85,852 bytes (83 KiB) | `docs/device-test/iridium-04-detail-screen.png` | KEEP — smoke-test evidence |
| 76,138 bytes (74 KiB) | `docs/device-test/iridium-11-history-with-book.png` | KEEP — smoke-test evidence |
| 72,457 bytes (70 KiB) | `docs/device-test/iridium-03-library-loaded.png` | KEEP — smoke-test evidence |

Recommendation: **keep all 29 PNGs + the font** — they are t5/t21/t24 device-test attachments and Mori baselines. Optional future saving (not this task): recompress PNGs; saves are marginal (~10–20%) and would rewrite evidence files, so don't.

### 3c. Everything else in the push (for completeness)

All remaining changed files are small text sources (Kotlin, markdown, XML dumps, toml). No jars/aars/so binaries added. Pre-existing tracked binaries (`mipmap-*` launcher icons, `gradle-wrapper.jar`) are untouched by this range.

## 4. `.gitignore` coverage

`.gitignore` (verbatim relevant lines):

```
.gradle/
build/
local.properties
.idea/
.vscode/
*.iml
*.hprof
*.log
.DS_Store
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.ap_
*.dex
*.class
.kotlin/
out/
# Release signing material must never be committed.
*.jks
*.keystore
/store.pass
/key.pass
# Design-reference drops at the repo root (screenshots, mockups).
/*.png
/*.jpg
/*.jpeg
/*.avif
```

| category | covered | note |
|----------|---------|------|
| `*.jks`, `*.keystore`, `/store.pass`, `/key.pass` | yes | signing material blocked; verified zero tracked |
| `local.properties` | yes | verified zero tracked |
| `*.apk`, `*.aab`, `*.dex`, `*.class`, `build/`, `.gradle/` | yes | verified zero built artifacts tracked |
| `/*.png`, `/*.jpg`, `/*.jpeg`, `/*.avif` (root-level only) | partial | covers stray root drops only — `docs/device-test/*.png` evidence PNGs are intentionally tracked and unaffected |
| `*.epub` | **NO** | `git check-ignore -- pg64317-images-3.epub` exits 1 (not ignored). The book stays out of git purely by never being `git add`ed — see §5 |

## 5. Untracked check — `pg64317-images-3.epub` stays out

| check | command | result |
|-------|---------|--------|
| Working-tree status | `git status --porcelain=v1 --untracked-files=all` | **clean (empty)** — no untracked files in this worktree |
| Binary `.epub` on disk | `find . -name "*.epub" -not -path "./.git/*"` | **(none)** — the book file is absent from this worktree |
| Binary `.epub` tracked | `git ls-files` filtered | **(none)** — only `Epub*.kt` / `epub-*` / docs path substrings match |
| Ignore rule for the book | `git check-ignore -v -- pg64317-images-3.epub` | exit 1 — **not ignored** (no `*.epub` rule in `.gitignore`) |

Confirmed: **`pg64317-images-3.epub` is untracked and absent from the push.** Caution carried forward from the plan: because no ignore rule covers `*.epub`, a careless `git add .` from a directory containing the book would stage it. If the book must live next to the repo, add an explicit `*.epub` (or the exact filename) ignore rule before that — not done here (this task writes one docs file only).

## 6. Bottom line

1. **Remote:** push goes to `agentcraft-push-blocked://…` — push cannot succeed as configured. This is the intended safety gate.
2. **Secrets:** scan clean; no `.pem`/`.jks`/`.keystore`/`google-services.json`/`local.properties`/`.pass`/binary-`.epub` tracked; only CI secret *names* referenced in `release.yml` plus benign `password="false"` XML attributes.
3. **Binaries:** two files > 1 MiB (4.0 MiB font — intentional design-system asset; 1.3 MiB reader-retry screenshot — intentional evidence) + 27 further evidence PNGs + small text sources ≈ 14.94 MiB working-tree sum. All look intentional; recommend **keep**, with optional font-subsetting noted as future work.
4. **Untracked:** `pg64317-images-3.epub` stays untracked/absent; it is NOT ignore-covered, so keep it out of `git add` scope.
5. **DO NOT PUSH without Priyanshu approval.** This task performed zero pushes, zero merges, zero remote changes — audit only. Handing to t26 (preview) + t27 (gate) for the remaining verdicts before Priyanshu decides.
