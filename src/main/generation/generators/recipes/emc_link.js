const { mkdirSync, readFileSync, existsSync, writeFileSync } = require("fs");
const { MATTER_TIERS } = require("../../util");

const BASE = `${__dirname}/emc_link.json`;
module.exports = function run(outDir) {
    if(!existsSync(`${outDir}/emc_link`)) mkdirSync(`${outDir}/emc_link`);
    const base = readFileSync(BASE).toString();
    return MATTER_TIERS.filter(tier => !["basic", "dark", "red", "final"].includes(tier)).map((tier, index, arr) => {
        if(tier === "magenta") return; // we still need it for pink
        writeFileSync(`${outDir}/emc_link/${tier}.json`, base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
        return [BASE, `${outDir}/emc_link/${tier}.json`];
    }).filter(Boolean);
}
