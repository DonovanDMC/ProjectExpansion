const {generic} = require("../../util");

const BASE = `${__dirname}/relay.json`;
module.exports = (outDir) => generic(outDir, "relay", BASE);
