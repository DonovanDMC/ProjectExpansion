import { FUEL_DISABLED } from "../../constants";
import { generic } from "../../util";

const BASE = new URL("fuel.json", import.meta.url).pathname;
export default generic.bind(null, "fuel", BASE, FUEL_DISABLED);
