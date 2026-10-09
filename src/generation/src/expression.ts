export type Scope = Record<string, unknown>;

type Compiled = (scope: Scope) => unknown;
const cache = new Map<string, Compiled>();

/** evaluates a JavaScript expression with the scope's values as variables */
export function evaluate(expression: string, scope: Scope): unknown {
    let compiled = cache.get(expression);
    if (compiled === undefined) {
        // eslint-disable-next-line @typescript-eslint/no-implied-eval -- templates are trusted, and this is what lets them use plain expressions
        compiled = new Function("$scope", `with ($scope) { return (${expression}\n); }`) as Compiled;
        cache.set(expression, compiled);
    }
    return compiled(scope);
}

/** a scope that falls back to its parent for anything it doesn't define */
export function childScope(parent: Scope, values: Scope): Scope {
    return Object.assign(Object.create(parent) as Scope, values);
}
