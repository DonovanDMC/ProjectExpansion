const {generic} = require("../../util");

const BASE = `${__dirname}/emc_link.json`;
module.exports = (outDir) => generic(outDir, "emc_link", BASE);
