import { describe, expect, it } from "vitest";
import { createSubmissionGuard } from "./reviewSubmissionGuard";

describe("createSubmissionGuard", () => {
  it("runs only one concurrent submission and releases after it completes", async () => {
    const guard = createSubmissionGuard();
    let release!: () => void;
    const pending = new Promise<void>((resolve) => { release = resolve; });
    let calls = 0;
    const first = guard.run(async () => { calls += 1; await pending; });
    const second = await guard.run(async () => { calls += 1; });
    expect(second).toBe(false);
    expect(calls).toBe(1);
    release();
    expect(await first).toBe(true);
    expect(await guard.run(async () => { calls += 1; })).toBe(true);
    expect(calls).toBe(2);
  });
});
