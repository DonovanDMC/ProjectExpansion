import { genericMatter } from "../../util";

const BASE = new URL("relay.json", import.meta.url).pathname;
export default genericMatter.bind(null, "relay", BASE, []);
