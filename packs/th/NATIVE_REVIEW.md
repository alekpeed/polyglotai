# Thai (th) pack — native-review notes

**Status:** `authored-by-claude-pending-native-review`. This pack is machine-authored and has **not**
been reviewed by a native or fluent Thai speaker. Everything below is a flag for that review. The
biggest single risk is **tone accuracy** — a wrong tone diacritic is a wrong (often unrelated) word.

## Scope decisions (intentional, not gaps)
- **Standard / Central (Bangkok) Thai only.** Northern (คำเมือง), Isan/Lao, and Southern varieties are
  out of scope for this pack version.
- **Speech grading is deliberately disabled.** No reliable off-the-shelf pronunciation scorer grades
  lexical tone, which is meaning-bearing in Thai. The pronunciation surface is a **display-only tone
  guide** (listen-and-repeat, ungraded). Presenting a tone "score" we cannot compute accurately would
  mislead the learner. This is a product decision, not a missing feature.
- **Identity-based slurs excluded.** The profanity set covers general Thai swearing graded by severity
  (1–7) but deliberately omits ethnic/sexual/religious slurs pending explicit owner review of framing
  — same policy as the pt-br pack.

## Romanization convention used
Paiboon-style with tone diacritics on the vowel: **mid** = `maa` (no mark), **low** = `màa` (grave),
**falling** = `mâa` (circumflex), **high** = `máa` (acute), **rising** = `mǎa` (caron). A reviewer
should confirm both the *segmental* romanization and the *tone mark* on each headword.

## Tone guide (pronunciation/rules.json) — verify the melodies
Tone assignments were derived from consonant-class + tone-mark + syllable-type rules. The two flagship
minimal sets should be checked first, as everything keys off them:
- **khaa 5-way:** คา (mid, be stuck) · ข่า (low, galangal) · ค่า (falling, value) · ค้า (high, trade) ·
  ขา (rising, leg).
- **maa 3-way:** มา (mid, come) · ม้า (high, horse) · หมา (rising, dog).
Confirm the per-tone example words (ไก่ low, พ่อ falling, น้อง high, etc.) and the pitch-contour
descriptions.

## Items the authoring passes flagged (representative, not exhaustive)
**Vocabulary A1:** วันพฤหัสบดี (Thursday, multi-syllable tones); ยังไง (spoken "how", tones); เมื่อไหร่/
เท่าไหร่ (ไหร่ tone/length); แดด (low?); ปวด vs เจ็บ (ache/hurt split); นี่ (falling) vs นี้ (high) as the
demonstrative headword; ซื้อ (buy, อือ vowel + high tone); หมู (mǔu) vs มือ (mue) contrast; loanwords
แท็กซี่, รถไฟฟ้า.
**Vocabulary A2:** บริษัท (baw-rí-sàt), คอมพิวเตอร์, อีเมล, เพราะ (tone?), แอป/แอพ, รหัสผ่าน, สนามบิน,
เว็บไซต์, มะรืนนี้; ตั๋ว (rising) vs classifier ตัว (mid).
**Phrases:** เจอ romanization (jooe vs joe/jer); ราตรีสวัสดิ์ register vs everyday ฝันดี; เฉยๆ, เหนื่อย,
ฮัลโหล tones; the intentional template slots (มี…ไหม, ขอ…หนึ่งที่, ไป…ครับ) — confirm that pattern is
acceptable for the data format.
**Slang/profanity:** ชิลๆ respelling; แซ่บ (sâep vs sáep); severity calls on โคตร, สัตว์/สัส, มึง/กู
(context-dependent bonding vs. fighting word); ไอ้/อี merged male/female derogatory prefixes; ครับผม
(khráp vs colloquial kháp).
**Grammar & dialogues:** verify classifier choices, the ครับ/ค่ะ particle usage by speaker gender
throughout the dialogues, and that spoken reductions read naturally.

## What to prioritize
1. Every tone diacritic in `vocabulary/*.json` and `phrases/core.json`.
2. The tone-guide minimal sets and contour descriptions.
3. Politeness-particle correctness (ครับ/ค่ะ/คะ) by speaker gender in dialogues.
4. Profanity severity gradings and the recognize-only/avoid guidance.
