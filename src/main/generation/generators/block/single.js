const { copyFileSync, readdirSync } = require("fs");

module.exports = function run(outDir) {
    return readdirSync(`${__dirname}/single`, { withFileTypes: true }).map(d => {
        if(d.isDirectory() || d.name === "power_flower_base.json") return [];
        copyFileSync(`${__dirname}/single/${d.name}`, `${outDir}/${d.name}`);
        return ["single", `${outDir}/${d.name}`];
    });
};
