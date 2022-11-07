import { FUEL_DISABLED, MATTER_TIERS } from "../../constants";
import { exists } from "../../util";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const BASE = new URL("fuel.json", import.meta.url).pathname;
const BASE_BLOCK = new URL("fuel_block.json", import.meta.url).pathname;
const BASE_BLOCK_REVERSE = new URL("fuel_block_reverse.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    if (!await exists(new URL("fuel/item/", outDir))) {
        await mkdir(new URL("fuel/item/", outDir), { recursive: true });
    }
    if (!await exists(new URL("fuel/block/", outDir))) {
        await mkdir(new URL("fuel/block/", outDir), { recursive: true });
    }
    const base = (await readFile(BASE)).toString();
    const baseBlock = (await readFile(BASE_BLOCK)).toString();
    const baseBlockReverse = (await readFile(BASE_BLOCK_REVERSE)).toString();
    return (await Promise.all(MATTER_TIERS.filter(tier => !FUEL_DISABLED.includes(tier)).map(async(tier, index, arr) => {
        let res: Array<string> = [];
        if (tier !== "magenta") {
            await writeFile(new URL(`fuel/item/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || "magenta"));
            res = [BASE, new URL(`fuel/item/${tier}.json`, outDir).pathname];
        }
        await writeFile(new URL(`fuel/block/${tier}.json`, outDir), baseBlock.replace(/\$TIER\$/g, tier));
        await writeFile(new URL(`fuel/block/${tier}_reverse.json`, outDir), baseBlockReverse.replace(/\$TIER\$/g, tier));
        return [
            res,
            [BASE_BLOCK, new URL(`fuel/block/${tier}.json`, outDir).pathname],
            [BASE_BLOCK_REVERSE, new URL(`fuel/block/${tier}_reverse.json`, outDir).pathname]
        ];
    }))).flat();
}
