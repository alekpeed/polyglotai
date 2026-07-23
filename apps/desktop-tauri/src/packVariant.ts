// Palette variants within a pack "world" (the value App sets as data-pack: manifest.basePack ??
// manifest.id). Most worlds have a single look and never appear here; the ones that offer a choice
// list their variants most-preferred first — the first entry is the default and clears data-variant
// so the base [data-pack] tokens apply. Non-default variants set data-variant, which the matching
// [data-pack][data-variant] rules in App.css override with.

export interface PackVariant {
  id: string;
  label: string;
  hint: string;
}

export const PACK_VARIANTS: Record<string, PackVariant[]> = {
  ru: [
    { id: "gzhel", label: "Gzhel", hint: "Cobalt-on-porcelain — hand-painted blue-and-white folk ceramics." },
    { id: "hermitage", label: "Hermitage", hint: "Salon green and gilt on warm marble — a gilded museum hall." },
  ],
};

/** The variants a world offers (empty if it has a single look — hide the picker). */
export function variantsForPack(packWorldId: string | undefined): PackVariant[] {
  return (packWorldId && PACK_VARIANTS[packWorldId]) || [];
}

const STORAGE_KEY = "polyglotai-pack-variant";

function readMap(): Record<string, string> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Record<string, string>) : {};
  } catch {
    return {};
  }
}

/** The stored variant id for a world, or its default (first option) — or undefined for worlds with
 *  no variants. A stale stored value (variant later removed) falls back to the default. */
export function getStoredVariant(packWorldId: string | undefined): string | undefined {
  const options = variantsForPack(packWorldId);
  if (!options.length) return undefined;
  const stored = readMap()[packWorldId!];
  return options.some((v) => v.id === stored) ? stored : options[0].id;
}

export function setStoredVariant(packWorldId: string, variant: string) {
  const map = readMap();
  map[packWorldId] = variant;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(map));
  applyVariant(packWorldId, variant);
}

/** Reflect a world's active variant onto the root as data-variant (drives the CSS). Worlds with no
 *  variants — or one sitting on its default — clear the attribute so the base [data-pack] tokens win. */
export function applyVariant(packWorldId: string | undefined, variant?: string) {
  const root = document.documentElement;
  const options = variantsForPack(packWorldId);
  const active = variant ?? getStoredVariant(packWorldId);
  if (!options.length || !active || active === options[0].id) {
    root.removeAttribute("data-variant");
  } else {
    root.setAttribute("data-variant", active);
  }
}
