import { generic } from "../../util";

const BASE = new URL("matter.json", import.meta.url).pathname;
export default generic.bind(null, "matter", BASE, []);
