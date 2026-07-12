# pt-br pack — native-speaker review notes

This pack was authored by Claude (per the owner's decision: Claude authors, owner reviews).
It passes the automated validator (schema + cross-references + §10.1 volume targets), but the
items below were flagged by the authoring agents as needing a native Brazilian speaker's
sign-off. None block the app from running; they're quality/accuracy checkpoints.

## Editorial line (needs an explicit owner decision)

- **Identity-based slurs (racial, homophobic, ableist) were deliberately excluded** from the
  profanity set, even as recognize-only, pending owner review of *how* to frame them. The
  severity 7 item `vai tomar no cu` has warning notes flagging its homophobic imagery.
  Decision needed: include such terms as recognize-only with strong warnings, or keep them out?

## Grammar

- `grammar.voce-tu`: the claim that "tu falas" sounds bookish or specifically gaúcho/maranhense
  — regional characterization worth a native check.
- `grammar.ir-future`: "vou ir" presented as avoid-but-you'll-hear-it — soften or harden?
- `grammar.plural-formation`: bare-noun plural ("os amigo") described as stigmatized — review tone.
- `grammar.ficar`: the deliberately ambiguous "ficaram na festa" (stayed vs hooked up) example.
- `grammar.tem-ha`: "tem dois anos que moro aqui" (tem = ago) — confirm it belongs at A1.
- `grammar.gostar-de`: the "dji" palatalization note — region-qualify?

## New in slang/2026-internet.json (web-researched, highest review priority)

Fourteen current internet/Gen Z slang terms (farmar aura, algoritmado, NPC, delulu, lacrou,
surtou, mó, biscoiteiro/dar biscoito, lançou a braba, shippar, crush, calabreso, casca de bala,
tankar/intankável), added via web research (search + cross-referencing multiple sources) rather
than from training knowledge alone, at explicit owner request for "cutting edge street slang."
This category carries more risk than any other content in this pack and deserves the most
skeptical native read:

- **Freshness has a shelf life.** Several of these are tied to specific 2024-2025 viral moments
  (calabreso to a reality-TV argument, casca de bala to a song) — they may already read as
  dated/last-year slang to some speakers, or may have moved on to new variants by the time this
  is reviewed. Worth periodically re-checking rather than treating as a one-time addition.
- **English loanwords vs. genuine Portuguese slang** — deliberately excluded pure English meme
  terms with no Portuguese-specific adaptation (skibidi, rizz, brainrot) that came up in the same
  research, since they don't teach anything about Portuguese; kept shippar/crush because they're
  grammatically or usage-wise absorbed into Portuguese sentences. Worth a native check on whether
  that line was drawn in the right place.
- **Register/severity calibration** — all tagged `register: "internet"`, severity 1-2; a native
  speaker may judge some of these (NPC, biscoiteiro) as landing sharper/more mocking than the
  severity suggests.
- Web sources for each term are not retained in the repo — if a term needs re-verification, a
  fresh search is the right move rather than assuming the original research still holds.

## Slang / profanity / pronunciation

- Severity calibration of `puta` at 6 (rated for the noun's worst use; the interjection alone is ~5).
- `pron.e-aberto-fechado` / metaphony pairs (seca noun vs verb, sede/sede) — natives sometimes
  dispute these by region.
- "manjar" gloss in the j/ch pronunciation pair.
- The claim that reduced "feira" approaches "fera" in fast speech — ear-check.
- "ficar" glossed as usually-kissing-not-sex — regionally/generationally variable.

## Vocabulary / phrases

- IPA follows a broad São Paulo standard (coda r as /ʁ/, final -e/-o as /i/,/u/) — a carioca or
  southern reviewer may prefer different coda-r symbols.
- `vocab.real-currency` note "dez conto" (informal) — belongs here or in the slang file?
- `phrase.pois-nao` literalMeaning "well, no?" — sanity-check the gloss of this genuinely odd idiom.
- `vocab.seis` note about "meia" for 6 on the phone — reads clearly?
- `phrase.voce-vem-sempre-aqui` labeled register "humorous" (intentionally corny pickup line) —
  keep or drop?
- Slug suffixes `vocab.metro-subway`, `vocab.real-currency` (lemmas "metrô"/"real") — rename to
  bare slugs if preferred.

## Dialogues

- `dialogue.texting-friend` uses authentic textspeak (td, vc, q, dps, bjs, kkk) — confirm it
  should render as-is; it probably should not get TTS audio, or needs expansion rules.
- `dialogue.negotiating-market`: "seu Zé" endearments (minha filha, freguesa) — dated for the region?
- `dialogue.customer-service`: CPF read as spelled-out numbers to avoid digit-string TTS issues.
- `dialogue.hair-salon`: "cortar uns quatro dedos" (finger-widths) — region/salon-culture dependent.
