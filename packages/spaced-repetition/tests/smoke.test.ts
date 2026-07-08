import { describe, expect, it } from "vitest";
import type { Scheduler } from "../src/index.js";

describe("spaced-repetition scaffold", () => {
  it("exposes a Scheduler interface shape usable by implementations", () => {
    const fakeScheduler: Scheduler = {
      initialState: () => ({
        difficulty: 0,
        stability: 0,
        retrievability: 1,
        state: "new",
        dueAt: new Date(0).toISOString(),
        lastReviewedAt: null,
        lapses: 0,
        reps: 0,
      }),
      schedule: (current) => current,
    };
    expect(fakeScheduler.initialState().state).toBe("new");
  });
});
