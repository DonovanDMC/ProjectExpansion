const { mkdirSync, copyFileSync, existsSync } = require("fs");
const { genericBlock} = require("../../util");

const BASE = `${__dirname}/power_flower.json`;
module.exports = function run(outDir) {
    if(!existsSync(`${outDir}/power_flower`)) mkdirSync(`${outDir}/power_flower`);
    copyFileSync(`${__dirname}/single/power_flower_base.json`, `${outDir}/power_flower/base.json`);
    return genericBlock(outDir, "power_flower", BASE);
};
