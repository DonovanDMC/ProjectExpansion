import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "fs/promises";

const BASE = new URL("relay.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	if (!await exists(new URL("relay/", outDir))) await mkdir(new URL("relay/", outDir), { recursive: true });
	const base = (await readFile(BASE)).toString();
	return (await Promise.all(MATTER_TIERS.filter(tier => !["basic", "dark", "red", "final"].includes(tier)).map(async(tier, index, arr) => {
		if (tier === "magenta") return; // we still need it for pink
		await writeFile(new URL(`relay/${tier}.json`, outDir), base.replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
		return [BASE, new URL(`relay/${tier}.json`, outDir).pathname];
	}))).filter(Boolean);
}
