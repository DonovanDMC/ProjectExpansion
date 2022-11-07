import { genericMatter } from "../../util";

const BASE = new URL("emc_link.json", import.meta.url).pathname;
export default genericMatter.bind(null, "emc_link", BASE, []);
