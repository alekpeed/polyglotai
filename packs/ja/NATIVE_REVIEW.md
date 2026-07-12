# ja pack — native-speaker review notes

This pack was authored by Claude as a pilot (per the same "Claude authors, owner reviews"
pattern used for pt-br). It passes the automated validator (schema + cross-references), but
nothing here has had a native Japanese speaker's sign-off yet. None of the notes below block
the app from running — they're accuracy/naturalness checkpoints before treating this as more
than a pilot.

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
