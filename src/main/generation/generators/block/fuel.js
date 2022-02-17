const { mkdirSync } = require("fs");
const { genericBlock, FUEL_DISABLED } = require("../../util");

const BASE = `${__dirname}/fuel.json`;
module.exports = (outDir) => genericBlock(outDir, "fuel", BASE, FUEL_DISABLED);
