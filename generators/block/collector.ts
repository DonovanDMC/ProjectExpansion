import { genericBlockMatter } from "../../util";

const BASE = new URL("collector.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "collector", BASE, [], undefined);
