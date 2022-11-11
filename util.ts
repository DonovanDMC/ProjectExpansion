import {DYE_COLORS, MATTER_TIERS} from "./constants";
import type { PathLike } from "fs";
import {
	access,
	copyFile,
	mkdir,
	readdir,
	readFile,
	writeFile
} from "fs/promises";
import { dirname } from "path";

export const exists = (input: PathLike) => access(input).then(() => true, () => false);
export async function genericBlock(values: Array<string>, replacer: RegExp, name: string, base: string, exclude: Array<string>, extra: string | undefined, outDir: URL): Promise<Array<[string, string]>> {
	if (!await exists(new URL(`${name}/`, outDir))) await mkdir(new URL(`${name}/`, outDir), { recursive: true });
	return Promise.all(values.filter(tier => !exclude.includes(tier)).map(async(type) => {
		await writeFile(new URL(`${name}/${type}${!extra ? "" : extra}.json`, outDir), (await readFile(base)).toString().replace(replacer, type));
		return [base, new URL(`${name}/${type}${!extra ? "" : extra}.json`, outDir).pathname];
	}));
}
export async function genericBlockMatter(name: string, base: string, exclude: Array<string>, extra: string | undefined, outDir: URL): Promise<Array<[string, string]>> {
	return genericBlock(MATTER_TIERS, /\$TIER\$/g, name, base, exclude, extra, outDir);
}
export async function genericBlockColor(name: string, base: string, exclude: Array<string>, extra: string | undefined, outDir: URL): Promise<Array<[string, string]>> {
	return genericBlock(DYE_COLORS, /\$COLOR\$/g, name, base, exclude, extra, outDir);
}

export async function generic(values: Array<string>, replacer: RegExp, name: string, base: string, exclude: Array<string>, outDir: URL): Promise<Array<[string, string]>> {
	return Promise.all(values.filter(tier => !exclude.includes(tier)).map(async (type) => {
		await writeFile(new URL(`${type}_${name}.json`, outDir), (await readFile(base)).toString().replace(replacer, type));
		return [base, new URL(`${type}_${name}.json`, outDir).pathname];
	}));
}

export async function genericMatter(name: string, base: string, exclude: Array<string>, outDir: URL): Promise<Array<[string, string]>> {
	return generic(MATTER_TIERS, /\$TIER\$/g, name, base, exclude, outDir);
}

export async function genericColor(name: string, base: string, exclude: Array<string>, outDir: URL): Promise<Array<[string, string]>> {
	return generic(DYE_COLORS, /\$COLOR\$/g, name, base, exclude, outDir);
}

export async function genericLanguage(name: string, dir: string, outDir: URL): Promise<Array<[string, string]>> {
	const lang = {};

	async function read(d: string) {
		const r = await readdir(d, { withFileTypes: true });
		for (const c of r) {
			if (c.isDirectory()) await read(`${d}/${c.name}/`);
			else Object.assign(lang, JSON.parse((await readFile(`${d}/${c.name}`)).toString()));
		}
	}

	await read(dir);
	await writeFile(new URL(`${name}.json`, outDir), JSON.stringify(lang, null, "\t"));
	return [[name, new URL(`${name}.json`, outDir).pathname]];
}

// @TODO for the love of everything that is holy make this shit dynamic
export async function single(currentDir: string, outDir: URL): Promise<Array<[string, string]>> {
	currentDir = `${dirname(currentDir)}/`;
	const final: Array<[string, string]> = [];
	const dir1 = await readdir(new URL("single/", currentDir), { withFileTypes: true });
	for (const d of dir1) {
		if (currentDir.endsWith("block/") && d.name === "power_flower_base.json") continue;
		if (d.isDirectory()) {
			const dir2 = await readdir(new URL(`single/${d.name}/`, currentDir), { withFileTypes: true });
			for (const dd of dir2) {
				if (dd.isDirectory()) {
					const dir3 = await readdir(new URL(`single/${d.name}/${dd.name}/`, currentDir), { withFileTypes: true });
					for (const ddd of dir3) {
						if (ddd.isDirectory()) throw new Error(`Nested Directory "${new URL(`single/${d.name}/${dd.name}/${ddd.name}/`, currentDir).pathname}" Is Too Deep.`);
						if (!await exists(new URL(`${d.name}/${dd.name}/`, outDir))) await mkdir(new URL(`${d.name}/${dd.name}/`, outDir), { recursive: true });
						await copyFile(new URL(`single/${d.name}/${dd.name}/${ddd.name}`, currentDir), new URL(`${d.name}/${dd.name}/${ddd.name}`, outDir));
						final.push(["single", new URL(`${d.name}/${dd.name}/${ddd.name}`, outDir).pathname]);
					}
				} else {
					if (!await exists(new URL(`${d.name}/`, outDir))) await mkdir(new URL(`${d.name}/`, outDir), { recursive: true });
					await copyFile(new URL(`single/${d.name}/${dd.name}`, currentDir), new URL(`${d.name}/${dd.name}`, outDir));
					final.push(["single", new URL(`${d.name}/${dd.name}`, outDir).pathname]);
				}
			}
		} else {
			await copyFile(new URL(`single/${d.name}`, currentDir), new URL(`${d.name}`, outDir));
			final.push(["single", new URL(`${d.name}`, outDir).pathname]);
		}
	}

	return final;
}