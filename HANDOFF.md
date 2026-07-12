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
- **Ignore `alekpeed/lifeos` entirely.** That repo has a stale, unrelated copy of this project's
  early history (was PR #1, branch `claude/mvp-implementation-plan-zkfw1d`) from before the
  project was deliberately migrated into its own `polyglotai` repo. Every commit from that branch
  already exists in `polyglotai`'s history — nothing is at risk there. The PR is closed; the
  branch itself may still need manual deletion (no tool for it, low priority, purely cosmetic):
  `https://github.com/alekpeed/lifeos/branches/all` → find the branch → delete.
- **CI**: GitHub Actions, green as of `7d932fa`. Runs typecheck/test/pack-validate + Rust migration
  tests + desktop builds (mac/win/ubuntu) on every push to `main`.

## Architecture, current state

- **Monorepo**: `packages/` (shared-types, language-pack-sdk, core-learning, ai-orchestration,
  pronunciation, spaced-repetition) + `apps/desktop-tauri` + `packs/` (content packs live outside
  the code, loaded via a Vite glob).
- **Two full language packs**: `packs/pt-br` (Tier-1 complete: 300 vocab/75 phrases/25 grammar/25
  dialogues/etc.) and `packs/ja` (pilot scale: 60 vocab/9 grammar/3 dialogues/7 pronunciation
  rules).
- **One micro-pack**: `packs/ja-goshuin` — a new concept, built this session. Small, situational,
  hyper-focused packs (a single interest area, not a whole language) that inherit
  grammar/pronunciation from a parent pack via a `basePack` field, so they only author what's
  actually new. Goshuin Seeker: 49 vocab/phrases, 7 dialogues, 7 culture notes, zero new grammar.
- **Design system**: `apps/desktop-tauri/src/App.css`, CSS custom properties on `:root`, two
  independent axes — `data-theme` (light/dark/classic) and `data-pack` (per-language palette +
  structural overrides). pt-br = "Concreto & Ipê" (periwinkle/gold/mint, Brazil-flag-derived).
  Japanese = "Ai to Sumi" (藍と墨 — flat washi/sumi/indigo/gold, vertical tategaki title, hanko
  seal, mincho typography via a bundled Noto Serif JP subset font, seigaiha background pattern
  with a `--ja-bg-image` slot left open for real artwork). Vermilion is intentionally parked, not
  deleted — may return later.
- **Cloud accounts**: Supabase-backed, optional — local-only mode works without signing in.
  Multi-profile schema (one account → many language profiles) is deployed.

## Most recent session's work (chronological)

1. **Ai to Sumi design overhaul** — implemented the full approved mockup across every screen
   (Onboarding, AppShell/sidebar, Dashboard, Library, Review, Drill, Tutor, Conversation,
   Interpreter, Pronunciation, Settings), scoped entirely under `[data-pack="ja"]`. Also fixed a
   hardcoded pt-br "Olá" greeting and hardcoded Portuguese strings in Tutor/Conversation that were
   leaking through regardless of active pack (caught via a user screenshot).
2. **Micro-pack architecture + Goshuin Seeker pack**:
   - Wired up `basePack` inheritance (the merge logic existed and was tested, but nothing ever
     called it — `loadPackForId` in `bootstrap.ts` now resolves it recursively).
   - Added a genuinely new `CultureNote` content type (freeform title+body+tags, rendered straight
     from the in-memory pack, not imported into the review DB since it's reference reading, not
     SRS content). Added a Culture tab to Library.
   - `[data-pack]` theming and the language picker both fall back to `basePack`, so a micro-pack
     auto-inherits its parent's look and groups under it in the picker ("Goshuin Seeker —
     Micro-pack · part of Japanese") instead of appearing as an unrelated tile.
   - Authored and shipped `packs/ja-goshuin`, verified end-to-end via Playwright.
3. **CI investigation** (false alarm) — confirmed `polyglotai` main is fully green; the CI failure
   the user saw was actually on the unrelated stale `lifeos` PR.
4. **lifeos cleanup** — closed PR #1, confirmed no data loss.

## Outstanding / not yet done

- **lifeos branch deletion** — manual, link above, low priority.
- **CI gap**: the pack-validation step in `.github/workflows/ci.yml` only validates `pt-br`
  (`pnpm --filter @polyglotai/language-pack-sdk run validate ../../packs/pt-br`). It doesn't
  validate `ja` or `ja-goshuin` — a bad edit to either would ship without CI catching it. Small
  fix: add two more `validate` lines. Flagged but never picked back up.
- **A "fun feature tour" doc** for the user's friend (all current functions + moonshot ideas,
  enthusiastic non-technical tone) was drafted but turned out to be meant for a different window,
  so it was parked in that session's scratchpad and won't exist in a fresh session. Regenerate on
  request if still wanted — it doesn't need repo context beyond what's in this handoff.

## Ideas discussed, not built (moonshots)

- More micro-packs: onsen/ryokan, izakaya/ramen ordering, konbini survival, matsuri, business
  keigo (Japan); carnaval, futebol, churrasco, praia, feira haggling (Brazil). Same pattern as
  goshuin — cheap now that the infra exists.
- Country-flavored packs per language (Portuguese → Brazil/Portugal/Angola/Mozambique; English →
  US/UK/Ghana/Australia). Same `basePack` mechanism, one level up. This was already documented as
  a roadmap idea in the old lifeos history, now doubly relevant since the infra is real.

## Model note

No special model requirement — Sonnet handled all of this fine (design overhaul, infra work,
content authoring). Opus was used briefly for a CI investigation at the user's request, not
because it was strictly needed.
