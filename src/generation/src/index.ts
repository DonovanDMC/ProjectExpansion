import { resolve } from "node:path";
import { parseArgs } from "node:util";

import { Generator, writeOutputs } from "./generator.ts";
import { TemplateError } from "./template.ts";
import { compareVersions } from "./version.ts";

const USAGE = "usage: bun src/index.ts <minecraft version> <output dir> [--loader forge|neoforge] [--verbose]";

const { values, positionals } = parseArgs({
    allowPositionals: true,
    options: {
        loader: { type: "string" },
        templates: { type: "string" },
        verbose: { type: "boolean", short: "v" },
        help: { type: "boolean", short: "h" },
    },
});

const [version, output] = positionals;
if (values.help || version === undefined || output === undefined) {
    console.log(USAGE);
    process.exit(values.help ? 0 : 1);
}

const generator = new Generator({
    templates: resolve(values.templates ?? resolve(import.meta.dir, "../templates")),
    version,
    // only matters to templates that check it, we use NeoForge after 1.20.1
    loader: values.loader ?? (compareVersions(version, "1.20.2") >= 0 ? "neoforge" : "forge"),
});

try {
    const outputs = await generator.generate();
    await writeOutputs(outputs, resolve(output));
    if (values.verbose) {
        for (const file of outputs.values()) {
            console.log(`${file.sources.join(", ")} -> ${file.path}`);
        }
    }
    console.log(`Generated ${outputs.size} files for ${version} (${generator.options.loader})`);
} catch (err) {
    if (err instanceof TemplateError) {
        console.error(`error: ${err.message}`);
        process.exit(1);
    }
    throw err;
}
