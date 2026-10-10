import { tmpdir } from "node:os";
import { join } from "node:path";

import { links } from "./config.ts";

import type { Commit } from "./git.ts";

// release commits only update updates.json, their message is the version
const RELEASE_COMMIT = /^\d+\.\d+\.\d+(?:-[\w.]+)?$/;
// "message (hash)" or "message (hash, hash)", with full or short hashes
const COMMIT_LINE = /^(.*) \(((?:[\da-f]{6,40})(?:,\s?[\da-f]{6,40})*)\)$/;

export function draftChangelog(commits: Array<Commit>, base: string | null): string {
    const lines = [
        "# Changelog for this release, lines starting with # are removed and an empty changelog cancels publishing.",
        "# Lines ending in (hash) link the commits for CurseForge, Modrinth and Discord, GitHub links them by itself.",
        base ? `# Commits since ${base}:` : "# No earlier release was found, add the changes yourself.",
    ];
    for (const commit of commits) {
        if (RELEASE_COMMIT.test(commit.subject)) continue;
        lines.push(`* ${commit.subject} (${commit.hash})`);
    }
    return `${lines.join("\n")}\n`;
}

export function stripComments(text: string): string {
    return text.split(/\r?\n/).filter(line => !line.startsWith("#")).join("\n").trim();
}

// GitHub links bare hashes itself, the other platforms need markdown links
export function linkCommits(changelog: string): string {
    return changelog.split("\n").map((line) => {
        const match = COMMIT_LINE.exec(line);
        if (!match) return line;
        const commits = match[2]!.split(/,\s?/).map(hash => `[${hash.slice(0, 7)}](${links.commit(hash)})`);
        return `${match[1]} (${commits.join(", ")})`;
    }).join("\n");
}

export async function editChangelog(draft: string): Promise<string> {
    const file = join(tmpdir(), `projectexpansion-changelog-${process.pid}.md`);
    await Bun.write(file, draft);
    const editor = [process.env.VISUAL, process.env.EDITOR].find(e => e !== undefined && e !== "") ?? "nano";
    const proc = Bun.spawn([...editor.split(" "), file], { stdio: ["inherit", "inherit", "inherit"] });
    if (await proc.exited !== 0) throw new Error(`${editor} exited with ${proc.exitCode}`);
    const text = stripComments(await Bun.file(file).text());
    await Bun.file(file).delete();
    return text;
}
