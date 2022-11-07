import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const BASE = new URL("power_flower.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    if (!await exists(new URL("power_flower/", outDir))) {
        await mkdir(new URL("power_flower/", outDir), { recursive: true });
    }
    const base = (await readFile(BASE)).toString();
    return Promise.all(MATTER_TIERS.map(async tier => {
        await writeFile(new URL(`power_flower/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier));
        return [BASE, new URL(`power_flower/${tier}.json`, outDir).pathname];
    }));
}
