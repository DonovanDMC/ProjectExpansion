import { exists } from "../../util";
import { DYE_COLORS } from "../../constants";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const BASE = new URL("advanced_alchemical_chest.json", import.meta.url).pathname;
const BASE_RECOLOR = new URL("advanced_alchemical_chest_recolor.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    if (!await exists(new URL("advanced_alchemical_chest/recolor/", outDir))) {
        await mkdir(new URL("advanced_alchemical_chest/recolor/", outDir), { recursive: true });
    }
    const base = await readFile(BASE, "utf8");
    const baseRecolor = await readFile(BASE_RECOLOR, "utf8");
    return (await Promise.all(DYE_COLORS.map(async color => {
        const res: Array<[string, string]> = [];
        await writeFile(new URL(`advanced_alchemical_chest/recolor/${color}.json`, outDir), baseRecolor.replace(/\$COLOR\$/g, color));
        await writeFile(new URL(`advanced_alchemical_chest/${color}.json`, outDir), base.replace(/\$COLOR\$/g, color));
        res.push([BASE, new URL(`advanced_alchemical_chest/${color}.json`, outDir).pathname], [BASE_RECOLOR, new URL(`advanced_alchemical_chest/recolor/${color}.json`, outDir).pathname])

        return res;
    }))).flat();
}
