import secrets from "./secrets.json" assert { type: "json" };
import MCVersion from "./MCVersion";
import { Octokit } from "@octokit/rest";
import simpleGit from "simple-git";
import fetch from "node-fetch";
import readProperties from "properties-reader";
import FormData from "form-data";
import { assert } from "tsafe";
import { execSync } from "child_process";
import { copyFile, readFile, unlink } from "fs/promises";

const __dirname = new URL(".", import.meta.url).pathname.slice(0, -1);
const props = readProperties(`${__dirname}/../../gradle.properties`);
function getProperty(name: string) { return props.get(name); }
const git = simpleGit(".");
const octo = new Octokit({ auth: secrets.gitAuth });
const latestTag = (await git.raw(["describe", "--tags", "--abbrev=0"])).trim();
const commits = await git.log([`${latestTag}..HEAD`]);
const cfProjectID = getProperty("cfProjectID") as number | null;
const branch = (await git.revparse(["--abbrev-ref", "HEAD"])).trim();
const mcVersion = getProperty("mcVersion") as string | null;
const version = getProperty("localVersion") as string | null;
assert(cfProjectID !== null, "cfProjectID is null");
assert(mcVersion !== null, "mcVersion is null");
assert(version !== null, "version is null");
const gitLog: Array<string> = [];
const otherLog: Array<string> = [];

for (const commit of commits.all) {
	if (/^\d+\.\d+\.\d+$/.test(commit.message)) continue;
	gitLog.push(`* ${commit.message} (${commit.hash})`);
	const shortHash = (await git.revparse(["--short", commit.hash])).trim();
	otherLog.push(`* ${commit.message} ([${shortHash}](https://github.com/DonovanDMC/ProjectExpansion/commit/${commit.hash}))`);
}
const files = execSync(`ls ${__dirname}/../../build/libs/signed`).toString().trim().split("\n").map(f => f.trim());
if (files.length === 0) throw new Error("Signed Jar Is Not Present");
// if (files[0].includes(latestTag)) throw new Error("Version Has Not Been Bumped");
await copyFile(`${__dirname}/../../build/libs/signed/${files[0]}`, `${__dirname}/${files[0]}`);

const cfData = new FormData();
const file = await readFile(`${__dirname}/${files[0]}`);
cfData.append("metadata", JSON.stringify({
	changelog: otherLog.join("\n"),
	changeLogType: "markdown",
	displayName: `[${branch}] ${version}`,
	gameVersions: [
		MCVersion.CF_FORGE,
		MCVersion["CF_1.15"],
		MCVersion["CF_1.15.1"],
		MCVersion["CF_1.15.2"]
	],
	releaseType: "release",
	relations: {
		projects: [
			{
				slug: "projecte",
				type: "requiredDependency"
			}
		]
	}
}));
cfData.append("file", file, {
	contentType: "application/jar",
	filename: files[0]
});
const cfUpload = await fetch(`https://minecraft.curseforge.com/api/projects/${cfProjectID}/upload-file`, {
	method: "POST",
	headers: {
		"X-Api-Token": secrets.cfAuth
	},
	body: cfData
});
if (cfUpload.status !== 200) throw new Error(`Unexpected ${cfUpload.status} ${cfUpload.statusText} "${await cfUpload.text()}"`);
const { id: cfID } = await cfUpload.json() as { id: number; };
console.log("CurseForge Uploaded File: https://www.curseforge.com/minecraft/mc-mods/project-expansion/files/%d", cfID);

// @TODO modrinth uploading

await git.push();
await git.tag([`${mcVersion}-${version}`]);
await git.push(["--tags"]);
const release = await octo.repos.createRelease({
	owner: "DonovanDMC",
	repo: "ProjectExpansion",
	tag_name: `${mcVersion}-${version}`,
	name: `[${branch}] ${version}`,
	body: [
		"**What's Changed**",
		...gitLog,
		"",
		"**Other Platforms**",
		`* [CurseForge](https://www.curseforge.com/minecraft/mc-mods/project-expansion/files/${cfID})`,
		`* [Modrinth](https://modrinth.com/mod/project-expansion/version/${mcVersion}-${version})`
	].join("\n"),
	draft: false,
	target_commitish: branch
});
console.log("Github Release: %s", release.data.html_url);

execSync(`gh release upload ${mcVersion}-${version} ${__dirname}/${files[0]} --repo DonovanDMC/ProjectExpansion --clobber`);
console.log("File Uploaded To Release");
await unlink(files[0]);