# generation

Generates Project Expansion's models, blockstates, recipes, advancements, loot tables, tags and lang files for every
supported Minecraft version. Gradle runs it before `processResources`, writing into `versions/<version>/src/generated/resources`.

```sh
bun install
bun src/index.ts <minecraft version> <output dir> [--loader forge|neoforge] [--verbose]
bun test
```

## Templates

`templates/` mirrors the 1.21 resource layout (`assets/<namespace>/...`, `data/<namespace>/...`). Every `.json` file is
a template, and is written to the same path, moved to wherever the target version expects it (ex: `recipe/` becomes
`recipes/` before 1.21, and `c:` tags become their Forge equivalents). Other files are copied as is.

- Files and folders starting with `_` are partials, and are never written on their own.
- Files inside a folder ending with `.d` are merged into one file, ex: `lang/en_us.d/**/*.json` becomes `lang/en_us.json`.
  Setting the same key twice is an error.

### Headers

A template can be wrapped in a header to make several files or be skipped:

```json
{
	"$for": { "kind": ["collector", "relay"], "tier": "MATTER" },
	"$let": { "model": "`block/${kind}/${tier}`" },
	"$if": "since('1.19.4')",
	"$output": "{{tier}}_{{kind}}",
	"$content": { "parent": "{{id(model)}}" }
}
```

| Key        | Description                                                                                                  |
|------------|--------------------------------------------------------------------------------------------------------------|
| `$for`     | Renders the file for every combination of values. Later loops can use earlier ones, and `<name>Index` is set. |
| `$let`     | Extra variables, evaluated in order.                                                                         |
| `$if`      | Skips the file when false.                                                                                   |
| `$output`  | File name relative to the template's folder (`.json` is optional, a leading `/` is relative to the root).     |
| `$content` | The template.                                                                                                |

### Expressions

`{{expr}}` is replaced with the result of a JavaScript expression, in values and keys. A string that's only one
expression keeps the expression's raw value, so `"{{ingredient('minecraft:stone')}}"` becomes an object, and
`undefined` removes the property (or array element) entirely.

### Directives

| Directive                                                              | Result                                                                                                 |
|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `{"$if": "expr", ...rest}`                                             | `rest` when true, otherwise removed.                                                                   |
| `{"$if": "expr", "$then": a, "$else": b}`                              | `a` or `b` (removed when the branch isn't given).                                                      |
| `{"$version": {">=1.20.5": a, ">=1.17 <1.20.5": b, "else": c}}`        | The first branch whose range matches the version, ranges also take `||`.                               |
| `{"$each": "expr", "$as": "it", "$index": "i", "$do": template}`       | Renders `$do` for every item (arrays, numbers count up, objects give `{key, value}`). Spliced into arrays. |
| `"...name": value`                                                     | Merges an object, or every object from an `$each`, into the containing object.                         |
| `{"$template": "_partials/name", "$with": {...}, ...rest}`             | Renders a partial with extra variables, then merges `rest` over it.                                    |

Partials can declare defaults with `{"$defaults": {...}, "$content": ...}`. A default of `null` leaves the variable
undefined, so properties that only use it are left out.

### Variables

| Name                                                 | Description                                                                 |
|------------------------------------------------------|-----------------------------------------------------------------------------|
| `mc`, `loader`                                       | The version and loader being generated.                                     |
| `since(v)`, `before(v)`, `matches(range)`            | Version checks.                                                             |
| `MOD_ID`, `id(path)`                                 | `projectexpansion`, and `projectexpansion:<path>`.                          |
| `MATTER`, `MATTER_ITEMS`, `MATTER_BLOCKS`, `FUEL`    | Matter tiers, and the ones with matter items, matter blocks & fuel.        |
| `STAR_TYPES`, `STAR_TIERS`, `DYES`                   | Star types and tiers, and dye colors.                                       |
| `matterItem(tier)`                                   | A tier's matter item, using ProjectE's or the final star shard where needed. |
| `previous(list, item)`, `next(list, item)`           | Neighbors in a list, errors at the ends.                                    |
| `range(n)`, `repeat(n, value)`, `title(name)`        | `[0..n)`, `n` copies of a value, and `light_blue` -> `Light Blue`.          |

These produce whatever format the version reads, `#` marks a tag:

| Helper                       | Description                                                                          |
|------------------------------|--------------------------------------------------------------------------------------|
| `ingredient(ref)`            | A recipe ingredient. Common tags without a Forge equivalent fall back to an item.    |
| `result(id, count?)`         | A recipe result (`id` since 1.20.5, `item` before).                                  |
| `icon(id, {components, nbt})`| An advancement icon, using components since 1.20.5 and nbt before.                   |
| `itemPredicate(ref)`         | An item predicate (`items: "id"` since 1.20.5, `items: ["id"]` since 1.17, `item` before). |
| `tag(id)`                    | A tag's id in this version, or `null` when there isn't one.                          |
| `tagEntry(ref, required?)`   | A tag file value, optional when `required` is false.                                 |

New version differences belong in `src/formats.ts`, so templates can keep using the same helpers.
