const { mkdirSync } = require("fs");
const { genericBlock } = require("../../util");

const BASE = `${__dirname}/relay.json`;
module.exports = (outDir) => genericBlock(outDir, "relay", BASE);
