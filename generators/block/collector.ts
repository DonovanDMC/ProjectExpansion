import { genericBlock } from "../../util";

const BASE = new URL("collector.json", import.meta.url).pathname;
export default genericBlock.bind(null, "collector", BASE, [], undefined);
