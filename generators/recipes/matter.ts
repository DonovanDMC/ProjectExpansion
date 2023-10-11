import { exists } from "../../util";
import { MATTER_TIERS, OTHER_MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const BASE = new URL("matter.json", import.meta.url).pathname;
const BASE_ALT = new URL("matter_alt.json", import.meta.url).pathname;
const BASE_BLOCK = new URL("matter_block.json", import.meta.url).pathname;
const BASE_BLOCK_REVERSE = new URL("matter_block_reverse.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    if (!await exists(new URL("matter/item/", outDir))) {
        await mkdir(new URL("matter/item/", outDir), { recursive: true });
    }
    if (!await exists(new URL("matter/block/", outDir))) {
        await mkdir(new URL("matter/block/", outDir), { recursive: true });
    }
    const base = (await readFile(BASE)).toString();
    const baseAlt = (await readFile(BASE_ALT)).toString();
    const baseBlock = (await readFile(BASE_BLOCK)).toString();
    const baseBlockReverse = (await readFile(BASE_BLOCK_REVERSE)).toString();
    return (await Promise.all(MATTER_TIERS.filter(tier => !OTHER_MATTER_TIERS.concat("final").includes(tier)).map(async(tier, index, arr) => {
        let res: Array<Array<string>> = [];
        if(tier !== "magenta") {
            await writeFile(new URL(`matter/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
            await writeFile(new URL(`matter/${tier}_alt.json`, outDir), baseAlt.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
            res = [
                [BASE, new URL(`matter/${tier}.json`, outDir).pathname],
                [BASE_ALT, new URL(`matter/${tier}_alt.json`, outDir).pathname]
            ];
        }
        await writeFile(new URL(`matter/block/${tier}.json`, outDir), baseBlock.replace(/\$TIER\$/g, tier));
        await writeFile(new URL(`matter/block/${tier}_reverse.json`, outDir), baseBlockReverse.replace(/\$TIER\$/g, tier));
        return [
            ...res,
            [BASE_BLOCK, new URL(`matter/block/${tier}.json`, outDir).pathname],
            [BASE_BLOCK_REVERSE, new URL(`matter/block/${tier}_reverse.json`, outDir).pathname]
        ];
    }))).flat();
}
