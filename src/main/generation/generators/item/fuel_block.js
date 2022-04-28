const {generic, FUEL_DISABLED} = require("../../util");

const BASE = `${__dirname}/fuel_block.json`;
module.exports = (outDir) => generic(outDir, "fuel_block", BASE, FUEL_DISABLED);
