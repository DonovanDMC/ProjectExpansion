const {mkdirSync, readFileSync, existsSync, writeFileSync} = require("fs");
const {MATTER_TIERS} = require("../../util");

const BASE = `${__dirname}/emc_link.json`;
module.exports = function run(outDir) {
	if (!existsSync(`${outDir}/emc_link`)) mkdirSync(`${outDir}/emc_link`);
	const base = readFileSync(BASE).toString();
	return MATTER_TIERS.map((tier, index, arr) => {
		writeFileSync(`${outDir}/emc_link/${tier}.json`, base.replace(/\$TARGET\$/g, getMatter(tier)).replace(/\$CORE\$/g, getCore(tier)).replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
		return [BASE, `${outDir}/emc_link/${tier}.json`];
	}).filter(Boolean);
}

function getCore(tier) {
	switch (tier) {
		case MATTER_TIERS[0]:
			return "projecte:condenser_mk1";
		default:
			return "projectexpansion:$PREV$_emc_link";
	}
}

function getMatter(tier) {
	switch (tier) {
		case MATTER_TIERS[0]:
			return "projecte:transmutation_tablet";
		case MATTER_TIERS[1]:
		case MATTER_TIERS[2]:
			return "projecte:$TIER$_matter";
		case MATTER_TIERS[15]:
			return "projectexpansion:final_star_shard"
		default:
			return "projectexpansion:$TIER$_matter";
	}
}