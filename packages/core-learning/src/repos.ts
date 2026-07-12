import { FsrsScheduler } from "@polyglotai/spaced-repetition";
import { ConversationRepo, type IConversationRepo } from "./conversation/conversationRepo.js";
import { PronunciationRepo, type IPronunciationRepo } from "./pronunciation/pronunciationRepo.js";
import type { Database } from "./db/database.js";
import { FeatureFlagRegistry, type IFeatureFlagRegistry } from "./featureflags/registry.js";
import { PackRegistry } from "./packs/registry.js";
import { ProfileRepo, type IProfileRepo } from "./profile/profile.js";
import { ReviewRepo, type IReviewRepo } from "./review/reviewRepo.js";

/** The bundle of repositories/services the app composes over one Database connection. Typed by
 * interface (not the concrete SQL-backed classes) for profiles/flags/reviews/conversations/
 * pronunciation, so a non-SQL backend (e.g. a cloud-account implementation over Supabase
 * Postgres, see apps/desktop-tauri/src/cloud/) can satisfy this same shape. `db` and `packs`
 * stay concrete — every backend still reads pack content from the local bundled database. */
export interface Repos {
  db: Database;
  profiles: IProfileRepo;
  flags: IFeatureFlagRegistry;
  packs: PackRegistry;
  reviews: IReviewRepo;
  conversations: IConversationRepo;
  pronunciation: IPronunciationRepo;
}

/**
 * Wires every repository over a single Database (plan §3). The app calls this once with the
 * Tauri adapter; tests call it with the node:sqlite adapter. `clock` is injected so tests get
 * deterministic timestamps.
 */
export function createRepos(db: Database, clock: () => Date = () => new Date()): Repos {
  const iso = () => clock().toISOString();
  return {
    db,
    profiles: new ProfileRepo(db, iso),
    flags: new FeatureFlagRegistry(db, iso),
    packs: new PackRegistry(db),
    reviews: new ReviewRepo(db, new FsrsScheduler(), clock),
    conversations: new ConversationRepo(db, iso),
    pronunciation: new PronunciationRepo(db, iso),
  };
}
