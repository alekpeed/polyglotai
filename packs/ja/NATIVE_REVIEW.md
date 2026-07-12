# ja pack — native-speaker review notes

This pack was authored by Claude (per the same "Claude authors, owner reviews" pattern used for
pt-br), growing from a 60-vocab pilot to a full §10.1 Tier-1 pack across several sessions. It
passes the automated validator (schema + cross-references) and now meets or exceeds every
Tier-1 volume target. Nothing here has had a native Japanese speaker's sign-off yet — none of
the notes below block the app from running, they're accuracy/naturalness checkpoints before
treating this pack as production-ready rather than "complete but unreviewed."

## A1–A2 backbone expansion (volume upgrade)

The pack was expanded from the original 60-vocab pilot to an A1–A2 backbone that meets the
§10.1 Tier-1 targets for vocabulary (320), phrases (75), grammar (25), and pronunciation (20).
The new content lives in `vocabulary/a1-extended.json`, `vocabulary/a2.json`,
`phrases/core.json`, `grammar/a2.json`, and `pronunciation/extended.json`. It was authored
directly in one pass and is the highest-priority target for native review — roughly 5× the
prior surface area. Specific things worth a careful native check:

- **Transitive/intransitive verb pairs** (始める/始まる, 開ける/開く, 閉める/閉まる, 止まる…):
  the pairing and the が/を marking in the examples should be gut-checked.
- **Particle choices in the A2 grammar** (`grammar/a2.json`): が vs を with できる/potential,
  より vs のほうが ordering in comparatives, plain-form-before-と in ～と思います, and the
  て-form derivations in the ladder.
- **Example-sentence naturalness at volume** — 320 vocab + 75 phrases means many short example
  sentences; a native pass would catch any that read textbook-stiff or pick an odd register.
- **naturalMeaning usage notes** — many entries carry a one-line usage/nuance note (homophones,
  pitch-accent pairs, casual-vs-polite forms, rendaku). These are the most error-prone content
  and deserve the closest read.
- **Pronunciation additions** (`pronunciation/extended.json`): the mora-timing, moraic-ん
  assimilation, and particle-sound (は→wa, へ→e, を→o) rules are simplifications; confirm the
  framing and the minimal pairs.
- **CEFR labels on the A2 set** — items were tagged A1 vs A2 by feel, not against a formal
  syllabus; the boundary is approximate and worth a sanity pass.

## Dialogue expansion (3 → 27)

Added `dialogues/everyday.json` with 24 new scenarios (target was 25; total is now 27) —
restaurant/konbini/hotel/train/bank/post-office/doctor visits, small talk (hobbies, family,
weather, work), social situations (declining an invitation, apologizing for lateness, borrowing
something, asking a teacher for help), and a phone reservation. All `keyVocabulary` references
resolve against the expanded vocab/phrase set. Worth a native check on:

- **Service-register phrases** (かしこまりました, ご利用ですか, お待ちしております, etc.) —
  these lean into keigo-adjacent staff speech without the pack modeling keigo formally yet; a
  native speaker should confirm the register reads as natural, situational staff speech rather
  than something that needs to be "taught" as a rule.
- **Dialogue-specific grammar notes** — several introduce a pattern not covered by a dedicated
  grammar item (見に行く/食べに行く, 動詞＋てみる, もらえますか, されています, ので vs から) —
  worth confirming the one-line explanations hold up and whether any deserve their own
  full grammar entry in a future pass.

## slangRegister filled via idioms (0 → 21)

Added `idioms/core.json`: 21 `RealSpeechItem`s of `kind: "idiom"` — everyday 慣用句 (body-part
idioms like 頭がいい, 気にする, 目がない), yojijukugo (一石二鳥), and a few ことわざ/proverbs
(猿も木から落ちる, 郷に入っては郷に従え). All are `register: "neutral"` or `"informal"` and
`severity: 1` — deliberately no slang, profanity, or severity ramp, per the scope decision
below. `learnerShouldUse` is split between `"use"` (safe, common, low-risk to produce) and
`"recognize-only"` (idioms that are fine to understand but where a learner producing them
might sound unnatural or overly bookish coming from a non-native speaker) — that judgment call
is itself worth a native sanity check, since it's more subjective than the schema/grammar
content.

## Lesson-based categories (0 → full target on all four)

Added `lessons/a1.json` (10 listening exercises + 10 writing prompts + 5 roleplay scenarios,
25 total) and `assessments/placement.json` (10 assessments), following the same pattern as
pt-br's `lessons/a1.json` / `assessments/placement.json`. Listening and roleplay lessons use
`dialogueRef` against the existing dialogue set; writing prompts and assessments are
self-contained. All ten §10.1 content categories now meet or exceed their Tier-1 target — this
completes the pack's volume buildout. Worth a native check on:

- **Assessment answer keys** — several assessments (te-form, potential, comparatives,
  suggestions/opinions) have a single "correct" answer given, but Japanese often allows more
  than one valid phrasing; worth confirming the given answers aren't presented as more rigid
  than they should be.
- **Listening-exercise comprehension questions** — written from the dialogue transcripts
  directly rather than by actually listening to audio (no audio exists yet), so they test
  reading comprehension of the transcript, not real listening skill; that's a known and
  accepted limitation of this content type until audio generation exists.
- **Writing-prompt example answers** — marked with "e.g." throughout since these are meant as
  one valid model answer, not the only correct one; worth confirming none of the model answers
  contain unnatural phrasing at the volume this was authored.

## Slang/profanity added — reverses an earlier documented decision (0 → 27)

**This section is a direct reversal of a call this same file used to document.** The original
"Scope decisions" note below (kept for the historical record, now struck through in spirit if
not in markdown) argued that Japanese's real register axis is politeness/keigo rather than a
slang→profanity severity ramp, and that reusing pt-br's model as-is would be the wrong move.
That reasoning wasn't wrong on its own terms — the later web research done for this very
addition turned up the same observation independently (Japanese genuinely has fewer,
less-systematized insult words than English/Portuguese, precisely because social weight that
other languages carry in profanity gets carried by politeness-register violations instead). But
the owner explicitly and repeatedly requested slang and profanity content for Japanese anyway,
so `featureFlags.slang`/`featureFlags.profanity` are now `true` and:

- `slang/general.json` — 15 current net-slang / wakamono-kotoba items (草, やばい, ガチ, エモい,
  ぴえん, 無理, 推し, 尊い, チルい, 陽キャ/陰キャ, 詰んだ, 神, それな, シャバい,
  あざまる水産), researched the same way as the pt-br slang refresh (web search, not training
  knowledge alone) and carrying the same staleness risk that entails.
- `profanity/general.json` — 12 items spanning the real severity range: mild (バカ, アホ,
  ダサい), moderate (クソ, うざい, キモい, ちくしょう), and severe (てめえ, ふざけるな, くたばれ,
  死ね, ぶっ殺す). Severity 6-7 items are explicitly `learnerShouldUse: "avoid"` with warning
  notes — recognize-in-media vocabulary, not usable output, mirroring how pt-br treats its own
  most severe items.
- **This unlocked a schema gap**: `RealSpeechItem` never had `reading`/`romaji` fields (pt-br
  never needed them, being Latin-script) — added them to `packages/shared-types`, a new DB
  migration (`0006_real_speech_reading.sql`), and threaded them through the importer/query
  layer/Library UI, matching the exact pattern `vocabulary_items` got in migration 0005.

Highest native-review priorities on this addition specifically:
- **アホ's regional register flip** (Kansai vs. elsewhere) is asserted from research, not
  personal knowledge — exactly the kind of nuance that needs a Kansai speaker's confirmation.
- **Severity calibration generally** — Japanese insult severity is far more context/tone-driven
  than a fixed 1-7 scale can really capture (バカ said fondly vs. said cruelly is the same word);
  the scale here is a best-effort approximation, flagged as such.
- **死ね and ぶっ殺す's `avoid` framing** — worth confirming this reads as appropriately serious
  rather than either overstated or understated.
- Same staleness caveat as pt-br's 2026 slang batch: several terms are tied to specific recent
  cultural moments and may need periodic re-verification, not a one-time addition.

## Still deliberately not built

Nothing remains against the §10.1 Tier-1 targets. Everything below this point is genuinely
future/roadmap work, not a Tier-1 gap:
- Kanji stroke order / handwriting practice.
- Keigo (formal/humble speech) as its own modeled register axis (a dedicated `ja-keigo`
  micro-pack now covers this separately — see that pack's own docs).
- Regional dialects (Kansai-ben, etc.).
- Audio generation for dialogues/listening exercises.
- JLPT-aligned tagging alongside CEFR (see scope decision below).

## Scope decisions (need an explicit owner call)

- ~~**No formality/register axis yet.**~~ **Superseded — see "Slang/profanity added" above.**
  This bullet originally argued Japanese's real register axis is politeness/keigo rather than a
  slang→profanity severity ramp, and recommended not reusing pt-br's model. The owner overrode
  this and requested slang/profanity content anyway; kept here, struck through, so the reversal
  and its original reasoning are both on the record rather than silently overwritten.
- **CEFR labels, not JLPT.** Every item is tagged "A1" for consistency with the rest of the
  app's UI (severity gauges, level badges), even though Japanese learners more commonly think
  in JLPT terms (N5-N1). Worth an explicit decision on whether to add JLPT tags alongside CEFR.

## Content worth a native check

- `gram.wa-ga`: は vs が is one of the most argued-about points in Japanese pedagogy — the
  explanation given here (topic vs. subject, defaulting to は) is a common simplification, not
  the full picture. Worth a native speaker's gut check on whether the examples read naturally.
- `pron.pitch-accent`: only one minimal pair (箸/橋) is given, and Tokyo-standard pitch is
  assumed throughout — pitch accent varies by region (Kansai-ben inverts many patterns).
- `pron.g-nasal`: described as a Tokyo/standard tendency, not universal — flagged in the rule
  text itself, but worth confirming that framing doesn't overstate how common it still is
  among younger speakers.
- Example sentence naturalness generally: everything was written directly rather than
  translated from English, but a native pass would catch anything that reads as slightly
  textbook-stiff (e.g. dialogue register consistency, particle choices in edge cases).
