import { exists } from "./util";
import { mkdir, readdir, rm } from "node:fs/promises";

const inDir = new URL("generators/", import.meta.url);
const outDir = new URL("out/", import.meta.url);
if (await exists(outDir)) {
    await rm(outDir, { recursive: true, force: true });
}

await mkdir(outDir);

interface ModuleIndex {
    format: string;
    outDir: URL;
}

interface ModuleRunner {
    default(this: void, outDir: URL): Promise<Array<[string, string]>>;
}

async function run(dir: URL) {
    const { format, outDir: out } = await import(new URL("index.ts", dir).pathname) as ModuleIndex;
    if (await exists(out)) {
        await rm(out, { recursive: true, force: true });
    }
    await mkdir(out, { recursive: true });
    for (const gen of (await readdir(dir, { withFileTypes: true }))) {
        if (gen.isDirectory() || !gen.name.endsWith(".ts") || gen.name === "index.ts") {
            continue;
        }
        const { default: mod } = await import(new URL(gen.name, dir).pathname) as ModuleRunner;
        // @ts-expect-error arrays can be empty, ts begs to differ
        const res = (await mod(out)).filter(arr => arr.length !== 0);
        for (const [input, output] of res) {
            if (dir.pathname.endsWith("lang/")) {
                console.log(format.trim(), gen.name, output.split("/").slice(-1)[0].replace(/\.json/, ""));
            } else {
                console.log(format.trim(), input.replace(dir.pathname, "").replace(/\//, ""), gen.name, output.replace(out.pathname, "").replace(/\.json/, ""));
            }
        }
    }
}

const mod = await readdir(inDir);
for (const dir of mod) {
    await run(new URL(`${dir}/`, inDir));
}
await rm(outDir, { recursive: true, force: true });
