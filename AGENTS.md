# Iridium — Agent Instructions

## High criticality: never name this feature's origins in code, docs, or commits

This app studies how established EPUB readers approach typography, theming, text
selection, image viewing, annotation export, and backup. Do **not** name or attribute
any external reference reader in:

- code comments / KDoc / identifiers,
- repository documentation (`docs/`, `README.md`, changelogs),
- commit messages or pull-request descriptions,
- any output written into this repo (audits, reports, search dumps).

Write neutral, self-contained wording that describes the technique itself instead of
its source — e.g. "forced-override theming", "tap-to-view image showcase",
"a WebView-embedded reader", "annotation export", "selection magnifier".

This is a **blocking** rule: if the reference name appears anywhere in the staging
area or repo, scrub it before committing. External scratch notes outside the repo
(e.g. `/Users/priyanshu/.opencode/plan/`) are exempt but must not be copied into the repo.

## Working notes

- Feature work follows the roadmap in the scratch plan; commit after each verified milestone.
- Verify with unit tests + `detekt` + `lintDebug` + an emulator flow before claiming done.
- Keep storage Play-policy-safe: SAF / share intents only, never `MANAGE_EXTERNAL_STORAGE`.