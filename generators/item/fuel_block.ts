import { FUEL_DISABLED } from "../../constants";
import { generic } from "../../util";

const BASE = new URL("fuel_block.json", import.meta.url).pathname;
export default generic.bind(null, "fuel_block", BASE, FUEL_DISABLED);
