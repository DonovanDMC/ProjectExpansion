const {copyFileSync, readdirSync} = require("fs");

module.exports = function run(outDir) {
	return readdirSync(`${__dirname}/single`, {withFileTypes: true}).map(d => {
		copyFileSync(`${__dirname}/single/${d.name}`, `${outDir}/${d.name}`);
		return ["single", `${outDir}/${d.name}`];
	});
};
