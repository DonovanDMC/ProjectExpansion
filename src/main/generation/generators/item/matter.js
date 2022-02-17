const { generic } = require("../../util");

const BASE = `${__dirname}/matter.json`;
module.exports = (outDir) => generic(outDir, "matter", BASE);
