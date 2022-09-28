import { access, readFile, writeFile } from "fs/promises";

const file = process.argv[2];
if (!file || !(await access(file).then(() => true, () => false))) throw new Error("invalid file provided");
const contents = (await readFile(file)).toString().split("\n");
await writeFile(file, contents.filter(l => !l.startsWith("#")).join("\n"));
