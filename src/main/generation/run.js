const {execSync} = require("child_process");
const {readdirSync, existsSync, mkdirSync, rmdirSync} = require("fs");

const inDir = `${__dirname}/generators`;
const outDir = `${__dirname}/out`;
if (existsSync(outDir)) execSync(`rm -rf ${outDir}`);

mkdirSync(outDir);

function run(dir) {
	const {format, outDir: out} = require(`${dir}/index.js`);
	if (existsSync(out)) execSync(`rm -rf ${out}`);
	execSync(`mkdir -p ${out}`);
	readdirSync(dir, {withFileTypes: true}).forEach(gen => {
		if (gen.isDirectory() || !gen.name.endsWith(".js") || gen.name === "index.js") return;
		const res = (require(`${dir}/${gen.name}`))(out).filter(arr => arr.length > 0);
		for (const [input, output] of res) {
			// console.log(format.trim(), input.replace(__dirname, "").slice(1), `${dir}/${gen.name}`.replace(__dirname, "").slice(1), output.replace(assetsDir, "").slice(1).replace(/\.json/, ""));
			if (dir.endsWith("lang")) console.log(format.trim(), gen.name, output.split("/").slice(-1)[0].replace(/\.json/, ""));
			else console.log(format.trim(), input.replace(dir, "").replace(/\//, ""), gen.name, output.replace(out, "").slice(1).replace(/\.json/, ""));
		}
	})
}

readdirSync(inDir).map(dir => run(`${inDir}/${dir}`));
rmdirSync(outDir);