import { genericMatter } from "../../util";

const BASE = new URL("collector.json", import.meta.url).pathname;
export default genericMatter.bind(null, "compressed_collector", BASE, []);
