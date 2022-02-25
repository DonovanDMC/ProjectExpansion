const { mkdirSync, readFileSync, existsSync, writeFileSync } = require("fs");
const { MATTER_TIERS } = require("../../util");

const BASE = `${__dirname}/relay.json`;
module.exports = function run(outDir) {
    if(!existsSync(`${outDir}/relay`)) mkdirSync(`${outDir}/relay`);
    const base = readFileSync(BASE).toString();
    return MATTER_TIERS.filter(tier => !["basic", "dark", "red", "magenta", "final"].includes(tier)).map((tier, index, arr) => {
        writeFileSync(`${outDir}/relay/${tier}.json`, base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || "basic"));
        return [BASE, `${outDir}/relay/${tier}.json`];
    });
}
