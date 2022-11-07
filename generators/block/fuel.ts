import { FUEL_DISABLED } from "../../constants";
import { genericBlockMatter } from "../../util";

const BASE = new URL("fuel.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "fuel", BASE, FUEL_DISABLED, undefined);
