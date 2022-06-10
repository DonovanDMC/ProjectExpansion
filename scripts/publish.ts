import config from "../config.json" assert { type: "json" };
import type { ErrorResponse, SuccessResponse } from "../types";
import simpleGit from "simple-git";
import readProperties from "properties-reader";
import { assert } from "tsafe";
import { fetch, File, FormData } from "undici";
import { access, readdir, readFile } from "fs/promises";

const exists = (input: string) => access(input).then(() => true, () => false);
const baseDir = process.argv[2];
const gitlogFile = process.argv[3];
const otherlogFile = process.argv[4];
if (!baseDir) throw new Error("base dir is required");
if (!gitlogFile || !(await exists(gitlogFile))) throw new Error(`invalid gitlog file (${gitlogFile})`);
if (!otherlogFile || !(await exists(otherlogFile))) throw new Error(`invalid otherlog file (${otherlogFile})`);

const props = readProperties(`${baseDir}/gradle.properties`);

function getProperty(name: string) {
	return props.get(name);
}

const git = simpleGit(baseDir);
await git.pull(["--tags"]);
const currentCommit = await git.revparse("HEAD");
const latestTag = (await git.raw(["describe", "--tags", "--abbrev=0"])).toString().trim();
const mcVersion = getProperty("mcVersion") as string | null;
const version = getProperty("localVersion") as string | null;
assert(mcVersion, "mcVersion isn't present");
assert(version, "version isn't present");

if (latestTag.split("-")[1] === version) throw new Error("Local Version Matches Latest");
if (!await exists(`${baseDir}/build/libs/signed`)) throw new Error("Signed Jar Is Not Present");
const files = await readdir(`${baseDir}/build/libs/signed`);
const file = files.find(f => f.includes(version));
if (files.length === 0 || !file) throw new Error("Signed Jar Is Not Present");
const gitlog = (await readFile(gitlogFile)).toString();
const otherlog = (await readFile(otherlogFile)).toString();
const fileContent = await readFile(`${baseDir}/build/libs/signed/${file}`);

await git.push();
await git.tag([`${mcVersion}-${version}`]);
await git.push(["--tags"]);
const data = new FormData();
data.append("gitlog", gitlog);
data.append("changelog", otherlog);
data.append("version", version);
data.append("expectedLatestCommit", currentCommit);
data.append("file", new File([fileContent], file));
const req = await fetch(`http://localhost:3621/publish/${config.mcVersion}`, {
	method:  "POST",
	headers: {
		Authorization: config.auth
	}
});
if (req.status !== 201) throw new Error(`Unexpected ${req.status} ${req.statusText} "${await req.text()}"`);
const body = await req.json() as SuccessResponse | ErrorResponse;
if (body.success) {
	console.log("Successfully Published");
	console.log("Curseforge:", body.data.curseforgeURL);
	console.log("Modrinth:", body.data.modrinthURL);
	console.log("Github:", body.data.gitURL);
}
