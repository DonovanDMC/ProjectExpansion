const {mkdirSync, readFileSync, existsSync, writeFileSync} = require("fs");
const {MATTER_TIERS} = require("../../util");

const BASE = `${__dirname}/collector.json`;
const BASE_COMPRESSED = `${__dirname}/compressed_collector.json`;
module.exports = function run(outDir) {
    if (!existsSync(`${outDir}/collector`)) mkdirSync(`${outDir}/collector`);
    if (!existsSync(`${outDir}/collector/compressed`)) mkdirSync(`${outDir}/collector/compressed`);
    const base = readFileSync(BASE).toString();
    const baseCompressed = readFileSync(BASE_COMPRESSED).toString();
    return MATTER_TIERS.map((tier, index, arr) => {
        let res = [];
        if (!["basic", "dark", "red", "magenta", "final"].includes(tier)) {
            writeFileSync(`${outDir}/collector/${tier}.json`, base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
            res = [BASE, `${outDir}/collector/${tier}.json`];
        }

        writeFileSync(`${outDir}/collector/compressed/${tier}.json`, baseCompressed.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
        return [
            res,
            [BASE_COMPRESSED, `${outDir}/collector/compressed/${tier}.json`]
        ]
    }).reduce((a, b) => a.concat(b), []);
}
