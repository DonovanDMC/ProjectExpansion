import { generic } from "../../util";

const BASE = new URL("emc_link.json", import.meta.url).pathname;
export default generic.bind(null, "emc_link", BASE, []);
