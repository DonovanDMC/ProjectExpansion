import { STAR_TIERS, STAR_TYPES } from "../../constants";
import { readFile, writeFile } from "fs/promises";

const BASE = new URL("star.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	const base = (await readFile(BASE)).toString();
	return (await Promise.all(STAR_TYPES.map(type => Promise.all(STAR_TIERS.map(async(tier) => {
		await writeFile(new URL(`${type}_star_${tier}.json`, outDir), base.replace(/\$TYPE\$/g, type).replace(/\$TIER\$/g, tier));
		return [BASE, new URL(`${type}_star_${tier}.json`, outDir).pathname];
	}))))).reduce((a, b) => a.concat(b), []);
}
