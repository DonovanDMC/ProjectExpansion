import { exists } from "../../util";
import { MATTER_TIERS } from "../../constants";
import { mkdir, readFile, writeFile } from "fs/promises";

const BASE = new URL("emc_link.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	if (!await exists(new URL("emc_link/", outDir))) await mkdir(new URL("emc_link/", outDir), { recursive: true });
	const base = (await readFile(BASE)).toString();
	return MATTER_TIERS.map(async(tier, index, arr) => {
		await writeFile(new URL(`emc_link/${tier}.json`, outDir), base.replace(/\$TARGET\$/g, getMatter(tier)).replace(/\$CORE\$/g, getCore(tier)).replace(/\$TIER\$/g, tier).replace(/\$PREV\$/g, arr[index - 1]));
		return [BASE, new URL(`emc_link/${tier}.json`, outDir).pathname];
	}).filter(Boolean);
}

function getCore(tier: string) {
	switch (tier) {
		case MATTER_TIERS[0]:
			return "projecte:condenser_mk1";
		default:
			return "projectexpansion:$PREV$_emc_link";
	}
}

function getMatter(tier: string) {
	switch (tier) {
		case MATTER_TIERS[0]:
			return "projecte:transmutation_tablet";
		case MATTER_TIERS[1]:
		case MATTER_TIERS[2]:
			return "projecte:$TIER$_matter";
		case MATTER_TIERS[15]:
			return "projectexpansion:final_star_shard";
		default:
			return "projectexpansion:$TIER$_matter";
	}
}
