import { OTHER_MATTER_TIERS } from "../../constants";
import { genericMatter } from "../../util";

const BASE = new URL("matter_block.json", import.meta.url).pathname;
export default genericMatter.bind(null, "matter_block", BASE, OTHER_MATTER_TIERS.concat("final"));
