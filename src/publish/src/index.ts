import { join, relative, resolve } from "node:path";
import { parseArgs } from "node:util";

import { draftChangelog, editChangelog, linkCommits } from "./changelog.ts";
import { isDevVersion, links, loadVersions, PLATFORMS, readProperties, readSecrets, type Platform, type Secrets } from "./config.ts";
import { Git } from "./git.ts";
import {
    announceDiscord,
    createGitHubRelease,
    curseforgeMetadata,
    discordAnnouncement,
    githubReleaseBody,
    modrinthMetadata,
    uploadCurseForge,
    uploadModrinth,
    type Published,
    type PublishedVersion,
    type Release,
} from "./platforms.ts";
import { addRelease, formatUpdates, type UpdatesJson } from "./updates.ts";

const USAGE = `usage: scripts/publish.sh [options]
  --versions <list>     comma separated minecraft versions, defaults to every enabled version
  --platforms <list>    comma separated, any of ${PLATFORMS.join(", ")}, defaults to all of them
  --since <ref>         start the changelog here instead of at the last release
  --skip-build          use the jars that are already built
  --skip-git            don't commit, tag or push, for retrying uploads after the tags were pushed
  --dry-run             do everything local, print what would be uploaded instead
  -y, --yes             don't ask before publishing`;

const { values } = parseArgs({
    options: {
        "versions": { type: "string" },
        "platforms": { type: "string" },
        "since": { type: "string" },
        "skip-build": { type: "boolean" },
        "skip-git": { type: "boolean" },
        "dry-run": { type: "boolean" },
        "yes": { type: "boolean", short: "y" },
        "help": { type: "boolean", short: "h" },
    },
});
if (values.help) {
    console.log(USAGE);
    process.exit(0);
}

function fail(message: string): never {
    console.error(`error: ${message}`);
    process.exit(1);
}
const dryRun = values["dry-run"] ?? false;
const skipGit = values["skip-git"] ?? false;
const skipBuild = values["skip-build"] ?? false;
const root = resolve(import.meta.dir, "../../..");
const git = new Git(root);

// what to publish
const props = await readProperties(join(root, "gradle.properties"));
const modId = props.get("mod_id") ?? fail("mod_id is missing from gradle.properties");
const modVersion = props.get("mod_version") ?? fail("mod_version is missing from gradle.properties");
if (isDevVersion(modVersion)) {
    if (dryRun) console.warn(`warning: ${modVersion} is a dev version, it couldn't be published`);
    else fail(`${modVersion} is a dev version, set mod_version in gradle.properties first`);
}

const allVersions = await loadVersions(root);
let versions = allVersions.filter(v => v.enabled);
if (values.versions) {
    const wanted = values.versions.split(",").map(v => v.trim());
    for (const name of wanted) {
        const version = allVersions.find(v => v.minecraft === name) ?? fail(`unknown version ${name}`);
        if (!version.enabled) fail(`${name} has publishing disabled in versions/${name}/publish.json`);
    }
    versions = versions.filter(v => wanted.includes(v.minecraft));
}
if (versions.length === 0) fail("nothing to publish");

const platforms = (values.platforms?.split(",").map(p => p.trim()) ?? [...PLATFORMS]) as Array<Platform>;
for (const platform of platforms) if (!PLATFORMS.includes(platform)) fail(`unknown platform ${platform}`);
let secrets: Secrets | null = null;
try {
    secrets = readSecrets(process.env, platforms);
} catch (err) {
    if (!dryRun) fail((err as Error).message);
}

const tags = versions.map(v => `${v.minecraft}-${modVersion}`);
console.log(`Publishing ${modVersion} for ${versions.map(v => v.minecraft).join(", ")} to ${platforms.join(", ")}${dryRun ? " (dry run)" : ""}`);

// git has to match what gets tagged, problems only warn on a dry run
function problem(message: string): void {
    if (dryRun) console.warn(`warning: ${message}`);
    else fail(message);
}
await git.run("fetch", "--tags", "--quiet");
const remoteTags = await git.remoteTags();
if (skipGit) {
    for (const tag of tags) if (!remoteTags.has(tag)) problem(`--skip-git needs ${tag} to already be pushed`);
} else {
    if (!await git.isClean()) problem("the working tree has uncommitted changes");
    const upstream = await git.upstream();
    if (!upstream) problem(`${await git.branch()} has no upstream to push to`);
    else if ((await git.aheadBehind(upstream)).behind !== 0) problem(`${await git.branch()} is behind ${upstream}, pull first`);
    const localTags = await git.localTags();
    for (const tag of tags) if (localTags.has(tag) || remoteTags.has(tag)) problem(`${tag} is already tagged`);
}

// jars, the game tests have to pass for releaseJar to finish
const started = Date.now();
if (!skipBuild) {
    const tasks = versions.map(v => `:${v.minecraft}:releaseJar`);
    console.log(`Building ${tasks.join(" ")}`);
    const gradle = Bun.spawn(["./gradlew", ...tasks], { cwd: root, stdio: ["inherit", "inherit", "inherit"] });
    if (await gradle.exited !== 0) fail("the build failed");
}
const jars = new Map<string, string>();
for (const v of versions) {
    const name = `${modId}-${v.minecraft}-${modVersion}.jar`;
    const signedPath = join(root, "versions", v.minecraft, "build/libs/signed", name);
    const signed = Bun.file(signedPath), unsigned = Bun.file(join(root, "versions", v.minecraft, "build/libs", name));
    // signing quietly skips without its secrets, so check it actually happened for this jar
    if (!await signed.exists()) fail(`${relative(root, signedPath)} is missing, is scripts/secrets.sh set up for signing?`);
    if (await unsigned.exists() && signed.lastModified < unsigned.lastModified) fail(`${relative(root, signedPath)} is older than the jar it was signed from`);
    if (!skipBuild && signed.lastModified < started) fail(`${relative(root, signedPath)} wasn't rebuilt`);
    jars.set(v.minecraft, signedPath);
}

// changelog
const base = values.since ?? await git.previousRelease(versions.map(v => v.minecraft));
const commits = base === null ? [] : await git.commitsSince(base);
const githubChangelog = await editChangelog(draftChangelog(commits, base));
if (githubChangelog === "") fail("the changelog is empty, nothing was published");
const changelog = linkCommits(githubChangelog);

const releases: Array<Release> = versions.map((version, i) => ({
    changelog,
    githubChangelog,
    jar: jars.get(version.minecraft)!,
    modVersion,
    tag: tags[i]!,
    version,
}));

console.log(`\n${changelog}\n`);
for (const release of releases) console.log(`${release.tag}: ${relative(root, release.jar)} (${Math.round(Bun.file(release.jar).size / 1024)} KiB)`);
if (!values.yes && !dryRun) {
    const answer = prompt(`\nPublish ${releases.length} release${releases.length === 1 ? "" : "s"} to ${platforms.join(", ")}? [y/N]`);
    if (answer?.trim().toLowerCase() !== "y") fail("cancelled, nothing was published");
}

// updates.json, release commit and tags, the tags have to exist before the github releases
if (!skipGit) {
    const updatesFile = join(root, "updates.json");
    let updates = await Bun.file(updatesFile).json() as UpdatesJson;
    for (const release of releases) updates = addRelease(updates, release.version.minecraft, modVersion);
    if (dryRun) {
        console.log(`\nwould add to updates.json: ${tags.join(", ")}`);
        console.log(`would commit "${modVersion}", tag ${tags.join(", ")} and push`);
    } else {
        await Bun.write(updatesFile, formatUpdates(updates));
        await git.run("add", "updates.json");
        await git.run("commit", "--quiet", "-m", modVersion);
        for (const tag of tags) await git.run("tag", "-a", tag, "-m", tag);
        await git.run("push", "--quiet");
        await git.run("push", "--quiet", "origin", ...tags);
        console.log(`Pushed ${modVersion} with tags ${tags.join(", ")}`);
    }
}

// uploads, stops at the first failure since retrying is safer than guessing what got through
const results: Array<PublishedVersion> = [];
for (const [i, release] of releases.entries()) {
    const published: Published = {};
    results.push({ minecraft: release.version.minecraft, published });
    const step = async (platform: Platform, run: () => Promise<void>): Promise<void> => {
        if (!platforms.includes(platform)) return;
        try {
            await run();
        } catch (err) {
            console.error(`\n${release.tag} failed on ${platform}: ${(err as Error).message}`);
            const done = Object.keys(published);
            if (done.length !== 0) console.error(`${release.tag} is already on ${done.join(", ")}`);
            const retry = "scripts/publish.sh --skip-build --skip-git";
            console.error(`retry with: ${retry} --versions ${release.version.minecraft} --platforms ${platforms.filter(p => !done.includes(p)).join(",")}`);
            const later = releases.slice(i + 1).map(r => r.version.minecraft);
            if (later.length !== 0) console.error(`and:        ${retry} --versions ${later.join(",")} --platforms ${platforms.join(",")}`);
            process.exit(1);
        }
    };

    await step("curseforge", async () => {
        if (dryRun) {
            console.log(`\ncurseforge ${release.tag}:`, curseforgeMetadata(release));
            return;
        }
        published.curseforge = await uploadCurseForge(release, secrets!.curseforge);
        console.log(`CurseForge: ${links.curseforgeFile(published.curseforge)}`);
    });
    await step("modrinth", async () => {
        if (dryRun) {
            console.log(`\nmodrinth ${release.tag}:`, modrinthMetadata(release));
            return;
        }
        published.modrinth = await uploadModrinth(release, secrets!.modrinth);
        console.log(`Modrinth: ${links.modrinthVersion(published.modrinth)}`);
    });
    await step("github", async () => {
        if (dryRun) {
            console.log(`\ngithub ${release.tag}:\n${githubReleaseBody(release, published)}`);
            return;
        }
        published.github = await createGitHubRelease(release, published, secrets!.github);
        console.log(`GitHub: ${published.github}`);
    });
}

if (platforms.includes("discord")) {
    const announcement = discordAnnouncement(modVersion, changelog, results);
    if (dryRun) {
        console.log(`\ndiscord:\n${announcement}`);
    } else {
        await announceDiscord(announcement, secrets!.discordWebhook, secrets!.discordToken).catch((err: unknown) => fail(`the uploads worked but discord failed: ${(err as Error).message}`));
        console.log("Announced on Discord");
    }
}
console.log(dryRun ? "\nDry run finished, nothing was published" : `\nPublished ${modVersion}`);
