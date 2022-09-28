import { exists, genericBlock } from "../../util";
import { copyFile, mkdir } from "fs/promises";

const BASE = new URL("power_flower.json", import.meta.url).pathname;
export default async function run(outDir: URL) {
	if (!await exists(new URL("power_flower/", outDir))) await mkdir(new URL("power_flower/", outDir), { recursive: true });
	await copyFile(new URL("single/power_flower_base.json", import.meta.url), new URL("power_flower/base.json", outDir));
	return genericBlock("power_flower", BASE, [], undefined, outDir);
}
