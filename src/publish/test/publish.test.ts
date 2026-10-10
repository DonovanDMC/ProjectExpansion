import { describe, expect, test } from "bun:test";

import { draftChangelog, linkCommits, stripComments } from "../src/changelog.ts";
import { isDevVersion, parseProperties, readSecrets, validateVersionConfig, type Version } from "../src/config.ts";
import { curseforgeMetadata, discordAnnouncement, githubReleaseBody, modrinthMetadata, splitMessage, type Release } from "../src/platforms.ts";
import { addRelease, formatUpdates, type UpdatesJson } from "../src/updates.ts";

const HASH = "0123456789abcdef0123456789abcdef01234567";

const version: Version = {
    minecraft: "1.20.1",
    enabled: true,
    releaseType: "release",
    curseforge: { gameVersions: { "Forge": 7498, "1.20.1": 9990 }, dependencies: [{ slug: "projecte", type: "requiredDependency" }] },
    modrinth: { gameVersions: ["1.20.1"], loaders: ["forge"], dependencies: [] },
};
const release: Release = {
    version,
    modVersion: "2.0.0",
    tag: "1.20.1-2.0.0",
    jar: "/tmp/projectexpansion-1.20.1-2.0.0.jar",
    changelog: "* a",
    githubChangelog: "* a",
};

describe("changelog", () => {
    test("drafts skip release commits", () => {
        const draft = draftChangelog([{ hash: HASH, subject: "Fix things" }, { hash: HASH, subject: "2.0.0" }], "1.20.1-1.1.3");
        expect(stripComments(draft)).toBe(`* Fix things (${HASH})`);
    });

    test("comments are removed", () => {
        expect(stripComments("# a\n* b\n# c\n")).toBe("* b");
    });

    test("commits are linked", () => {
        expect(linkCommits(`* Fix (${HASH})`)).toBe(`* Fix ([0123456](https://github.com/DonovanDMC/ProjectExpansion/commit/${HASH}))`);
        expect(linkCommits("* Two (abcdef1, 1234567)")).toBe("* Two ([abcdef1](https://github.com/DonovanDMC/ProjectExpansion/commit/abcdef1), [1234567](https://github.com/DonovanDMC/ProjectExpansion/commit/1234567))");
        expect(linkCommits("* Not a hash (thing)")).toBe("* Not a hash (thing)");
    });
});

describe("config", () => {
    test("properties", () => {
        const props = parseProperties("# comment\nmod_id=projectexpansion\nmod_version = 2.0.0\n");
        expect(props.get("mod_id")).toBe("projectexpansion");
        expect(props.get("mod_version")).toBe("2.0.0");
    });

    test("dev versions are recognized", () => {
        for (const version of ["2.0.0-dev.0", "2.0.0-DEV", "2.0.0.dev1", "2.0.0-dev"]) expect(isDevVersion(version)).toBe(true);
        for (const version of ["2.0.0", "2.0.0-beta.1", "2.0.0-device"]) expect(isDevVersion(version)).toBe(false);
    });

    test("version configs are validated", () => {
        const { minecraft, ...config } = version;
        expect(validateVersionConfig(minecraft, config)).toEqual(config);
        expect(() => validateVersionConfig(minecraft, { ...config, releaseType: "nope" })).toThrow("releaseType");
        expect(() => validateVersionConfig(minecraft, { ...config, modrinth: { ...config.modrinth, loaders: [] } })).toThrow("loaders");
    });

    test("only the needed secrets are required", () => {
        expect(() => readSecrets({}, ["curseforge", "discord"])).toThrow("CURSEFORGE_TOKEN, DISCORD_WEBHOOK");
        expect(readSecrets({ GITHUB_TOKEN: "a" }, ["github"]).github).toBe("a");
    });
});

describe("updates.json", () => {
    const updates = { "homepage": "h", "1.20.1": { "1.20.1-1.1.3": "x" }, "promos": { "1.20.1-latest": "1.20.1-1.1.3" } } as unknown as UpdatesJson;

    test("adds the release and promos", () => {
        const updated = addRelease(updates, "1.20.1", "2.0.0");
        expect(updated["1.20.1"]).toEqual({ "1.20.1-1.1.3": "x", "1.20.1-2.0.0": "https://github.com/DonovanDMC/ProjectExpansion/releases/tag/1.20.1-2.0.0" });
        expect(updated.promos["1.20.1-latest"]).toBe("1.20.1-2.0.0");
        expect(updated.promos["1.20.1-recommended"]).toBe("1.20.1-2.0.0");
    });

    test("new versions go before promos", () => {
        expect(Object.keys(addRelease(updates, "1.21.1", "2.0.0"))).toEqual(["homepage", "1.20.1", "1.21.1", "promos"]);
    });

    test("formats with four spaces and no trailing newline", () => {
        expect(formatUpdates(updates)).toStartWith("{\n    \"homepage\"");
        expect(formatUpdates(updates)).toEndWith("}");
    });
});

describe("platforms", () => {
    test("curseforge metadata", () => {
        const meta = curseforgeMetadata(release);
        expect(meta.displayName).toBe("[1.20.1] 2.0.0");
        expect(meta.gameVersions).toEqual([7498, 9990]);
        expect(meta.changelogType).toBe("markdown");
        expect(meta.relations.projects).toEqual([{ slug: "projecte", type: "requiredDependency" }]);
    });

    test("modrinth metadata", () => {
        const meta = modrinthMetadata(release);
        expect(meta.version_number).toBe("1.20.1-2.0.0");
        expect(meta.version_type).toBe("release");
        expect(meta.loaders).toEqual(["forge"]);
    });

    test("github body only links what was published", () => {
        expect(githubReleaseBody(release, {})).not.toContain("Other Platforms");
        const body = githubReleaseBody(release, { curseforge: 1, modrinth: "abc" });
        expect(body).toContain("https://www.curseforge.com/minecraft/mc-mods/project-expansion/files/1");
        expect(body).toContain("https://modrinth.com/mod/project-expansion/version/abc");
    });

    test("discord announcement lists every version", () => {
        const text = discordAnnouncement("2.0.0", "* a", [{ minecraft: "1.20.1", published: { curseforge: 1, github: "g" } }, { minecraft: "1.21.1", published: {} }]);
        expect(text).toContain("* **1.20.1**: [CurseForge](<https://www.curseforge.com/minecraft/mc-mods/project-expansion/files/1>) · [GitHub](<g>)");
        expect(text).toContain("* **1.21.1**: ");
    });

    test("long messages split between lines", () => {
        const parts = splitMessage(["a".repeat(6), "b".repeat(6), "c".repeat(3)].join("\n"), 10);
        expect(parts).toEqual(["aaaaaa", "bbbbbb\nccc"]);
        expect(splitMessage("x".repeat(25), 10)).toEqual(["x".repeat(10), "x".repeat(10), "x".repeat(5)]);
        for (const part of splitMessage("line\n".repeat(1000))) expect(part.length).toBeLessThanOrEqual(2000);
    });
});
