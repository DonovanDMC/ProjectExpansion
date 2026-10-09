import { resolve } from "node:path";

import { describe, expect, test } from "bun:test";

import { Generator } from "../src/generator.ts";

const VERSIONS = ["1.16.5", "1.18.2", "1.19.2", "1.20.1", "1.21.1"];

function leftovers(value: unknown, path = "$"): Array<string> {
    if (typeof value === "string") {
        return value.includes("{{") ? [path] : [];
    }
    if (Array.isArray(value)) {
        return value.flatMap((item, i) => leftovers(item, `${path}[${i}]`));
    }
    if (typeof value === "object" && value !== null && !(value instanceof Uint8Array)) {
        return Object.entries(value).flatMap(([key, item]) => [...(key.startsWith("$") || key.startsWith("...") ? [`${path}.${key}`] : []), ...leftovers(item, `${path}.${key}`)]);
    }
    return [];
}

describe("project templates", () => {
    for (const version of VERSIONS) {
        test(`render cleanly for ${version}`, async () => {
            const outputs = await new Generator({ templates: resolve(import.meta.dir, "../templates"), version, loader: "forge" }).generate();
            expect(outputs.size).toBeGreaterThan(800);
            const problems = [...outputs.values()].flatMap(file => leftovers(file.content).map(at => `${file.path} ${at}`));
            expect(problems).toEqual([]);
        });
    }

    test("version specific files are only made where they're supported", async () => {
        const render = (version: string): Promise<Map<string, unknown>> => new Generator({ templates: resolve(import.meta.dir, "../templates"), version, loader: "forge" }).generate();
        const [old, current] = await Promise.all([render("1.16.5"), render("1.21.1")]);
        expect(current.has("data/projectexpansion/enchantment/alchemical_collection.json")).toBe(true);
        expect(old.has("data/projectexpansion/enchantment/alchemical_collection.json")).toBe(false);
        expect(old.has("data/projectexpansion/recipes/condenser_mk3.json")).toBe(true);
        expect(old.has("data/forge/tags/items/chests.json")).toBe(true);
    });
});
