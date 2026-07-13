# PolyglotAI — session handoff

> Transient handoff doc for picking up work in a new Claude Code window. Safe to delete once
> absorbed — not meant to live in the repo long-term.

## What this is

A multi-language learning app (Tauri + React desktop, optional Supabase cloud sync). Started as a
Brazilian Portuguese app, pivoted mid-project into a genuine multi-language platform: one account,
multiple per-language profiles, each with its own FSRS spaced-repetition queue, content, and a
**distinct visual identity** (not a reskin — a different design language per pack).

## Where everything lives

- **Canonical remote**: `https://github.com/alekpeed/polyglotai`, branch `main`. Direct commits to
  `main`, no PR flow.
- **Ignore `alekpeed/lifeos` entirely.** Stale, unrelated copy of this project's early history.
  Nothing at risk. The branch itself may still need manual deletion (no tool for it, low priority,
  purely cosmetic — tracked below): `https://github.com/alekpeed/lifeos/branches/all` → find the
  branch → delete.
- **CI**: GitHub Actions. Runs typecheck/test + a pack-validation loop (`for pack in packs/*/`,
  see below) + Rust migration tests + desktop builds (mac/win/ubuntu) on every push to `main`.
- **Git identity**: commits this session were authored as `Claude <noreply@anthropic.com>` (the
  shared Claude Code Remote bot identity) and show as "Unverified" on GitHub — this is expected;
  the signing key for that shared identity isn't something this environment controls. **The user
  has said they want to switch to committing under their own GitHub account/identity at some
  point** (tracked below) but explicitly does not want this brought up proactively — don't mention
  it unless they do.

## Architecture, current state

- **Monorepo**: `packages/` (shared-types, language-pack-sdk, core-learning, ai-orchestration,
  pronunciation, spaced-repetition) + `apps/desktop-tauri` + `packs/` (content packs live outside
  the code, loaded via a Vite glob).
- **Three full language packs**, each top-level (`basePack: null`):
  - `packs/pt-br` — Tier-1 complete (all ten §10.1 content categories at/above target), plus a
    2026-dated web-researched internet-slang refresh (`slang/2026-internet.json`).
  - `packs/ja` — Tier-1 complete (grew from a 60-vocab pilot across this session: 320 vocab, 75
    phrases, 25 grammar, 20 pronunciation, 27 dialogues, 21 idioms, 10 listening/10 writing/10
    assessment/5 roleplay lessons). Also now has slang (`slang/general.json`, 15 current
    net-slang/wakamono-kotoba terms) and profanity (`profanity/general.json`, 12 items severity
    2-7) — **this reverses an earlier documented decision** that Japanese's register axis is
    politeness, not a slang ramp; the owner explicitly overrode it. The reasoning and the reversal
    are both on record in `packs/ja/NATIVE_REVIEW.md`.
  - `packs/ru` — Russian, pilot scale (71 vocab, 9 grammar, 9 pronunciation rules, 3 dialogues,
    A1). New this session — first non-ja/pt-br language, and the first real test of the "third
    language" case. Built around an explicit, hard product requirement: **every vocab/dialogue/
    slang item shows Cyrillic AND a stress-marked Latin transliteration together** (the `romaji`
    field — generically named from its Japanese origin, not Japanese-specific). Pronunciation
    content is deliberately alphabet-literacy-first (false-friend Latin-lookalike letters like
    В/Н/Р/С/У/Х, hard/soft signs, akanye vowel reduction, unmarked word stress).
- **21 micro-packs** (all `basePack` set to a full-language pack's id, so they inherit
  grammar/pronunciation and only author what's new to their theme; all group under their parent
  language in the picker, never as their own tile — see UI section below):
  - Japanese (7): `ja-goshuin` (shrine stamps, the original pilot), `ja-onsen`, `ja-izakaya`,
    `ja-konbini`, `ja-matsuri`, `ja-keigo` (business formal speech — needed genuinely new grammar
    content, the exception to "micro-packs add zero new grammar"), `ja-yakuza` (media/fiction
    tough-guy register, built around **役割語/yakuwarigo** — the real linguistics term for
    stylized fictional-character speech; framed explicitly throughout as recognize-only, not
    real-life vocabulary — also needed new grammar content for its register shift).
  - Brazilian Portuguese (5): `pt-br-carnaval`, `pt-br-futebol`, `pt-br-churrasco`, `pt-br-praia`,
    `pt-br-feira`.
  - Russian (9, all new this session): `ru-banya`, `ru-dacha`, `ru-produkty` (grocery/rynok),
    `ru-novy-god` (New Year — Russia's actual biggest holiday), `ru-metro` (metro + marshrutka),
    `ru-tosty` (toasting culture/tamada), `ru-primety` (superstitions), `ru-svadba` (wedding
    customs), `ru-mat` (the Russian profanity system — grammatically productive, mirrors
    `ja-yakuza`'s approach of teaching the *system* not just a word list; severity-calibrated,
    every profanity example explicitly tagged "for recognition only," no hateful slurs — scoped
    strictly to mat, a distinct linguistic phenomenon).
- **Library UI, two real feature additions this session**:
  1. **Micro-packs live inside their language, not in the top-level picker.** The "which
     language?" screen (`LanguagePicker.tsx`) now shows only full languages. Every micro-pack is
     reachable via a "More from `<language>`" tab inside `Library.tsx`, with Start (new profile)
     or Continue (existing profile) per sibling pack. This was a real UX correction mid-session —
     micro-packs used to show as indented-but-still-separate tiles in the same picker list, which
     read as clutter (a micro-pack isn't a language).
  2. **Vocabulary and Culture tabs group by topic tag.** Every vocab/culture item already carried
     a `tags` array that never reached the UI (the DB column `tags_json` was written on import but
     never selected back out) — fixed at the query layer (`listVocabulary` in
     `packages/core-learning/src/services/content.ts`), and Library now renders both tabs as
     collapsible topic sections (e.g. "Food & Drink", "Conversation") instead of one long flat
     list. Grammar/dialogues/slang don't have a `tags` field in their schemas, so they're
     unaffected — would need a schema change + backfill to extend this there.
- **Schema fix**: `RealSpeechItem` (slang/profanity/idiom) never had `reading`/`romaji` fields —
  pt-br never needed them (Latin-script). Adding Japanese/Russian slang & profanity needed it, so
  it got the same treatment `VocabularyItem` got back in migration 0005: new fields in
  `packages/shared-types`, migration `0006_real_speech_reading.sql`, threaded through the
  importer/query-layer/review-card/Library UI. **Three separate places hardcode their own
  migration-file list** (Rust `lib.rs`, the Node test harness `nodeSqliteDb.ts`, and the
  browser-preview sql.js shim `tauriSqlPluginShim.ts`) — all three needed the same one-line
  addition this time; a real duplication worth collapsing at some point, flagged but not fixed.
- **CI pack-validation** now loops over `packs/*/` automatically (`.github/workflows/ci.yml`)
  instead of a hardcoded per-pack list — it was already the wrong shape at 3 packs; now there are
  24. Any future pack (full language or micro) is validated with zero CI changes required.
- **Design system**: `apps/desktop-tauri/src/App.css`, CSS custom properties on `:root`, two
  independent axes — `data-theme` (light/dark/classic) and `data-pack` (per-language palette +
  structural overrides). pt-br = "Concreto & Ipê". Japanese = "Ai to Sumi" (藍と墨). Russian and
  every micro-pack currently use the default/inherited look — no custom design language built for
  them yet (a real opportunity if the user wants to design one; confirmed the architecture
  supports it with zero code changes, just a new `[data-pack="x"]` CSS block).
- **Cloud accounts**: Supabase-backed, optional — local-only mode works without signing in.

## Outstanding / not yet done

- **Git identity switch** — user wants to eventually commit under their own GitHub account instead
  of the shared bot identity, to get the Verified badge and control their own signing key. Do NOT
  raise this proactively; user has explicitly asked not to hear about it unless they bring it up.
  Must not be forgotten before the project is considered fully wrapped, per the user.
- **lifeos branch deletion** — manual, link above, low priority, purely cosmetic.
- **Native-speaker review** — everything content-wise in this handoff is "authored by Claude,
  pending native review" (the established pattern for every pack — see each pack's own
  `NATIVE_REVIEW.md`). Highest-priority items specifically: `ru-mat`'s severity calibration,
  `アホ`'s claimed Kansai-vs-elsewhere register flip in `packs/ja/profanity/general.json`, and
  general staleness risk on both the pt-br and ja "current slang" batches (web-researched,
  meme-adjacent content that may already be dated by the time anyone reviews it).
- **`ru` is still pilot scale** — the natural next step, mirroring what `ja` went through this
  session (backbone expansion → dialogues → idioms → lessons/assessments, each a discrete pass).
- **Extending topic-grouping** to Grammar/Dialogues/Slang tabs — would need a `tags` field added
  to those content schemas and backfilled across every existing pack; bigger than the
  vocab/culture version that shipped.
- **User's own UI/UX overhaul work** — they've said they're designing overhauls themselves;
  architecture is confirmed to support a new per-pack design language cheaply (one CSS block, no
  code changes). No specific pack/direction chosen yet as of this handoff.

## Ideas discussed, not built (moonshots / parked)

- **Country-flavored pt-br/English variants (Brazil/Portugal/Angola/Mozambique, etc.)** —
  explicitly ruled out by the user: thin demand for those variants specifically, not their actual
  audience. Not a "someday" — a dropped idea, don't resurface without new context.
- **A "clone this app for X→English" product** — discussed as a separate, cheaper alternative to
  building one fully general multi-directional (any L1×L2) platform. Tabled, not scoped. If
  picked up again: the pack *content* schema needs no changes (author with English as the target,
  X as the explanation language); the real work is one round of app-shell UI translation into X.
- **More Russian micro-packs** beyond the 9 already built — the brainstormed list was exhausted
  by "build 'em all"; if more are wanted, would need a fresh round of ideation.

## Model note

No special model requirement surfaced this session — Sonnet handled design-system work, schema/
migration changes, large-scale parallel content authoring (via background subagents), and web
research equally well. Opus was used briefly at the user's explicit request, not because Sonnet
was insufficient.
