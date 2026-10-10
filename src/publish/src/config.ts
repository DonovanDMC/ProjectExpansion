import { readdir } from "node:fs/promises";
import { join } from "node:path";

export type ReleaseType = "release" | "beta" | "alpha";
export type Platform = "curseforge" | "modrinth" | "github" | "discord";
export const PLATFORMS: ReadonlyArray<Platform> = ["curseforge", "modrinth", "github", "discord"];

export interface CurseForgeDependency {
    slug: string;
    type: "embeddedLibrary" | "incompatible" | "optionalDependency" | "requiredDependency" | "tool";
}

export interface ModrinthDependency {
    dependency_type: "required" | "optional" | "incompatible" | "embedded";
    project_id: string;
    version_id?: string;
}

// versions/<version>/publish.json
export interface VersionConfig {
    curseforge: {
        dependencies: Array<CurseForgeDependency>;
        // name -> id, the names are only there so the ids make sense
        gameVersions: Record<string, number>;
    };
    enabled: boolean;
    modrinth: {
        dependencies: Array<ModrinthDependency>;
        gameVersions: Array<string>;
        loaders: Array<string>;
    };
    releaseType: ReleaseType;
}

export interface Version extends VersionConfig {
    minecraft: string;
}

export const PROJECT = {
    github: { owner: "DonovanDMC", repo: "ProjectExpansion" },
    curseforge: { id: 579177, slug: "project-expansion" },
    modrinth: { id: "a7yNQPzW", slug: "project-expansion" },
} as const;

const REPO = `https://github.com/${PROJECT.github.owner}/${PROJECT.github.repo}`;
export const links = {
    commit: (hash: string): string => `${REPO}/commit/${hash}`,
    githubRelease: (tag: string): string => `${REPO}/releases/tag/${tag}`,
    curseforgeFile: (id: number): string => `https://www.curseforge.com/minecraft/mc-mods/${PROJECT.curseforge.slug}/files/${id}`,
    // modrinth resolves versions by id or version number
    modrinthVersion: (idOrNumber: string): string => `https://modrinth.com/mod/${PROJECT.modrinth.slug}/version/${idOrNumber}`,
};

// 2.0.0-dev.0, 2.0.0-DEV, 2.0.0.dev1 and so on
export function isDevVersion(version: string): boolean {
    return /(?:^|[-.+_])dev(?:$|[-.+_\d])/i.test(version);
}

export function parseProperties(text: string): Map<string, string> {
    const props = new Map<string, string>();
    for (const raw of text.split(/\r?\n/)) {
        const line = raw.trim();
        if (line === "" || line.startsWith("#") || line.startsWith("!")) continue;
        const match = /^([^=:\s]+)\s*[=:]\s*(.*)$/.exec(line);
        if (match) props.set(match[1]!, match[2]!.trim());
    }
    return props;
}

export async function readProperties(file: string): Promise<Map<string, string>> {
    return parseProperties(await Bun.file(file).text());
}

export function validateVersionConfig(minecraft: string, config: unknown): VersionConfig {
    const fail = (message: string): never => {
        throw new Error(`versions/${minecraft}/publish.json: ${message}`);
    };
    if (typeof config !== "object" || config === null) fail("not an object");
    const c = config as Partial<VersionConfig>;
    if (typeof c.enabled !== "boolean") fail("enabled must be a boolean");
    if (!["release", "beta", "alpha"].includes(c.releaseType as string)) fail("releaseType must be release, beta or alpha");
    const cf = c.curseforge, mr = c.modrinth;
    if (!cf || typeof cf.gameVersions !== "object" || !Object.values(cf.gameVersions).every(id => Number.isInteger(id))) fail("curseforge.gameVersions must map names to ids");
    if (!Array.isArray(cf?.dependencies)) fail("curseforge.dependencies must be an array");
    if (!mr || !Array.isArray(mr.gameVersions) || mr.gameVersions.length === 0) fail("modrinth.gameVersions must be a non-empty array");
    if (!Array.isArray(mr?.loaders) || mr.loaders.length === 0) fail("modrinth.loaders must be a non-empty array");
    if (!Array.isArray(mr?.dependencies)) fail("modrinth.dependencies must be an array");
    return c as VersionConfig;
}

export async function loadVersions(root: string): Promise<Array<Version>> {
    const versions: Array<Version> = [];
    for (const entry of await readdir(join(root, "versions"), { withFileTypes: true })) {
        if (!entry.isDirectory()) continue;
        const file = Bun.file(join(root, "versions", entry.name, "publish.json"));
        if (!await file.exists()) continue;
        versions.push({ minecraft: entry.name, ...validateVersionConfig(entry.name, await file.json()) });
    }
    return versions.sort((a, b) => Bun.semver.order(a.minecraft, b.minecraft));
}

export interface Secrets {
    curseforge: string;
    // only used to publish the announcement in an announcement channel
    discordToken?: string;
    discordWebhook: string;
    github: string;
    modrinth: string;
}

const SECRET_NAMES: Record<Exclude<keyof Secrets, "discordToken">, [string, Platform]> = {
    curseforge: ["CURSEFORGE_TOKEN", "curseforge"],
    modrinth: ["MODRINTH_TOKEN", "modrinth"],
    github: ["GITHUB_TOKEN", "github"],
    discordWebhook: ["DISCORD_WEBHOOK", "discord"],
};

// only the secrets for the platforms being published to are required
export function readSecrets(env: Record<string, string | undefined>, platforms: ReadonlyArray<Platform>): Secrets {
    const missing: Array<string> = [];
    const secrets: Partial<Secrets> = { discordToken: env.DISCORD_TOKEN === "" ? undefined : env.DISCORD_TOKEN };
    for (const [key, [name, platform]] of Object.entries(SECRET_NAMES)) {
        const value = env[name];
        if (value) secrets[key as keyof typeof SECRET_NAMES] = value;
        else if (platforms.includes(platform)) missing.push(name);
    }
    if (missing.length !== 0) throw new Error(`missing secrets: ${missing.join(", ")} (see scripts/secrets.example.sh)`);
    return secrets as Secrets;
}
