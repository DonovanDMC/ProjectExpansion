const {mkdirSync} = require("fs");
const {genericBlock} = require("../../util");

const BASE = `${__dirname}/emc_link.json`;
module.exports = (outDir) => genericBlock(outDir, "emc_link", BASE);
