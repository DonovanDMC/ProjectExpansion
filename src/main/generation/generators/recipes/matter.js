const {mkdirSync, readFileSync, existsSync, writeFileSync} = require("fs");
const {MATTER_TIERS} = require("../../util");

const BASE = `${__dirname}/matter.json`;
const BASE_ALT = `${__dirname}/matter_alt.json`;
module.exports = function run(outDir) {
    if (!existsSync(`${outDir}/matter`)) mkdirSync(`${outDir}/matter`);
    const base = readFileSync(BASE).toString();
    const baseAlt = readFileSync(BASE_ALT).toString();
    return MATTER_TIERS.filter(tier => !["basic", "dark", "red", "magenta", "final"].includes(tier)).map((tier, index, arr) => {
        writeFileSync(`${outDir}/matter/${tier}.json`, base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
        writeFileSync(`${outDir}/matter/${tier}_alt.json`, baseAlt.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
        return [
            [BASE, `${outDir}/matter/${tier}.json`],
            [BASE_ALT, `${outDir}/matter/${tier}_alt.json`]
        ]
    }).reduce((a, b) => a.concat(b), []);
}
