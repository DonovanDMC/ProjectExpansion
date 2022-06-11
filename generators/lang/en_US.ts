import { genericLanguage } from "../../util";

export default genericLanguage.bind(null, "en_us", new URL("en_US/", import.meta.url).pathname);
