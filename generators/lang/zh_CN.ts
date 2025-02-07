import { genericLanguage } from "../../util";

export default genericLanguage.bind(null, "zh_cn", new URL("zh_CN/", import.meta.url).pathname);
