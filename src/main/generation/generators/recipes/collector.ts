import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "fs/promises";

const BASE = new URL("collector.json", import.meta.url).pathname;
const BASE_COMPRESSED = new URL("compressed_collector.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	if (!await exists(new URL("collector/compressed/", outDir))) await mkdir(new URL("collector/compressed/", outDir), { recursive: true });
	const base = (await readFile(BASE)).toString();
	const baseCompressed = (await readFile(BASE_COMPRESSED)).toString();
	return (await Promise.all(MATTER_TIERS.map(async(tier, index, arr) => {
		let res: Array<string> = [];
		if (!["basic", "dark", "red", "magenta", "final"].includes(tier)) {
			await writeFile(new URL(`collector/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
			res = [BASE, new URL(`collector/${tier}.json`, outDir).pathname];
		}

		await writeFile(new URL(`collector/compressed/${tier}.json`, outDir), baseCompressed.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
		return [
			res,
			[BASE_COMPRESSED, new URL(`collector/compressed/${tier}.json`, outDir).pathname]
		];
	}))).reduce((a, b) => a.concat(b), []);
}
