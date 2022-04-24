const {execSync} = require("child_process");
const {copyFileSync, readdirSync, existsSync} = require("fs");

module.exports = function run(outDir) {
    return readdirSync(`${__dirname}/single`, {withFileTypes: true}).map(d => {
        if (d.isDirectory()) {
            return readdirSync(`${__dirname}/single/${d.name}`, {withFileTypes: true}).map(dd => {
                // stars & fuel
                if (dd.isDirectory()) return readdirSync(`${__dirname}/single/${d.name}/${dd.name}`, {withFileTypes: true}).map(ddd => {
                    if (ddd.isDirectory()) return;
                    if (!existsSync(`${outDir}/${d.name}/${dd.name}`)) execSync(`mkdir -p ${outDir}/${d.name}/${dd.name}`);
                    copyFileSync(`${__dirname}/single/${d.name}/${dd.name}/${ddd.name}`, `${outDir}/${d.name}/${dd.name}/${ddd.name}`);
                    return ["single", `${outDir}/${d.name}/${dd.name}/${ddd.name}`];
                }).reduce((a, b) => a.concat(b), []);
                if (!existsSync(`${outDir}/${d.name}`)) execSync(`mkdir -p ${outDir}/${d.name}`);
                copyFileSync(`${__dirname}/single/${d.name}/${dd.name}`, `${outDir}/${d.name}/${dd.name}`);
                return ["single", `${outDir}/${d.name}/${dd.name}`];
            }).reduce((a, b) => a.concat(b), []);
        } else {
            copyFileSync(`${__dirname}/single/${d.name}`, `${outDir}/${d.name}`);
            return ["single", `${outDir}/${d.name}`];
        }
    });
};
