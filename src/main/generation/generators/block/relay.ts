import { genericBlock } from "../../util";

const BASE = new URL("relay.json", import.meta.url).pathname;
export default genericBlock.bind(null, "relay", BASE, [], undefined);
