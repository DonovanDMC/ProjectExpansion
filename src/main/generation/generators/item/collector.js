const {generic} = require("../../util");

const BASE = `${__dirname}/collector.json`;
module.exports = (outDir) => generic(outDir, "collector", BASE);
