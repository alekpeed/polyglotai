# ja pack — native-speaker review notes

This pack was authored by Claude as a pilot (per the same "Claude authors, owner reviews"
pattern used for pt-br). It passes the automated validator (schema + cross-references), but
nothing here has had a native Japanese speaker's sign-off yet. None of the notes below block
the app from running — they're accuracy/naturalness checkpoints before treating this as more
than a pilot.

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

## Still deliberately not built (scope for a follow-up session)

- **slangRegister** (target 20) is intentionally empty for now; the agreed approach is to fill
  it via the schema's `idiom` kind (慣用句 / set expressions with register notes), honoring the
  "no crude slang / no keigo severity ramp" stance below — not yet authored.
- **Lesson-based categories** — listening exercises, writing prompts, assessments, and roleplay
  scenarios (all target >0) are not yet built.

## Scope decisions (need an explicit owner call)

- **No formality/register axis yet.** pt-br models slang→profanity as a severity ramp;
  Japanese's real equivalent is politeness (tameguchi/casual → teineigo → keigo), a completely
  different axis. This pack deliberately ships plain teineigo (です/ます) only. Building the
  real formality model is future work, not something reused from pt-br's severity scale.
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

## Not included by design

- Kanji stroke order / handwriting practice — out of scope for this pilot.
- Keigo (formal/humble speech) — see scope decision above.
- Regional dialects (Kansai-ben, etc.) — pt-br modeled SP vs. RJ; a Japanese equivalent would
  be a deliberate follow-up, not an oversight.
