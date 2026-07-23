# Theme mockups

Self-contained HTML design concepts for PolyglotAI's per-language visual identities. Every file is
standalone — fonts and graphics are inlined (no network needed) — so you can just open it in any
browser. These are **design prototypes** (the "what it should feel like"), not the shipping app UI;
palettes get distilled into the app token systems (`apps/android-native/.../PolyPalette.kt` and
`apps/desktop-tauri/src/App.css`) separately.

## Shipping / implemented palettes
These looks are already wired into the app token system (light + dark):

| File | Language | Look |
|------|----------|------|
| `fr-cafe-de-paris.html` | French | **Café de Paris** — cream paper, near-black bistro ink, bright terrace red, absinthe green. The layout *is* a café: awning, chalkboard, and a `la carte` menu. |
| `ru-gzhel-hermitage-switcher.html` | Russian | Two switchable variants — **Gzhel** (cobalt-on-porcelain, default) and **Hermitage** (salon green + gilt). Toggle at the top. |
| `es-mx-barragan.html` | Mexican Spanish | **Barragán** — rosa mexicano, lilac, marigold; bold flat stucco planes. |

## Feature concept
| File | What it is |
|------|-----------|
| `tone-guide-th-vi.html` | The **tone guide** — Thai's 5 tones and Vietnamese's 6 drawn as pitch contours on a musical staff, with the meaning-changing minimal sets (khaa 5-way, ma 6-way). Ungraded, listen-and-repeat by design. |

## Concept directions (not yet implemented in-app)
Explorations where **the layout itself is the place**, not a reskinned template:

| File | Language | Concept |
|------|----------|---------|
| `th-grand-palace.html` | Thai | **Grand Palace / Wat Phra Kaew** — gilded tiered roofline, a golden-gable hero, jewel-mosaic mondop tiles, kranok flame dividers. |
| `vi-van-mieu-axis.html` | Vietnamese | **Văn Miếu (Temple of Literature)** — laid out as the ceremonial axis you process along: enter the great gate, pass under the Khuê Văn Các, cross the courtyard of the Well of Heavenly Clarity flanked by doctoral stelae on stone turtles, reach the sanctuary. |
| `es-mx-codex-journey.html` | Mexican Spanish | **Codex** — a footprint trail winding down the page between day-sign nodes, content painted onto alternating codex leaves; an alternate, ancient counterpart to the modernist Barragán. |
| `th-vi-theme-directions.html` | Thai + Vietnamese | The three-directions palette board that seeded the above. |

## Notes
- Speech grading is intentionally disabled for the tonal packs — no reliable off-the-shelf scorer
  grades lexical tone. See each pack's `metadata.json` / `NATIVE_REVIEW.md`.
- Rendering Thai in the actual apps needs a Thai-capable font (e.g. Noto Sans Thai) bundled; the
  mockups inline it. Vietnamese renders with the existing Latin fonts.
