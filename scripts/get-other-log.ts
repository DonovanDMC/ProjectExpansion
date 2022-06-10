import simpleGit from "simple-git";
import { access, readFile, writeFile } from "fs/promises";
import { tmpdir } from "os";


const gitlogFile = process.argv[2];
const id = process.argv[3];
if (!gitlogFile) throw new Error("gitlog file is required");
if (!id) throw new Error("edit id is required");
const GITLOG_LINE_REGEX = /^(.*) \(((?:([\da-f]{40})|([\da-f]{6,8})(?:,\s?)?)+)\)$/;
if (!(await access(gitlogFile).then(() => true, () => false))) throw new Error(`file "${gitlogFile}" cannot be accessed`);
const gitlog = (await readFile(gitlogFile)).toString();
const otherlog = [
	"# Log For Other Platforms"
];
const git = simpleGit(".");

for (const line of gitlog.split("\n")) {
	let m: RegExpMatchArray | null;
	if ((m = line.match(GITLOG_LINE_REGEX))) {
		const [, message, commits] = m;
		let finalMessage = `${message} (`;
		for (const commit of commits.split(/,\s?/)) {
			if (/([\da-f]{40})|([\da-f]{6,8})/.test(commit)) {
				const short = (await git.revparse(["--short", commit])).toString().trim();
				finalMessage += `[${short}](https://github.com/DonovanDMC/ProjectExpansion/commit/${commit}), `;
			}
		}
		otherlog.push(`${finalMessage.slice(0, -2)})`);
	} else otherlog.push(line);
}

await writeFile(`${tmpdir()}/${id}-otherlog`, otherlog.join("\n"));
process.stdout.write(`${tmpdir()}/${id}-otherlog`);
