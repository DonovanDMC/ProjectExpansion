import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "fs/promises";

const BASE = new URL("matter.json", import.meta.url).pathname;
const BASE_ALT = new URL("matter_alt.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	if (!await exists(new URL("matter/", outDir))) await mkdir(new URL("matter/", outDir), { recursive: true });
	const base = (await readFile(BASE)).toString();
	const baseAlt = (await readFile(BASE_ALT)).toString();
	return (await Promise.all(MATTER_TIERS.filter(tier => !["basic", "dark", "red", "magenta", "final"].includes(tier)).map(async(tier, index, arr) => {
		await writeFile(new URL(`matter/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
		await writeFile(new URL(`matter/${tier}_alt.json`, outDir), baseAlt.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1] || MATTER_TIERS[3]));
		return [
			[BASE, new URL(`matter/${tier}.json`, outDir).pathname],
			[BASE_ALT, new URL(`matter/${tier}_alt.json`, outDir).pathname]
		];
	}))).reduce((a, b) => a.concat(b), []);
}
