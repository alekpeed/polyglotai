# PolyglotAI

An adult-focused, real-world language learning app: AI tutor, spaced repetition, and
slang/register-aware language packs that teach how a language is actually spoken — including
profanity and taboo speech, labeled with register and severity so the learner gains judgment,
not just vocabulary.

Windows-first desktop MVP built with Tauri + React + SQLite; see
[`docs/mvp-implementation-plan.md`](docs/mvp-implementation-plan.md) for the full implementation
plan.

```text
apps/desktop-tauri/   Tauri + React desktop app (primary MVP target: Windows)
apps/android/         placeholder — Phase 6, reuses packages/*
apps/web/             placeholder — optional/future
packages/             platform-free TS libraries shared across apps
packs/pt-br/          the first language pack (data, not code)
docs/                 implementation plan and other design docs
```

## Getting started

```sh
pnpm install
pnpm -r typecheck
pnpm -r test
pnpm dev:desktop   # runs the Tauri app locally
```

Requires Node.js, [pnpm](https://pnpm.io), and [Rust](https://rustup.rs) (for the Tauri
desktop shell) installed locally.
