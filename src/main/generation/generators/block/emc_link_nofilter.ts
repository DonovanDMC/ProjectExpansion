import { genericBlock } from "../../util";

const BASE = new URL("emc_link_nofilter.json", import.meta.url).pathname;
export default genericBlock.bind(null, "emc_link", BASE, [], "_nofilter");
