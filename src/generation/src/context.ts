import * as data from "./data.ts";
import { Formats } from "./formats.ts";
import { compareVersions, satisfies } from "./version.ts";

import type { Scope } from "./expression.ts";

function relative(list: ReadonlyArray<string>, item: string, offset: number): string {
    const index = list.indexOf(item);
    const value = list[index + offset];
    if (index === -1 || value === undefined) {
        throw new RangeError(`"${item}" has no ${offset < 0 ? "previous" : "next"} value in [${list.join(", ")}]`);
    }
    return value;
}

/** everything templates can use in their expressions */
export function createScope(version: string, loader: string, formats = new Formats(version)): Scope {
    // a null prototype keeps Object.prototype's members from shadowing globals in expressions
    return Object.assign(Object.create(null) as Scope, {
        ...data,

        mc: version,
        loader,
        since: (other: string) => compareVersions(version, other) >= 0,
        before: (other: string) => compareVersions(version, other) < 0,
        matches: (range: string) => satisfies(version, range),

        id: (path: string) => `${data.MOD_ID}:${path}`,
        previous: (list: ReadonlyArray<string>, item: string) => relative(list, item, -1),
        next: (list: ReadonlyArray<string>, item: string) => relative(list, item, 1),
        range: (length: number) => Array.from({ length }, (_, i) => i),
        repeat: <T>(length: number, value: T) => Array.from({ length }, () => value),
        // "light_blue" -> "Light Blue"
        title: (value: string) => value.split("_").map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(" "),

        ingredient: formats.ingredient.bind(formats),
        result: formats.result.bind(formats),
        icon: formats.icon.bind(formats),
        itemPredicate: formats.itemPredicate.bind(formats),
        tag: formats.tag.bind(formats),
        tagEntry: formats.tagEntry.bind(formats),
    });
}
