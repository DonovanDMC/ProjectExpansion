import { OTHER_MATTER_TIERS } from "../../constants";
import { genericBlockMatter } from "../../util";

const BASE = new URL("matter.json", import.meta.url).pathname;
export default genericBlockMatter.bind(null, "matter", BASE, OTHER_MATTER_TIERS.concat("final"), undefined);
