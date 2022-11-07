import { FUEL_DISABLED } from "../../constants";
import { genericMatter } from "../../util";

const BASE = new URL("fuel.json", import.meta.url).pathname;
export default genericMatter.bind(null, "fuel", BASE, FUEL_DISABLED);
