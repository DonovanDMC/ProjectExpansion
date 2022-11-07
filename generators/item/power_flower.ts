import { genericMatter } from "../../util";

const BASE = new URL("power_flower.json", import.meta.url).pathname;
export default genericMatter.bind(null, "power_flower", BASE, []);
