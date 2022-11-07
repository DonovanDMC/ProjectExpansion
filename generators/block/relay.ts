import { genericBlockMatter } from "../../util";

const BASE = new URL("relay.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "relay", BASE, [], undefined);
