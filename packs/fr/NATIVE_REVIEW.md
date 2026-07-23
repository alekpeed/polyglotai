# fr pack — native-speaker review notes

This pack was authored by Claude at full Tier-1 scope (per the same "Claude authors, owner
reviews" pattern used for pt-br and ja). It passes the automated validator (schema +
cross-references + §10.1 volume targets), but nothing here has had a native French speaker's
sign-off yet. None of the notes below block the app from running — they're
accuracy/naturalness checkpoints before treating the content as reviewed.

## Scope decisions (need an explicit owner call)

- **Metropolitan French only.** Quebec, Belgian, Swiss, and African francophone variation is
  deliberately out of scope for v0.1 — one dialect entry (`fr-FR-standard`). Adding a
  `fr-CA` variant later would follow the pt-br dialect model (SP/RJ), but Quebec French
  differs enough (vocabulary, sacres-based profanity system, tu/vous norms, pronunciation)
  that it is closer to a sibling pack than a dialect toggle. Owner call needed on which.
- **The [œ̃]/[ɛ̃] nasal merger is presented as the default.** The pronunciation section teaches
  'un/brun' as merged into [ɛ̃] "for most Metropolitan speakers", noting the South and Belgium
  keep the distinction. This matches modern Parisian usage but a reviewer may prefer teaching
  the four-nasal system conservatively.
- **Dropped-ne negation is taught as standard spoken French, not as sloppiness.** Same
  editorial stance the pt-br pack takes on 'tô/cê'. Sentences are shown in both forms with
  register labels. A traditionalist reviewer may object; the product philosophy
  (spoken-reality-first) says keep it.
- **'on' is promoted over 'nous' for production.** Grammar item `grammar.on` explicitly tells
  the learner to default to on in speech. This is accurate for conversation but means learner
  output will skew casual; the formal register is still taught for recognition.
- **Identity-based slurs excluded** from the profanity set entirely, same policy as pt-br,
  pending explicit owner review of framing. The set stops at salaud/salope (severity 6) and
  documents the masculine/feminine asymmetry of that pair directly.

## Content worth a native check

- **IPA transcriptions** throughout vocabulary and pronunciation files were authored directly.
  Spot-check especially: e-muet syllabification in words like 'boulangerie' [bu.lɑ̃ʒ.ʁi] vs
  [bu.lɑ̃.ʒə.ʁi], 'médecin' [med.sɛ̃], and the liaison examples in `pron.liaison`.
- **Verlan glosses** (slang/general.json): meuf, ouf, relou, chelou, wesh, chanmé — the
  register/severity labels and 'who uses it' claims deserve a native's judgment; verlan
  currency shifts fast and a 2026-current speaker should confirm none of these read as dated.
- **Price/number realism in dialogues**: carnet price (€17.35), café crème (€4.50), rents
  (€950 for a Paris two-pièces) were chosen to be plausible for 2026 but are invented — a
  reviewer may want to nudge them.
- **The bise etiquette dialogue** (`dialogue.bises-etiquette`) makes regional claims (two
  bises, right cheek first in Paris) that are famously contested — deliberately hedged in the
  dialogue itself ('ça dépend des régions'), but worth a look.
- **'Tradition' baguette ordering** and boulangerie protocol, cheese-course conventions
  (`dialogue.fromagerie`), SNCF composter rules (paper tickets only now), and the pharmacy
  triage culture are asserted as cultural facts; all are standard but time-sensitive.
- **Grammar simplifications**: `grammar.imparfait-intro` teaches only c'était / il y avait /
  il faisait for production at A2 (recognition for the rest); `grammar.questions` labels
  inversion 'formal/written'. Both are pedagogical choices, not errors, but a teacher-reviewer
  may want to calibrate.

## Deliberately deferred (not oversights)

- Futur simple production (recognition only, via `grammar.futur-proche` note).
- Subjunctive entirely — B1 territory.
- Relative pronouns (qui/que/dont) — B1.
- Passé composé vs imparfait full contrast drills — the intro item plants the concept;
  systematic drilling belongs to the B1 expansion.
- Themed micro-packs (fr-marche, fr-bistrot, fr-verlan…) — the pt-br/ja/ru micro-pack pattern
  applies cleanly once the base pack is reviewed.
