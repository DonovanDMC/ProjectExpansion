import { genericBlockMatter } from "../../util";

const BASE = new URL("emc_link.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "emc_link", BASE, [], undefined);
