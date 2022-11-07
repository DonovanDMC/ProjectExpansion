import { genericMatter } from "../../util";

const BASE = new URL("collector.json", import.meta.url).pathname;
export default genericMatter.bind(null, "collector", BASE, []);
