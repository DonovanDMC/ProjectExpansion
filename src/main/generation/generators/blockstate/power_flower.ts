import { generic } from "../../util";

const BASE = new URL("power_flower.json", import.meta.url).pathname;
export default generic.bind(null, "power_flower", BASE, []);
