import { genericMatter } from "../../util";

const BASE = new URL("matter.json", import.meta.url).pathname;
export default genericMatter.bind(null, "matter", BASE, []);
