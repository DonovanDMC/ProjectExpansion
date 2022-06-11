import { generic } from "../../util";

const BASE = new URL("relay.json", import.meta.url).pathname;
export default generic.bind(null, "relay", BASE, []);
