# Vietnamese (vi) pack — native-review notes

**Status:** `authored-by-claude-pending-native-review`. This pack is machine-authored and has **not**
been reviewed by a native or fluent Vietnamese speaker. Everything below is a flag for that review.
Because every tone diacritic is meaning-bearing, **orthographic accuracy is the biggest risk**.

## Scope decisions (intentional, not gaps)
- **Northern (Hanoi) Vietnamese only.** Southern (Saigon) and Central varieties are out of scope for
  this pack version. Note the largest North/South difference is tonal: **Southern speakers merge hỏi
  and ngã into one tone (a 5-tone system)**, so this pack's six-tone guide is specifically Northern.
- **Speech grading is deliberately disabled.** No reliable off-the-shelf pronunciation scorer grades
  lexical tone — least of all the glottalized **ngã** and **nặng**. The pronunciation surface is a
  **display-only tone guide** (listen-and-repeat, ungraded). This is a product decision.
- **Identity-based slurs excluded.** The profanity set covers general Northern swearing graded by
  severity (1–7) but omits ethnic/regional/sexual/religious slurs pending explicit owner review.

## Convention used
The lemma is written in standard orthography (chữ Quốc ngữ), which **already carries the tone** in its
diacritic — so there is no separate romanization; the `romaji` field, where present, is only a light
English-approximation pronunciation hint. A reviewer should confirm every diacritic on every headword,
and that Northern lexical choices were used consistently (bát not tô, rẽ not quẹo, mì chính not bột
ngọt, đắt not mắc, quả not trái, bố not ba, etc.).

## Tone guide (pronunciation/rules.json) — verify the melodies
The flagship six-way minimal set should be checked first: **ma** (ngang, ghost) · **mà** (huyền, but) ·
**má** (sắc, cheek/mother) · **mả** (hỏi, grave) · **mã** (ngã, horse/code) · **mạ** (nặng, seedling).
Confirm the pitch-contour descriptions, especially the **glottal catch** notation for ngã and nặng,
and the Northern d/gi/r → /z/ merger and ch/tr merger entries.

## Items the authoring passes flagged (representative, not exhaustive)
**Vocabulary:** mười lăm (15) vs hai mươi (20) tens-marker; thứ tư (Wed) / chủ nhật (Sun); bảy (7);
đũa; xe buýt; bác sĩ (ngã); sếp; kém as comparative; càng…càng single-headword treatment; hoặc vs hay
for "or"; ốm (Northern "ill"); quả classifier; cục sạc; mũ vs nón; email/wifi respellings.
**Phrases:** Tính tiền vs Cho tôi tính tiền; Không cho mì chính vs không bỏ; A lô spelling; Cứu với vs
Cứu tôi với; Tôi vui bareness; khi nào placement and inclusive mình.
**Grammar:** được (post-verbal "can") vs có thể; classifier splits (cuốn/quyển, chiếc/cái); bao giờ/khi
nào front=future vs end=past; pronoun-shifting I/you claim; final-particle nuance (đấy, nhé vs nhỉ);
vừa/mới/vừa mới interchangeability; ditransitive cho order; rồi/chưa question-vs-statement.
**Dialogues:** 2026 Hanoi price realism (phở 40k, bia hơi, SIM); "tám số tám" wifi rendering; bia hơi
nhắm items (nem chua, lạc luộc); cam Hà Giang plausibility; "bốn gi-ga" for 4GB; formality tags on
street-food/taxi scenes with polite-casual address.
**Slang/profanity:** gato currency; bare chất as a compliment; severity gradings on vãi, đéo/đếch,
vcl/vl decomposition, địt mẹ abbreviations; ối giời ơi vs ối zời ơi orthography.

## What to prioritize
1. Every tone diacritic in `vocabulary/*.json` and `phrases/core.json`.
2. The tone-guide six-way set and the glottalized ngã/nặng descriptions.
3. The kinship-based pronoun/address choices in dialogues (age/relationship correctness).
4. Profanity severity gradings and the recognize-only/avoid guidance.
