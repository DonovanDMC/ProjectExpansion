import { genericBlockMatter } from "../../util";

const BASE = new URL("emc_link_nofilter.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "emc_link", BASE, [], "_nofilter");
