import { childScope, evaluate, type Scope } from "./expression.ts";
import { satisfies } from "./version.ts";

/** returned for values that were left out, the containing property or array element is dropped */
export const MISSING = Symbol("missing");

/** the results of an $each, spliced into arrays and merged into objects when spread */
export class Expanded {
    constructor(readonly values: Array<unknown>) {}
}

export class TemplateError extends Error {
    override name = "TemplateError";
}

export interface Partial {
    content: unknown;
    /** values used for variables the caller didn't give, null leaves them undefined */
    defaults: Record<string, unknown>;
    file: string;
}

export type PartialLoader = (ref: string, from: string) => Partial;

const DIRECTIVE_KEYS: Record<string, Array<string>> = {
    $each: ["$each", "$as", "$index", "$do"],
    $template: ["$template", "$with"],
    $version: ["$version"],
};

export function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value) && !(value instanceof Expanded);
}

function describe(value: unknown): string {
    if (value === null) {
        return "null";
    }
    return Array.isArray(value) ? "an array" : typeof value;
}

/**
 * Renders JSON templates:
 * - "{{expr}}" in strings (and keys) is replaced with the expression's value. A string that is only one
 *   expression becomes its raw value, so "{{ingredient('minecraft:stone')}}" can produce an object.
 * - {"$if": "expr", ...rest} keeps the rest of the object only when the expression is true,
 *   or picks between {"$then": ..., "$else": ...}
 * - {"$version": {">=1.20.5": ..., "else": ...}} picks the first range the version matches
 * - {"$each": "expr", "$as": "name", "$index": "i", "$do": ...} repeats $do for every item,
 *   spliced into arrays, and merged into objects with a "..." key
 * - {"$template": "_partials/name", "$with": {...}, ...rest} renders a partial with extra variables,
 *   with the rest of the object merged over the result. Partials can be {"$defaults": {...}, "$content": ...}
 * - "...name": value spreads an object (or $each of objects) into the containing object
 */
export class Renderer {
    constructor(readonly version: string, readonly loadPartial: PartialLoader) {}

    private condition(input: unknown, scope: Scope): boolean {
        if (typeof input === "boolean") {
            return input;
        }
        if (typeof input !== "string") {
            throw new TypeError(`$if must be an expression or boolean, got ${describe(input)}`);
        }
        return Boolean(evaluate(input, scope));
    }

    private renderArray(input: Array<unknown>, scope: Scope, file: string, at: string): Array<unknown> {
        const output: Array<unknown> = [];
        for (const [i, element] of input.entries()) {
            const value = this.render(element, scope, file, `${at}[${i}]`);
            if (value === MISSING) {
                continue;
            }
            if (value instanceof Expanded) {
                output.push(...value.values);
            } else {
                output.push(value);
            }
        }
        return output;
    }

    private renderEach(input: Record<string, unknown>, scope: Scope, file: string, at: string): Expanded {
        let items = typeof input.$each === "string" ? evaluate(input.$each, scope) : this.renderValue(input.$each, scope, file, `${at}.$each`);
        if (typeof items === "number") {
            items = Array.from({ length: items }, (_, i) => i);
        } else if (isPlainObject(items)) {
            items = Object.entries(items).map(([key, value]) => ({ key, value }));
        }
        if (!Array.isArray(items)) {
            throw new TypeError(`$each needs an array, object or number, got ${describe(items)}`);
        }
        const as = typeof input.$as === "string" ? input.$as : "it", index = typeof input.$index === "string" ? input.$index : "i";
        const values: Array<unknown> = [];
        for (const [i, item] of (items as Array<unknown>).entries()) {
            const value = "$do" in input ? this.render(input.$do, childScope(scope, { [as]: item, [index]: i }), file, `${at}.$do[${i}]`) : item;
            if (value === MISSING) {
                continue;
            }
            if (value instanceof Expanded) {
                values.push(...value.values);
            } else {
                values.push(value);
            }
        }
        return new Expanded(values);
    }

    private renderObject(input: Record<string, unknown>, scope: Scope, file: string, at: string): unknown {
        if ("$if" in input) {
            const { $if, $then, $else, ...rest } = input;
            const passed = this.condition($if, scope);
            if ("$then" in input || "$else" in input) {
                if (Object.keys(rest).length !== 0) {
                    throw new TypeError("$if can't have other keys when it has $then or $else");
                }
                const branch = passed ? $then : $else;
                const name = passed ? "$then" : "$else";
                return name in input ? this.render(branch, scope, file, `${at}.${name}`) : MISSING;
            }
            return passed ? this.render(rest, scope, file, at) : MISSING;
        }

        const directive = Object.keys(DIRECTIVE_KEYS).find(key => key in input);
        if (directive !== undefined) {
            const unknown = Object.keys(input).filter(key => key.startsWith("$") && !DIRECTIVE_KEYS[directive]!.includes(key));
            if (unknown.length !== 0) {
                throw new TypeError(`Unknown keys for ${directive}: ${unknown.join(", ")}`);
            }
        }
        switch (directive) {
            case "$version": {
                return this.renderVersion(input, scope, file, at);
            }
            case "$each": {
                return this.renderEach(input, scope, file, at);
            }
            case "$template": {
                return this.renderTemplate(input, scope, file, at);
            }
        }

        const output: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(input)) {
            if (key.startsWith("$")) {
                throw new TypeError(`Unknown directive "${key}"`);
            }
            if (key.startsWith("...")) {
                const spread = this.render(value, scope, file, `${at}.${key}`);
                if (spread === MISSING) {
                    continue;
                }
                for (const part of spread instanceof Expanded ? spread.values : [spread]) {
                    if (!isPlainObject(part)) {
                        throw new TypeError(`"${key}" can only spread objects, got ${describe(part)}`);
                    }
                    Object.assign(output, part);
                }
                continue;
            }
            const name = this.renderString(key, scope);
            if (typeof name !== "string") {
                throw new TypeError(`Key "${key}" rendered to ${describe(name)}, keys have to be strings`);
            }
            const rendered = this.renderValue(value, scope, file, `${at}.${name}`);
            if (rendered !== MISSING) {
                output[name] = rendered;
            }
        }
        return output;
    }

    private renderTemplate(input: Record<string, unknown>, scope: Scope, file: string, at: string): unknown {
        const { $template, $with, ...rest } = input;
        if (typeof $template !== "string") {
            throw new TypeError(`$template must be a path, got ${describe($template)}`);
        }
        const variables = $with === undefined ? {} : this.renderValue($with, scope, file, `${at}.$with`);
        if (!isPlainObject(variables)) {
            throw new TypeError(`$with must be an object, got ${describe(variables)}`);
        }
        const partial = this.loadPartial($template, file);
        for (const [name, value] of Object.entries(partial.defaults)) {
            if (!(name in variables) && !(name in scope)) {
                variables[name] = value === null ? undefined : value;
            }
        }
        let output = this.renderValue(partial.content, childScope(scope, variables), partial.file);
        if (Object.keys(rest).length !== 0) {
            if (!isPlainObject(output)) {
                throw new TypeError(`"${$template}" rendered to ${describe(output)}, so other keys can't be merged into it`);
            }
            output = { ...output, ...this.renderObject(rest, scope, file, at) as Record<string, unknown> };
        }
        return output;
    }

    private renderVersion(input: Record<string, unknown>, scope: Scope, file: string, at: string): unknown {
        const ranges = input.$version;
        if (!isPlainObject(ranges)) {
            throw new TypeError(`$version must be an object of version ranges, got ${describe(ranges)}`);
        }
        for (const [range, value] of Object.entries(ranges)) {
            if (range === "else" || satisfies(this.version, range)) {
                return this.render(value, scope, file, `${at}.$version["${range}"]`);
            }
        }
        return MISSING;
    }

    render(node: unknown, scope: Scope, file: string, at = "$"): unknown {
        try {
            if (typeof node === "string") {
                return this.renderString(node, scope);
            }
            if (Array.isArray(node)) {
                return this.renderArray(node, scope, file, at);
            }
            if (isPlainObject(node)) {
                return this.renderObject(node, scope, file, at);
            }
            return node;
        } catch (err) {
            if (err instanceof TemplateError) {
                throw err;
            }
            throw new TemplateError(`${file} at ${at}: ${(err as Error).message}`, { cause: err });
        }
    }

    renderString(input: string, scope: Scope): unknown {
        const whole = /^\{\{([\s\S]*?)\}\}$/.exec(input);
        if (whole !== null && !whole[1]!.includes("{{") && !whole[1]!.includes("}}")) {
            const value = evaluate(whole[1]!, scope);
            return value === undefined ? MISSING : value;
        }
        return input.replaceAll(/\{\{([\s\S]*?)\}\}/g, (_, expression: string) => {
            const value = evaluate(expression, scope);
            if (typeof value !== "string" && typeof value !== "number" && typeof value !== "boolean") {
                throw new TypeError(`"{{${expression}}}" is ${describe(value)}, which can't be put in a string`);
            }
            return String(value);
        });
    }

    /** like render, but $each results become plain arrays */
    renderValue(node: unknown, scope: Scope, file: string, at = "$"): unknown {
        const value = this.render(node, scope, file, at);
        return value instanceof Expanded ? value.values : value;
    }
}
