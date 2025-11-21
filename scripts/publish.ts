// I use a self signed cert locally
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
import config from "../config.json" assert {type: "json"};
import type {ErrorResponse, SuccessResponse} from "../types";
import simpleGit from "simple-git";
import readProperties from "properties-reader";
import {assert} from "tsafe";
import {access, readdir, readFile} from "fs/promises";
import { exec, execSync } from "child_process";
import { chdir } from "process";

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

async function execAsync(command: string, cwd?: string): Promise<string> {
    return new Promise((resolve, reject) => {
        exec(command, { cwd }, (err, stdout, stderr) => {
            if (err) return reject(err);
            if(stderr) console.error(stderr);
            if (/(?:\r?\n)+$/g.test(stdout)) stdout = stdout.replaceAll(/(?:\r?\n)+$/g, "");
            resolve(stdout);
        })
    });
}

chdir(baseDir);
await execAsync("git pull --tags");
const currentCommit = await execAsync("git rev-parse HEAD");
const latestTag = await execAsync("git describe --tags --abbrev=0 --always");
const git = simpleGit(baseDir);
const modId = getProperty("mod_id") as string | null;
const mcVersion = getProperty("minecraft_version") as string | null;
const modVersion = getProperty("mod_version") as string | null;
assert(modId, "mod_id isn't present");
assert(mcVersion, "minecraft_version isn't present");
assert(modVersion, "mod_version isn't present");

if (latestTag.split("-")[1] === modVersion) throw new Error("Local Version Matches Latest");
if (!await exists(`${baseDir}/build/libs/signed`)) throw new Error("Signed Jar Is Not Present");
const files = await readdir(`${baseDir}/build/libs/signed`);
const fileName = `${modId}-${mcVersion}-${modVersion}.jar`;
const file = files.find(f => f == fileName);
if (files.length === 0 || !file) throw new Error(`Signed Jar (${fileName}) Is Not Present`);
const gitlog = await Bun.file(gitlogFile).text();
const otherlog = await Bun.file(otherlogFile).text();
const fileContent = Bun.file(`${baseDir}/build/libs/signed/${file}`);

await execAsync("git push");
await execAsync(`git tag ${mcVersion}-${modVersion} -m ""`);
await execAsync(`git push --tags`);
const data = new FormData();
data.append("gitlog", gitlog);
data.append("changelog", otherlog);
data.append("version", modVersion);
data.append("expectedLatestCommit", currentCommit);
data.append("file", new File([fileContent], file));
const req = await fetch(`${config.endpoint}/publish/${config.gitName}`, {
	method:  "POST",
	headers: {
		Authorization: config.auth
	},
	body: data
});
if (req.status !== 201) throw new Error(`Unexpected ${req.status} ${req.statusText} "${await req.text()}"`);
const body = await req.json() as SuccessResponse | ErrorResponse;
if (body.success) {
	console.log("Successfully Published");
	console.log("Curseforge:", body.data.curseforgeURL);
	console.log("Modrinth:", body.data.modrinthURL);
	console.log("Github:", body.data.gitURL);
}
