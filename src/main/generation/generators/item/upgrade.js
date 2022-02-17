const { readFileSync, writeFileSync } = require("fs");
const { UPGRADE_TYPES, MATTER_TIERS } = require("../../util");

const BASE = `${__dirname}/upgrade.json`;
module.exports = function run(outDir) {
    const base = readFileSync(BASE).toString();
    return UPGRADE_TYPES.map(type => MATTER_TIERS.filter(type => !["final"].includes(type)).map((tier) => (
        writeFileSync(`${outDir}/${tier}_${type}_upgrade.json`, base.replace(/\$TYPE\$/g, type).replace(/\$TIER\$/g, tier)),
        [BASE, `${outDir}/${tier}_${type}_upgrade.json`]
    ))).reduce((a,b) => a.concat(b), []);
}
