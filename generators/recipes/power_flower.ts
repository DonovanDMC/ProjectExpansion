import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const BASE = new URL("power_flower.json", import.meta.url).pathname;
const BASE_UPGRADE = new URL("power_flower_upgrade.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    if (!await exists(new URL("power_flower/", outDir))) {
        await mkdir(new URL("power_flower/", outDir), { recursive: true });
    }
    const base = await readFile(BASE, "utf8");
    const baseUpgrade = await readFile(BASE_UPGRADE, "utf8");
    return (await Promise.all(MATTER_TIERS.map(async (tier, index, arr) => {
        await writeFile(new URL(`power_flower/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier));
        const res = [[BASE, new URL(`power_flower/${tier}.json`, outDir).pathname]];
        if (tier !== "basic") {
            await writeFile(new URL(`power_flower/${tier}_upgrade.json`, outDir), baseUpgrade.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
            res.push([BASE, new URL(`power_flower/${tier}_upgrade.json`, outDir).pathname]);
        }
        return res;
    }))).flat();
}
