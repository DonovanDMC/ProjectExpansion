const { writeFileSync, readFileSync, mkdirSync, existsSync } = require("fs");
const { resolve } = require("path");

const MATTER_TIERS = module.exports.MATTER_TIERS = [
    "basic", "dark", "red", "magenta",
    "pink", "purple", "violet", "blue",
    "cyan", "green", "lime", "yellow",
    "orange", "white", "fading", "final"
];

const STAR_TYPES = module.exports.STAR_TYPES = [
    "colossal",
    "magnum"
];

const STAR_TIERS = module.exports.STAR_TIERS = [
    "ein",
    "zwei",
    "drei",
    "vier",
    "sphere",
    "omega"
];

const UPGRADE_TYPES = module.exports.UPGRADE_TYPES = [
    "collector",
    "power_flower",
    "relay"
];

const FUEL_DISABLED = module.exports.FUEL_DISABLED = [
    "basic", "dark", "red", "fading", "final"
];

module.exports.genericBlock = function genericBlock(outDir, name, base, exclude = []) {
    if(!existsSync(`${outDir}/${name}`)) mkdirSync(`${outDir}/${name}`);
    return MATTER_TIERS.filter(tier => !exclude.includes(tier)).map(tier => (
        writeFileSync(`${outDir}/${name}/${tier}.json`, readFileSync(base).toString().replace(/\$TIER\$/g, tier)),
        [base, `${outDir}/${name}/${tier}.json`]
    ));
}

module.exports.generic = function generic(outDir, name, base, exclude = []) {
    return MATTER_TIERS.filter(tier => !exclude.includes(tier)).map(tier => (
        writeFileSync(`${outDir}/${tier}_${name}.json`, readFileSync(base).toString().replace(/\$TIER\$/g, tier)),
        [base, `${outDir}/${tier}_${name}.json`]
    ));
}

module.exports.assetsDir = resolve(`${__dirname}/../resources/assets/projectexpansion`);
module.exports.dataDir = resolve(`${__dirname}/../resources/data/projectexpansion`);
