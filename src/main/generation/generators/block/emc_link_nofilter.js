const {genericBlock} = require("../../util");

const BASE = `${__dirname}/emc_link_nofilter.json`;
module.exports = (outDir) => genericBlock(outDir, "emc_link", BASE, [], "_nofilter");
