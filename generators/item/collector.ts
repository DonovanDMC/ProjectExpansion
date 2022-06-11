import { generic } from "../../util";

const BASE = new URL("collector.json", import.meta.url).pathname;
export default generic.bind(null, "collector", BASE, []);
