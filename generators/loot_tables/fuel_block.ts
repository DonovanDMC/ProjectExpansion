import { FUEL_DISABLED } from "../../constants";
import { genericMatter } from "../../util";

const BASE = new URL("fuel_block.json", import.meta.url).pathname;
export default genericMatter.bind(null, "fuel_block", BASE, FUEL_DISABLED);
