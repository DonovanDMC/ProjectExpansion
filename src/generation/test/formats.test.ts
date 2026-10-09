import { describe, expect, test } from "bun:test";

import { Formats } from "../src/formats.ts";

const v1165 = new Formats("1.16.5"), v1201 = new Formats("1.20.1"), v1211 = new Formats("1.21.1");

describe("formats", () => {
    test("ingredients convert common tags", () => {
        expect(v1211.ingredient("#c:chests")).toEqual({ tag: "c:chests" });
        expect(v1201.ingredient("#c:chests")).toEqual({ tag: "forge:chests" });
        expect(v1201.ingredient("#c:player_workstations/crafting_tables")).toEqual({ item: "minecraft:crafting_table" });
        expect(v1201.ingredient("minecraft:stone")).toEqual({ item: "minecraft:stone" });
    });

    test("results use id since 1.20.5", () => {
        expect(v1211.result("minecraft:stone", 2)).toEqual({ id: "minecraft:stone", count: 2 });
        expect(v1201.result("minecraft:stone")).toEqual({ item: "minecraft:stone" });
    });

    test("item predicates", () => {
        expect(v1211.itemPredicate("minecraft:stone")).toEqual({ items: "minecraft:stone" });
        expect(v1211.itemPredicate("#minecraft:logs")).toEqual({ items: "#minecraft:logs" });
        expect(v1201.itemPredicate("minecraft:stone")).toEqual({ items: ["minecraft:stone"] });
        expect(v1201.itemPredicate("#minecraft:logs")).toEqual({ tag: "minecraft:logs" });
        expect(v1165.itemPredicate("minecraft:stone")).toEqual({ item: "minecraft:stone" });
    });

    test("icons carry components or nbt", () => {
        const data = { components: { "minecraft:custom_name": "\"a\"" }, nbt: "{a: 1}" };
        expect(v1211.icon("minecraft:stone", data)).toEqual({ id: "minecraft:stone", components: data.components });
        expect(v1201.icon("minecraft:stone", data)).toEqual({ item: "minecraft:stone", nbt: data.nbt });
    });

    test("paths are moved for older versions", () => {
        expect(v1211.path("data/projectexpansion/recipe/a.json")).toBe("data/projectexpansion/recipe/a.json");
        expect(v1201.path("data/projectexpansion/recipe/a.json")).toBe("data/projectexpansion/recipes/a.json");
        expect(v1201.path("data/minecraft/tags/block/mineable/pickaxe.json")).toBe("data/minecraft/tags/blocks/mineable/pickaxe.json");
        expect(v1201.path("data/minecraft/tags/damage_type/a.json")).toBe("data/minecraft/tags/damage_type/a.json");
        expect(v1201.path("data/c/tags/item/chests.json")).toBe("data/forge/tags/items/chests.json");
        expect(v1201.path("assets/projectexpansion/models/item/a.json")).toBe("assets/projectexpansion/models/item/a.json");
    });
});
