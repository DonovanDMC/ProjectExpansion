const { mkdirSync, readFileSync, existsSync, writeFileSync } = require("fs");
const { MATTER_TIERS, FUEL_DISABLED } = require("../../util");

const BASE = `${__dirname}/fuel.json`;
const BASE_BLOCK = `${__dirname}/fuel_block.json`;
const BASE_BLOCK_REVERSE = `${__dirname}/fuel_block_reverse.json`;
module.exports = function run(outDir) {
    if(!existsSync(`${outDir}/fuel`)) mkdirSync(`${outDir}/fuel`);
    if(!existsSync(`${outDir}/fuel/item`)) mkdirSync(`${outDir}/fuel/item`);
    if(!existsSync(`${outDir}/fuel/block`)) mkdirSync(`${outDir}/fuel/block`);
    const base = readFileSync(BASE).toString();
    const baseBlock = readFileSync(BASE_BLOCK).toString();
    const baseBlockReverse = readFileSync(BASE_BLOCK_REVERSE).toString();
    return MATTER_TIERS.filter(tier => !FUEL_DISABLED.includes(tier)).map((tier, index, arr) => {
        let res = [];
        if(tier !== "magenta") {
            writeFileSync(`${outDir}/fuel/item/${tier}.json`, base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || "magenta"));
            res = [BASE, `${outDir}/fuel/${tier}.json`];
        }
        writeFileSync(`${outDir}/fuel/block/${tier}.json`, baseBlock.replace(/\$TIER\$/g, tier));
        writeFileSync(`${outDir}/fuel/block/${tier}_reverse.json`, baseBlockReverse.replace(/\$TIER\$/g, tier));
        return [
            res,
            [BASE_BLOCK, `${outDir}/fuel/${tier}.json`],
            [BASE_BLOCK_REVERSE, `${outDir}/fuel/${tier}.json`]
        ];
    }).reduce((a, b) => a.concat(b), []);
}
