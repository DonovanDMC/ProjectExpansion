import { FUEL_DISABLED } from "../../constants";
import { genericBlock } from "../../util";

const BASE = new URL("fuel.json", import.meta.url).pathname;
export default genericBlock.bind(null, "fuel", BASE, FUEL_DISABLED, undefined);
