export type ThemePreference = "light" | "dark" | "system";

const STORAGE_KEY = "polyglotai-theme";

/** "system" means no stored override — the OS's prefers-color-scheme drives it. */
export function getStoredTheme(): ThemePreference {
  const v = localStorage.getItem(STORAGE_KEY);
  return v === "light" || v === "dark" ? v : "system";
}

function applyTheme(pref: ThemePreference) {
  const root = document.documentElement;
  if (pref === "system") root.removeAttribute("data-theme");
  else root.setAttribute("data-theme", pref);
}

export function setTheme(pref: ThemePreference) {
  if (pref === "system") localStorage.removeItem(STORAGE_KEY);
  else localStorage.setItem(STORAGE_KEY, pref);
  applyTheme(pref);
}

/** Call once, synchronously, before the first paint — avoids a light/dark flash on load. */
export function initTheme() {
  applyTheme(getStoredTheme());
}
