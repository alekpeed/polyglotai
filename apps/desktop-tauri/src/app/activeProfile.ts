const STORAGE_KEY = "polyglotai-active-profile-id";

/** Which of an account's (possibly several, one-per-language) profiles was last active on this
 * device — a UI convenience, not synced data. Null means "no choice remembered yet", not
 * "no profiles exist". */
export function getStoredActiveProfileId(): string | null {
  return localStorage.getItem(STORAGE_KEY);
}

export function setStoredActiveProfileId(id: string): void {
  localStorage.setItem(STORAGE_KEY, id);
}

/** Forgets the remembered choice — used by "switch language" to return to the picker. */
export function clearStoredActiveProfileId(): void {
  localStorage.removeItem(STORAGE_KEY);
}
