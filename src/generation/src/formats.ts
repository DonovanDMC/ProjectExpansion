import { compareVersions } from "./version.ts";

// JSON shapes that changed between Minecraft versions. Templates are written with these helpers
// so each version gets the format it reads, instead of rewriting the output afterward.

export type JsonObject = Record<string, unknown>;

interface TagAlias {
    // an item to use in its place before then, where there's no equivalent tag
    item?: string;
    // the first version where the tag exists under its own name
    since: string;
    // the equivalent tag before then
    tag?: string;
}

// common (c:) tags are Forge's before 1.21, some of which didn't have an equivalent
const TAG_ALIASES: Record<string, TagAlias> = {
    "c:chests": { since: "1.21", tag: "forge:chests" },
    "c:glass_blocks": { since: "1.21", tag: "forge:glass" },
    "c:obsidians": { since: "1.21", tag: "forge:obsidian" },
    "c:player_workstations/crafting_tables": { since: "1.21", item: "minecraft:crafting_table" },
};

// resource folders that were made singular in 1.21
const PLURAL_FOLDERS: Record<string, string> = {
    "advancement": "advancements",
    "loot_table": "loot_tables",
    "recipe": "recipes",
    "tags/block": "tags/blocks",
    "tags/entity_type": "tags/entity_types",
    "tags/fluid": "tags/fluids",
    "tags/item": "tags/items",
};

export class Formats {
    constructor(readonly version: string) {}

    /** what to use in place of a tag that doesn't exist in this version */
    private tagFallback(id: string): string {
        const item = TAG_ALIASES[id]?.item;
        if (item === undefined) {
            throw new Error(`Tag "${id}" has no equivalent in ${this.version}`);
        }
        return item;
    }

    /**
     * an advancement icon, extra data is given as both its 1.20.5+ components and its older nbt
     * ex: icon("minecraft:stone", { components: { ... }, nbt: "{...}" })
     */
    icon(id: string, data: { components?: JsonObject; nbt?: string } = {}): JsonObject {
        if (this.since("1.20.5")) {
            return { id, ...(data.components === undefined ? {} : { components: data.components }) };
        }
        return { item: id, ...(data.nbt === undefined ? {} : { nbt: data.nbt }) };
    }

    /** a recipe ingredient, "#" marks a tag */
    ingredient(ref: string): JsonObject {
        if (ref.startsWith("#")) {
            const tag = this.tag(ref.slice(1));
            return tag === null ? { item: this.tagFallback(ref.slice(1)) } : { tag };
        }
        return { item: ref };
    }

    /** an item predicate matching one item, "#" marks a tag */
    itemPredicate(ref: string): JsonObject {
        if (ref.startsWith("#")) {
            const tag = this.tag(ref.slice(1));
            if (tag === null) {
                return this.itemPredicate(this.tagFallback(ref.slice(1)));
            }
            return this.since("1.20.5") ? { items: `#${tag}` } : { tag };
        }
        if (this.since("1.20.5")) {
            return { items: ref };
        }
        // predicates only took a single item before 1.17
        return this.since("1.17") ? { items: [ref] } : { item: ref };
    }

    /** where a file in the 1.21 layout lives in this version, relative to the resources root */
    path(path: string): string {
        const match = /^data\/([^/]+)\/(.+)$/.exec(path);
        if (match === null) {
            return path;
        }
        let [, namespace, rest] = match as unknown as [string, string, string];
        // common tag files move to whatever they alias to
        if (namespace === "c" && rest.startsWith("tags/")) {
            const [, type, name] = /^tags\/([^/]+)\/(.+)\.json$/.exec(rest) ?? [];
            if (type !== undefined && name !== undefined) {
                const tag = this.tag(`c:${name}`);
                if (tag === null) {
                    throw new Error(`Tag file "${path}" has no equivalent in ${this.version}`);
                }
                const [tagNamespace, tagName] = tag.split(":") as [string, string];
                namespace = tagNamespace;
                rest = `tags/${type}/${tagName}.json`;
            }
        }
        if (!this.since("1.21")) {
            for (const [singular, plural] of Object.entries(PLURAL_FOLDERS)) {
                if (rest.startsWith(`${singular}/`)) {
                    rest = `${plural}${rest.slice(singular.length)}`;
                    break;
                }
            }
        }
        return `data/${namespace}/${rest}`;
    }

    /** a recipe result, item stacks use "id" since 1.20.5 */
    result(id: string, count = 1): JsonObject {
        return {
            [this.since("1.20.5") ? "id" : "item"]: id,
            ...(count === 1 ? {} : { count }),
        };
    }

    since(version: string): boolean {
        return compareVersions(this.version, version) >= 0;
    }

    /** a tag's id for this version, or null if it doesn't have one */
    tag(id: string): string | null {
        const alias = TAG_ALIASES[id];
        if (alias === undefined || this.since(alias.since)) {
            return id;
        }
        return alias.tag ?? null;
    }

    /** a value in a tag file, optional entries don't fail to load when what they point to is missing */
    tagEntry(ref: string, required = true): unknown {
        const value = ref.startsWith("#") ? `#${this.tag(ref.slice(1)) ?? this.tagFallback(ref.slice(1))}` : ref;
        return required ? value : { id: value, required: false };
    }
}
