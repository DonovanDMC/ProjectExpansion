const { readFileSync, writeFileSync } = require("fs");
const { STAR_TYPES, STAR_TIERS } = require("../../util");

const BASE = `${__dirname}/star.json`;
module.exports = function run(outDir) {
    const base = readFileSync(BASE).toString();
    return STAR_TYPES.map(type => STAR_TIERS.map(tier => (
        writeFileSync(`${outDir}/${type}_star_${tier}.json`, base.replace(/\$TYPE\$/g, type).replace(/\$TIER\$/g, tier)),
        [BASE, `${outDir}/${type}_star_${tier}.json`]
    ))).reduce((a,b) => a.concat(b), []);
}
