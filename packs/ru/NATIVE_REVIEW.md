# ru pack — native-speaker review notes

This pack was authored by Claude as a pilot (per the same "Claude authors, owner reviews"
pattern used for pt-br and ja). It passes the automated validator (schema + cross-references),
but nothing here has had a native Russian speaker's sign-off yet. None of the notes below block
the app from running — they're accuracy/naturalness checkpoints before treating this as more
than a pilot.

## Scope decisions (need an explicit owner call)

- **Case coverage is intentionally partial.** Russian has six grammatical cases; this pilot
  covers only the prepositional (location) and genitive-after-negation (нет + genitive), plus a
  conceptual overview of all six. Dative, accusative, instrumental, and the rest of genitive's
  uses (possession, quantities) are follow-up work, not an oversight — trying to cover all six
  cases in a pilot would have meant a much larger, less carefully-checked first pass.
- **Stress marks in romaji are an authoring convention, not standard orthography.** Real
  Russian text never marks stress; this pack's `romaji` field adds an acute accent (´) on the
  stressed vowel in every transliteration, since so much of the pronunciation content (akanye,
  vowel reduction) is meaningless without knowing which syllable is stressed. Worth an explicit
  decision on whether that convention should continue at Tier-1 scale, where it's a lot more
  accents to place correctly and to keep consistent.
- **Aspect (perfective/imperfective) isn't covered yet.** Russian verbs come in aspect pairs
  (e.g. писать/написать, both "to write") that are arguably the single hardest concept for an
  English speaker learning Russian — deliberately deferred rather than introduced at A1 alongside
  the case system and two conjugation patterns.
- **CEFR labels, not a Russian-specific proficiency scale.** Every item is tagged "A1" for
  consistency with the rest of the app, matching the same choice already made for ja (CEFR over
  JLPT) — no Russian-specific equivalent scale is in wide enough use to warrant a parallel tag.

## Content worth a native check

- **Word stress placement** in every `romaji` field — stress is the single easiest thing for a
  non-native author to get subtly wrong, and it cascades into vowel-reduction pronunciation for
  the whole word. This is the single highest-priority thing to check in this pack.
- **Transliteration consistency** — the pack uses a simplified, learner-facing romanization
  (not a strict scientific transliteration standard), e.g. х→kh, ц→ts, щ→shch, ж→zh. Worth
  confirming this reads naturally to an English-speaking learner and stays consistent across all
  four content files.
- `gram.case-system-overview` and the two case-specific grammar items (`gram.prepositional-case`,
  `gram.genitive-negation`): case explanations are one of the most-debated areas of Russian
  pedagogy for simplification vs. accuracy trade-offs — worth a native/instructor gut check.
- `pron.gogo-genitive`: flagged in the rule itself as a specific historical exception rather than
  a general pattern — worth confirming the framing doesn't oversimplify.
- Example sentence naturalness generally: everything was written directly rather than translated
  from English, but a native pass would catch anything that reads as textbook-stiff.

## Not included by design

- Cursive/handwritten Cyrillic forms (printed Cyrillic can differ notably from handwriting,
  e.g. т, д, and п) — out of scope for this pilot; a legitimate follow-up if handwriting
  recognition or copy-practice content is ever built.
- Aspect pairs (perfective/imperfective) — see scope decision above.
- Dative, accusative, and instrumental cases — see scope decision above.
- Regional dialects/accents — pt-br modeled SP vs. RJ, ja deferred this; a Russian equivalent
  would be a deliberate follow-up, not an oversight.
