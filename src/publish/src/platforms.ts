import { basename } from "node:path";

import { links, PROJECT, type CurseForgeDependency, type ModrinthDependency, type ReleaseType, type Version } from "./config.ts";

export interface Release {
    // markdown with linked commits, for CurseForge, Modrinth and Discord
    changelog: string;
    // markdown with bare hashes, GitHub links those itself
    githubChangelog: string;
    jar: string;
    modVersion: string;
    tag: string;
    version: Version;
}

export interface Published {
    curseforge?: number;
    github?: string;
    modrinth?: string;
}

export interface PublishedVersion {
    minecraft: string;
    published: Published;
}

const USER_AGENT = "DonovanDMC/ProjectExpansion (hewwo@yiff.rocks)";

async function check(res: Response, platform: string): Promise<Response> {
    if (!res.ok) throw new Error(`${platform}: unexpected ${res.status} ${res.statusText}: ${await res.text()}`);
    return res;
}

export function displayName(release: Release): string {
    return `[${release.version.minecraft}] ${release.modVersion}`;
}

export interface CurseForgeMetadata {
    changelog: string;
    changelogType: "markdown";
    displayName: string;
    gameVersions: Array<number>;
    relations: { projects: Array<CurseForgeDependency> };
    releaseType: ReleaseType;
}

export function curseforgeMetadata(release: Release): CurseForgeMetadata {
    return {
        changelog: release.changelog,
        changelogType: "markdown",
        displayName: displayName(release),
        gameVersions: Object.values(release.version.curseforge.gameVersions),
        releaseType: release.version.releaseType,
        relations: { projects: release.version.curseforge.dependencies },
    };
}

export async function uploadCurseForge(release: Release, token: string): Promise<number> {
    const data = new FormData();
    data.append("metadata", JSON.stringify(curseforgeMetadata(release)));
    data.append("file", Bun.file(release.jar), basename(release.jar));
    const res = await check(await fetch(`https://minecraft.curseforge.com/api/projects/${PROJECT.curseforge.id}/upload-file`, {
        method: "POST",
        headers: { "X-Api-Token": token, "User-Agent": USER_AGENT },
        body: data,
    }), "curseforge");
    return (await res.json() as { id: number }).id;
}

export interface ModrinthMetadata {
    changelog: string;
    dependencies: Array<ModrinthDependency>;
    featured: boolean;
    file_parts: Array<string>;
    game_versions: Array<string>;
    loaders: Array<string>;
    name: string;
    primary_file: string;
    project_id: string;
    status: "listed";
    version_number: string;
    version_type: ReleaseType;
}

export function modrinthMetadata(release: Release): ModrinthMetadata {
    return {
        project_id: PROJECT.modrinth.id,
        name: displayName(release),
        version_number: release.tag,
        changelog: release.changelog,
        game_versions: release.version.modrinth.gameVersions,
        version_type: release.version.releaseType,
        loaders: release.version.modrinth.loaders,
        dependencies: release.version.modrinth.dependencies,
        featured: true,
        status: "listed",
        file_parts: ["file"],
        primary_file: "file",
    };
}

export async function uploadModrinth(release: Release, token: string): Promise<string> {
    const data = new FormData();
    data.append("data", JSON.stringify(modrinthMetadata(release)));
    data.append("file", Bun.file(release.jar), basename(release.jar));
    const res = await check(await fetch("https://api.modrinth.com/v2/version", {
        method: "POST",
        headers: { "Authorization": token, "User-Agent": USER_AGENT },
        body: data,
    }), "modrinth");
    return (await res.json() as { id: string }).id;
}

export function githubReleaseBody(release: Release, published: Published): string {
    const others = [
        published.curseforge === undefined ? null : `* [CurseForge](${links.curseforgeFile(published.curseforge)})`,
        published.modrinth === undefined ? null : `* [Modrinth](${links.modrinthVersion(published.modrinth)})`,
    ].filter(line => line !== null);
    return [
        "**What's Changed**",
        release.githubChangelog,
        ...others.length === 0 ? [] : ["", "**Other Platforms**", ...others],
    ].join("\n");
}

export async function createGitHubRelease(release: Release, published: Published, token: string): Promise<string> {
    const headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": `Bearer ${token}`,
        "User-Agent": USER_AGENT,
        "X-GitHub-Api-Version": "2022-11-28",
    };
    const { owner, repo } = PROJECT.github;
    const res = await check(await fetch(`https://api.github.com/repos/${owner}/${repo}/releases`, {
        method: "POST",
        headers,
        body: JSON.stringify({
            tag_name: release.tag,
            name: displayName(release),
            body: githubReleaseBody(release, published),
            prerelease: release.version.releaseType !== "release",
        }),
    }), "github");
    const created = await res.json() as { html_url: string; id: number };
    await check(await fetch(`https://uploads.github.com/repos/${owner}/${repo}/releases/${created.id}/assets?name=${encodeURIComponent(basename(release.jar))}`, {
        method: "POST",
        headers: { ...headers, "Content-Type": "application/java-archive" },
        body: Bun.file(release.jar),
    }), "github asset");
    return created.html_url;
}

export function discordAnnouncement(modVersion: string, changelog: string, releases: Array<PublishedVersion>): string {
    const downloads = releases.map(({ minecraft, published }) => {
        const found = [
            published.curseforge === undefined ? null : `[CurseForge](<${links.curseforgeFile(published.curseforge)}>)`,
            published.modrinth === undefined ? null : `[Modrinth](<${links.modrinthVersion(published.modrinth)}>)`,
            published.github === undefined ? null : `[GitHub](<${published.github}>)`,
        ].filter(link => link !== null);
        return `* **${minecraft}**: ${found.join(" · ")}`;
    });
    // "**1.18.2**, **1.19.2** & **1.20.1**"
    const names = releases.map(({ minecraft }) => `**${minecraft}**`);
    const versions = names.length > 1 ? `${names.slice(0, -1).join(", ")} & ${names.at(-1)}` : names.join("");
    return [`New release **${modVersion}** for ${versions}`, "", "Changelog:", changelog, "", "Downloads:", ...downloads].join("\n");
}

// discord messages are capped at 2000 characters, split between lines where possible
export function splitMessage(content: string, max = 2000): Array<string> {
    const messages: Array<string> = [];
    let current = "";
    for (let line of content.split("\n")) {
        while (line.length > max) {
            if (current !== "") {
                messages.push(current);
                current = "";
            }
            messages.push(line.slice(0, max));
            line = line.slice(max);
        }
        const next = current === "" ? line : `${current}\n${line}`;
        if (next.length > max) {
            messages.push(current);
            current = line;
        } else {
            current = next;
        }
    }
    if (current !== "") messages.push(current);
    return messages;
}

export async function announceDiscord(content: string, webhook: string, botToken?: string): Promise<void> {
    for (const message of splitMessage(content)) {
        const res = await check(await fetch(`${webhook}?wait=true`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            // no pings, no embeds
            body: JSON.stringify({ content: message, allowed_mentions: { parse: [] }, flags: 1 << 2 }),
        }), "discord");
        if (botToken === undefined) continue;
        // publishes it to servers following the announcement channel
        const { channel_id, id } = await res.json() as { channel_id: string; id: string };
        await check(await fetch(`https://discord.com/api/v10/channels/${channel_id}/messages/${id}/crosspost`, {
            method: "POST",
            headers: { Authorization: `Bot ${botToken}` },
        }), "discord crosspost");
    }
}
