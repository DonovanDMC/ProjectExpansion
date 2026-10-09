import { describe, expect, test } from "bun:test";

import { compareVersions, satisfies } from "../src/version.ts";

describe("versions", () => {
    test("compares versions with different lengths", () => {
        expect(compareVersions("1.21", "1.21.0")).toBe(0);
        expect(compareVersions("1.20.1", "1.20.5")).toBe(-1);
        expect(compareVersions("1.21.1", "1.9")).toBe(1);
    });

    test("matches ranges", () => {
        expect(satisfies("1.20.1", "*")).toBe(true);
        expect(satisfies("1.20.1", "1.20.1")).toBe(true);
        expect(satisfies("1.20.1", ">=1.17 <1.20.5")).toBe(true);
        expect(satisfies("1.21.1", ">=1.17 <1.20.5")).toBe(false);
        expect(satisfies("1.16.5", "<1.17 || >=1.21")).toBe(true);
    });

    test("rejects invalid versions", () => {
        expect(() => satisfies("1.20.1", ">=abc")).toThrow();
    });
});
