import { FUEL_TYPES } from "../../constants";
import { readFile, writeFile } from "node:fs/promises";

const BASE = new URL("fuel.json", import.meta.url).pathname;
const BASE_BLOCK = new URL("fuel_block.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
    const base = (await readFile(BASE)).toString();
    const baseBlock = (await readFile(BASE_BLOCK)).toString();
    let res: Array<Array<string>> = [];
    return (await Promise.all(FUEL_TYPES.map(async(tier, index, arr) => {
        if (tier !== "magenta") {
            await writeFile(new URL(`${tier}_fuel.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
            res.push([BASE, new URL(`${tier}_fuel.json`, outDir).pathname]);

            await writeFile(new URL(`${tier}_fuel_block.json`, outDir), baseBlock.replace(/\$TIER\$/g, tier));
            res.push([BASE_BLOCK, new URL(`${tier}_fuel_block.json`, outDir).pathname]);
        }

        return res;
    }))).flat();
}
