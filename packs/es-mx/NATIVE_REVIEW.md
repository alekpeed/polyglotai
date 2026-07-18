# es-mx pack — native-speaker review notes

This pack was authored by Claude at full Tier-1 scope (per the same "Claude authors, owner
reviews" pattern used for pt-br, ja, and fr). It passes the automated validator (schema +
cross-references + §10.1 volume targets), but nothing here has had a native Mexican speaker's
sign-off yet. None of the notes below block the app from running — they're
accuracy/naturalness checkpoints before treating the content as reviewed.

## Scope decisions (need an explicit owner call)

- **Mexican Spanish only (Central Mexico / CDMX standard), and deliberately so.** One dialect
  entry (`es-MX-standard`). This was chosen because es-MX is the neutral Latin American default
  AND the dialect Azure's pronunciation assessment models cleanly (see the Spanish-dialect
  scope note that preceded this build). Consequences baked into the content: **ustedes** for
  all plural 'you' (never vosotros), **seseo** throughout (c/z = [s], no [θ]), Mexican
  vocabulary (carro/coche, celular, chamba, jitomate, camión=bus, torta=sandwich, depa,
  recámara, colonia), and the [x] value for j/g/x in place names (México, Oaxaca).
- **Iberian Spanish is out of scope**: no vosotros, no distinción [θ], no Peninsular vocabulary
  (piso, móvil, ordenador, coger). A future `es-ES` would be a sibling pack, not a dialect
  toggle — the profanity systems alone diverge completely.
- **Other Latin American dialects are out of scope for pronunciation**: Rioplatense (sheísmo,
  voseo), Caribbean (s-aspiration/dropping), Andean, etc. Per the dialect discussion, grading
  those against the es-MX model produces false negatives, so this pack targets es-MX only.
- **Voseo is absent** — correct for Mexico (voseo is Argentine/Central American), but worth a
  conscious note since it's a defining feature elsewhere.
- **Identity-based slurs excluded** entirely from the profanity set, same policy as pt-br and
  fr, pending explicit owner review of framing. The set covers the culturally central
  chingar/madre families and pinche/pendejo/cabrón with severity and recognize-only/avoid
  guidance, but stops short of ethnic/homophobic/ableist slurs.

## Content worth a native check

- **IPA transcriptions** throughout vocabulary and the 20 pronunciation rules were authored
  directly. Spot-check the intervocalic softening ([β ð ɣ] in haba/nada/lago), the tap-vs-trill
  pairs (pero/perro, caro/carro), and the x place-name values (México [x], Xochimilco [s],
  Oaxaca [x]).
- **Slang currency and register** (slang/general.json): güey, neta, chido, chafa, fresa, la
  hueva, cuate/compa/carnal, ahorita's elasticity — the severity labels and 'who uses it'
  claims deserve a native's judgment; some (fresa, hueva) carry class/vulgarity nuance that
  shifts by region and generation.
- **The chingar and madre families** (profanity/general.json) are the hardest to calibrate:
  the notes claim 'chingón' is near-playful while 'chinga tu madre' is severity 7, and that
  'cabrón'/'pendejo' swing between insult and affection by tone. A native should confirm the
  severity spreads and the recognize-only/avoid guidance land right for an adult learner.
- **Prices in dialogues** (tacos 45 pesos, metro card 15 + 5/ride, rent 10,000, bus to Puebla
  480) were chosen as plausible for 2026 but are invented — worth a nudge to current reality.
- **Cultural claims** asserted as fact: OXXO as bill-pay/recarga hub, INE as default ID, 'la
  comida' as the main midday meal, provecho said to strangers, the tú/usted host-switch and
  the decline-then-accept hospitality dance (dialogue.saludo-usted), las mañanitas, aguas
  frescas by flavor. All standard, all time-sensitive.
- **The tú/usted register guidance** leans toward 'default to usted with strangers, more so
  than in Spain' — accurate for most of Mexico but varies by region, age, and setting (Mexico
  City younger circles tutear quickly); a reviewer may want to soften or regionalize.

## Deliberately deferred (not oversights)

- Subjunctive as a taught system — B1. (It appears in fixed phrases: ojalá sea, antes de que,
  que le vaya bien, cuando pueda — flagged in dialogue grammarNotes but not drilled.)
- Preterite vs imperfect full contrast drills — the imperfect intro plants the concept;
  systematic contrast belongs to the B1 expansion.
- Future simple and conditional production (recognition only; ir a + infinitive covers A2).
- Formal commands beyond the common service set; full pronoun-attachment rules.
- Themed micro-packs (es-mx-cantina, es-mx-mercado, es-mx-chilango-slang, es-mx-fut…) — the
  pt-br/ja/ru micro-pack pattern applies cleanly once the base pack is reviewed.
