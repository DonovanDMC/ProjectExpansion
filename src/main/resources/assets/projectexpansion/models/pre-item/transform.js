const { execSync } = require("child_process");
const { readdirSync, existsSync, mkdirSync, copyFileSync } = require("fs");
const { resolve } = require("path");

function copyFile(src, dst) {
    copyFileSync(src, dst);
    console.debug(src.replace(inDir, "").slice(1), "->", dst.replace(inDir, "").slice(1));
}

const inDir = `${__dirname}`;
const outDir = `${__dirname}/../item`;
if(existsSync(outDir)) execSync(`rm -rf ${outDir}`);
mkdirSync(outDir);
readdirSync(inDir, { withFileTypes: true }).forEach(d => {
    if(!d.isDirectory()) {
        if(!d.name.endsWith(".json")) return;
        copyFile(`${inDir}/${d.name}`, `${outDir}/${d.name}`);
    }
    else {
        if(d.name === "output") return;
        readdirSync(`${inDir}/${d.name}`, { withFileTypes: true }).forEach(sub => {
            if(!sub.isDirectory()) {
                if(!sub.name.endsWith(".json")) return;
                copyFile(`${inDir}/${d.name}/${sub.name}`, `${outDir}/${sub.name.replace(/\.json/, "")}_${d.name}.json`);
            }
            else {
                if(d.name === "star") {
                        readdirSync(`${inDir}/${d.name}/${sub.name}`, { withFileTypes: true }).forEach(sub2 => {
                            if(sub2.isDirectory()) console.error(`Unexpected Subdirectory in ${resolve(inDir, d.name, sub.name)} "${sub2.name}"`);
                            else {
                                if(!sub2.name.endsWith(".json")) return;
                                copyFile(`${inDir}/${d.name}/${sub.name}/${sub2.name}`, `${outDir}/${sub.name}_${d.name}_${sub2.name}`);
                            }
                        });
                } else if (d.name === "upgrade") {
                        readdirSync(`${inDir}/${d.name}/${sub.name}`, { withFileTypes: true }).forEach(sub2 => {
                            if(sub2.isDirectory()) console.error(`Unexpected Subdirectory in ${resolve(inDir, d.name, sub.name)} "${sub2.name}"`);
                            else {
                                if(!sub2.name.endsWith(".json")) return;
                                copyFile(`${inDir}/${d.name}/${sub.name}/${sub2.name}`, `${outDir}/${sub2.name.replace(/\.json/, "")}_${sub.name}_${d.name}.json`);
                            }
                        });
                } else console.error(`Unexpected Subdirectory in ${resolve(inDir, d.name)} "${sub.name}"`);
            }
        });
    }
});
