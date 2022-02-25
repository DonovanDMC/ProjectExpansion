const { generic, FUEL_DISABLED } = require("../../util");

const BASE = `${__dirname}/fuel.json`;
module.exports = (outDir) => generic(outDir, "fuel", BASE, FUEL_DISABLED);
