import { describe, expect, it } from "vitest";
import { CORE_LEARNING_PACKAGE_NAME } from "../src/index.js";

describe("core-learning scaffold", () => {
  it("loads the package entry point", () => {
    expect(CORE_LEARNING_PACKAGE_NAME).toBe("@polyglotai/core-learning");
  });
});
