const {readFileSync, writeFileSync} = require("fs");
const {UPGRADE_TYPES} = require("../../util");

const BASE = `${__dirname}/upgrade.json`;
module.exports = function run(outDir) {
	const base = readFileSync(BASE).toString();
	return UPGRADE_TYPES.map(type => (
		writeFileSync(`${outDir}/${type}_upgrade.json`, base.replace(/\$TYPE\$/g, type)),
			[BASE, `${outDir}/${type}_upgrade.json`]
	));
}
