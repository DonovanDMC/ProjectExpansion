import { genericBlock } from "../../util";

const BASE = new URL("emc_link.json", import.meta.url).pathname;
export default genericBlock.bind(null, "emc_link", BASE, [], undefined);
