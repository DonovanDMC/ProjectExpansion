import type { SimpleGit, LogResult } from "simple-git";
import simpleGit from "simple-git";
import { writeFile } from "fs/promises";
import { tmpdir } from "os";

const gitDir = process.argv[2];
const id = process.argv[3];
if (!gitDir) throw new Error("git dir is required");
if (!id) throw new Error("edit id is required");

const git = simpleGit(gitDir);
await git.pull(["--tags", "--force"]);
const latestTag = (await git.raw(["describe", "--tags", "--abbrev=0"])).toString().trim();
const commits = await git.log([`${latestTag}..HEAD`]) as SimpleGit & LogResult;
const gitLog = [
	"# Log For Github"
];

for (const commit of commits.all) {
	if (/^\d+\.\d+\.\d+$/.test(commit.message)) continue;
	gitLog.push(`* ${commit.message} (${commit.hash})`);
}

await writeFile(`${tmpdir()}/${id}-gitlog`, gitLog.join("\n"));
process.stdout.write(`${tmpdir()}/${id}-gitlog`);
