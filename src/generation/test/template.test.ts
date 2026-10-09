import { describe, expect, test } from "bun:test";

import { createScope } from "../src/context.ts";
import { MISSING, Renderer, TemplateError, type Partial } from "../src/template.ts";

function render(template: unknown, version = "1.21.1", partials: Record<string, Partial> = {}): unknown {
    const renderer = new Renderer(version, (ref) => {
        const partial = partials[ref];
        if (partial === undefined) {
            throw new Error(`no partial ${ref}`);
        }
        return partial;
    });
    return renderer.renderValue(template, createScope(version, "neoforge"), "test.json");
}

describe("templates", () => {
    test("interpolates strings and keys", () => {
        expect(render({ "{{MOD_ID}}:key": "{{1 + 1}} items" })).toEqual({ "projectexpansion:key": "2 items" });
    });

    test("keeps the raw value of a single expression", () => {
        expect(render({ value: "{{[1, 2]}}", number: "{{3}}" })).toEqual({ value: [1, 2], number: 3 });
    });

    test("drops undefined values", () => {
        expect(render({ a: "{{undefined}}", b: [1, "{{undefined}}"] })).toEqual({ b: [1] });
    });

    test("$if keeps or drops the rest of the object", () => {
        expect(render([{ $if: "since('1.21')", a: 1 }, { $if: "before('1.21')", b: 2 }])).toEqual([{ a: 1 }]);
        expect(render({ value: { $if: false, $then: 1, $else: 2 } })).toEqual({ value: 2 });
        expect(render({ value: { $if: false, $then: 1 } })).toEqual({});
    });

    test("$version picks the first matching range", () => {
        const template = { $version: { ">=1.20.5": "new", ">=1.17": "middle", "else": "old" } };
        expect(render(template, "1.21.1")).toBe("new");
        expect(render(template, "1.20.1")).toBe("middle");
        expect(render(template, "1.16.5")).toBe("old");
        expect(render({ value: { $version: { ">=1.21": 1 } } }, "1.20.1")).toEqual({});
    });

    test("$each splices into arrays and spreads into objects", () => {
        expect(render([0, { $each: "[1, 2]", $do: "{{it * 10}}" }, 3])).toEqual([0, 10, 20, 3]);
        expect(render({ "...": { $each: "['a', 'b']", $as: "key", $index: "n", $do: { "{{key}}": "{{n}}" } } })).toEqual({ a: 0, b: 1 });
        expect(render({ $each: 2 })).toEqual([0, 1]);
    });

    test("$template renders partials with defaults", () => {
        const partials = { thing: { content: { name: "{{name}}", extra: "{{extra}}" }, defaults: { extra: null }, file: "_partials/thing.json" } };
        expect(render({ $template: "thing", $with: { name: "a" } }, "1.21.1", partials)).toEqual({ name: "a" });
        expect(render({ $template: "thing", $with: { name: "a", extra: 1 }, other: true }, "1.21.1", partials)).toEqual({ name: "a", extra: 1, other: true });
    });

    test("reports where errors happen", () => {
        expect(() => render({ a: { b: "{{missing}}" } })).toThrow(TemplateError);
        expect(() => render({ a: { b: "{{missing}}" } })).toThrow("test.json at $.a.b: missing is not defined");
        expect(() => render({ $unknown: 1 })).toThrow("Unknown directive");
    });

    test("MISSING is not leaked", () => {
        expect(render("{{undefined}}")).toBe(MISSING);
    });
});
