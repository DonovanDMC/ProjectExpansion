const {mkdirSync, readFileSync, existsSync, writeFileSync} = require("fs");
const {MATTER_TIERS} = require("../../util");

const BASE = `${__dirname}/power_flower.json`;
module.exports = function run(outDir) {
	if (!existsSync(`${outDir}/power_flower`)) mkdirSync(`${outDir}/power_flower`);
	const base = readFileSync(BASE).toString();
	return MATTER_TIERS.map((tier, index, arr) => {
		writeFileSync(`${outDir}/power_flower/${tier}.json`, base.replace(/\$TIER\$/g, tier));
		return [BASE, `${outDir}/power_flower/${tier}.json`];
	});
}
