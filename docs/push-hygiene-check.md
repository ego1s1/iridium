# Push hygiene check (t8) — secrets, binaries, remote

Date: 2026-10-06 · Worktree branch: `agentcraft/wren/t8-push-hygiene-secrets` (from `main`) · Range audited: `origin/main..main` (14 commits ahead)
Verdict: **DO NOT PUSH without explicit Priyanshu approval.** Push remote is intentionally blocked (see §1). Secrets scan is clean; payload is ~7.35 MiB of intentional binaries (one 4 MB font + 16 docs screenshots).

## 1. Remote table — push is blocked by design

`git remote -v` (verbatim):

| name   | direction | URL                                                        |
|--------|-----------|------------------------------------------------------------|
| origin | fetch     | `git@github.com:ego1s1/iridium.git`                        |
| origin | push      | `agentcraft-push-blocked:///git@github.com:ego1s1/iridium.git` |

- Push URL scheme `agentcraft-push-blocked://` is **not a real transport** — any `git push` to `origin` fails until Priyanshu unblocks/approves. Confirmed: **agentcraft-push-blocked push remote**.
- No other remotes configured. No push, fetch, pull, or remote-URL change was made during this task (read-only audit).
- Rule restated: **no push without Priyanshu approval. No merge without explicit Priyanshu approval either** (report-only task).

## 2. Secrets scan — clean (no live credentials in the push)

Patterns grepped (case-insensitive, excluding `.git/`, `build/`, `.gradle/`):

```
api[-_]?key, apikey, keystore[-_]?password, storePassword, keyPassword,
google-services, google_services, \.pem, \.jks,
BEGIN (RSA )?PRIVATE KEY, AKIA[0-9A-Z]{16}, AIza[0-9A-Za-z_-]{35}, xox[bap]-, sk-live-, secret[-_]?key
```

Result: **no live secrets found.** Only matches are CI *references* (names of GitHub Actions secrets, never values):

- `.github/workflows/release.yml:96` — `secrets.KEYSTORE_BASE64 || vars.KEYSTORE_BASE64`
- `.github/workflows/release.yml:98` — `secrets.KEY_PASSWORD || vars.KEY_PASSWORD`
- `.github/workflows/release.yml:103,105` — `::add-mask::`, `base64 -d > release.keystore` (masked at runtime)
- `.github/workflows/release.yml:89,110,113,120` — `ci-keystore.jks` filename references (file itself untracked, gitignored via `*.jks`)

Tracked-file checks (all clean):

| check | command | result |
|-------|---------|--------|
| Private keys / keystores tracked | `git ls-files \| grep -iE '\.pem$\|\.jks$\|\.keystore$\|google-services.*\.json$'` | **(none)** |
| `google-services.json` / `local.properties` / `.pass` tracked | `git ls-files \| grep -iE 'google-services\|local\.properties\|keystore\|\.pass$'` | **(none tracked)** |
| `.gitignore` covers | `*.jks`, keystore/pass entries | yes (`*.jks` line 21) |

Caveat: grep is a heuristic, not a guarantee — a rotated-key review before the eventual push is still recommended, but nothing in `origin/main..main` looks like a credential.

## 3. Large-file / binary table — what the push would carry

Scope: files changed in `origin/main..main`, sizes from working tree. Working-tree sum of all changed files: **7,709,551 bytes (~7.35 MiB)**.

### 3a. Files > 1 MiB (strict) — exactly one

| size | file | keep / prune |
|------|------|--------------|
| 4,155,832 bytes (4.0 MiB) | `core/designsystem/src/main/res/font/google_sans_flex.ttf` | **KEEP (confirm intentional)** — variable font backing the Expressive design system; referenced by new `AppFonts.kt`. Pruning would break typography. Flag: 4 MB in git history is permanent bloat; if size matters later, consider font subsetting or a CDN/hosted-font follow-up — but not as part of this push. |

### 3b. Screenshots / PNGs in the push (all < 1 MiB each, ~2.9 MiB combined)

16 new PNGs under `docs/device-test/` (smoke-test evidence from t5). None individually exceeds 1 MiB, but listed here per acceptance criteria:

| size | file | keep / prune |
|------|------|--------------|
| 487,560 bytes (476 KiB) | `docs/device-test/iridium-07-reader-chrome.png` | **KEEP** — smoke-test evidence |
| 421,424 bytes (411 KiB) | `docs/device-test/iridium-06-reader-chapter.png` | **KEEP** — smoke-test evidence |
| 411,043 bytes (401 KiB) | `docs/device-test/iridium-05-reader-malformed-epub.png` | **KEEP** — smoke-test evidence |
| 361,199 bytes (352 KiB) | `docs/device-test/iridium-09-reader-toc.png` | **KEEP** — smoke-test evidence |
| 285,144 bytes (278 KiB) | `docs/device-test/iridium-08-reader-settings-sheet.png` | **KEEP** — smoke-test evidence |
| 178,492 bytes (174 KiB) | `docs/device-test/mori-02-settings.png` | **KEEP** — Mori baseline reference |
| 174,506 bytes (170 KiB) | `docs/device-test/iridium-12-settings-appearance.png` | **KEEP** — smoke-test evidence |
| 169,655 bytes (165 KiB) | `docs/device-test/iridium-13-settings-reading.png` | **KEEP** — smoke-test evidence |
| 151,167 bytes (147 KiB) | `docs/device-test/mori-01-library.png` | **KEEP** — Mori baseline reference |
| 147,884 bytes (144 KiB) | `docs/device-test/mori-03-stats.png` | **KEEP** — Mori baseline reference |
| 111,180 bytes (108 KiB) | `docs/device-test/iridium-02-onboarding-folder.png` | **KEEP** — smoke-test evidence |
| 90,396 bytes (88 KiB) | `docs/device-test/iridium-01-onboarding-welcome.png` | **KEEP** — smoke-test evidence |
| 85,968 bytes (83 KiB) | `docs/device-test/iridium-10-detail-progress.png` | **KEEP** — smoke-test evidence |
| 85,852 bytes (83 KiB) | `docs/device-test/iridium-04-detail-screen.png` | **KEEP** — smoke-test evidence |
| 76,138 bytes (74 KiB) | `docs/device-test/iridium-11-history-with-book.png` | **KEEP** — smoke-test evidence |
| 72,457 bytes (70 KiB) | `docs/device-test/iridium-03-library-loaded.png` | **KEEP** — smoke-test evidence |

Combined PNG payload: ~3,338,566 bytes (~3.18 MiB). Recommendation: **keep all 16** — they are the t5 device-test report attachments and Mori baselines. Optional future saving (not this task): recompress PNGs; saves are marginal (~10–20%) and would rewrite evidence files, so don't.

### 3c. Everything else in the push (for completeness)

All remaining changed files are small text sources (Kotlin, markdown, toml — largest is `ReaderChrome.kt` at 23,045 bytes). No jars/aars/so binaries added. Pre-existing tracked binaries (`mipmap-*` launcher icons, `gradle-wrapper.jar`) are untouched by this range.

## 4. Bottom line

1. **Remote:** push goes to `agentcraft-push-blocked://…` — push cannot succeed as configured. This is the intended safety gate.
2. **Secrets:** scan clean; no `.pem`/`.jks`/`google-services.json`/`local.properties` tracked; only CI secret *names* referenced in `release.yml`.
3. **Binaries:** one 4.0 MiB font (`google_sans_flex.ttf`) + 16 PNGs (~3.18 MiB) + small text sources = ~7.35 MiB total. All look intentional; recommend **keep**, with optional font-subsetting noted as future work.
4. **No push without Priyanshu approval.** This task performed zero pushes, zero merges, zero remote changes — audit only. Handing to t6 (preview) + t7 (gate) for the remaining verdicts before Priyanshu decides.
