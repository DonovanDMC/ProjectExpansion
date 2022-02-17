const { mkdirSync } = require("fs");
const { genericBlock } = require("../../util");

const BASE = `${__dirname}/collector.json`;
module.exports = (outDir) => genericBlock(outDir, "collector", BASE);
