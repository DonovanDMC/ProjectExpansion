import { $ } from "bun";

export interface Commit {
    hash: string;
    subject: string;
}

export class Git {
    constructor(readonly root: string) {}

    async aheadBehind(upstream: string): Promise<{ ahead: number; behind: number }> {
        const [ahead, behind] = (await this.run("rev-list", "--left-right", "--count", `HEAD...${upstream}`)).split(/\s+/).map(Number);
        return { ahead: ahead ?? 0, behind: behind ?? 0 };
    }

    async branch(): Promise<string> {
        return this.run("rev-parse", "--abbrev-ref", "HEAD");
    }

    async commitsSince(base: string): Promise<Array<Commit>> {
        const out = await this.run("log", "--format=%H%x09%s", `${base}..HEAD`);
        return out.split("\n").filter(Boolean).map((line) => {
            const [hash, ...subject] = line.split("\t");
            return { hash: hash!, subject: subject.join("\t") };
        });
    }

    async isClean(): Promise<boolean> {
        return await this.run("status", "--porcelain") === "";
    }

    async localTags(): Promise<Set<string>> {
        return new Set((await this.run("tag", "--list")).split("\n").filter(Boolean));
    }

    // the closest earlier release of any of the versions, so the log covers everything since the last release
    async previousRelease(minecraftVersions: Array<string>): Promise<string | null> {
        let best: { distance: number; tag: string } | null = null;
        for (const version of minecraftVersions) {
            const tag = await this.run("describe", "--tags", "--abbrev=0", "--match", `${version}-*`, "HEAD").catch(() => null);
            if (!tag) continue;
            const distance = Number(await this.run("rev-list", "--count", `${tag}..HEAD`));
            if (!best || distance < best.distance) best = { distance, tag };
        }
        return best?.tag ?? null;
    }

    async remoteTags(remote = "origin"): Promise<Set<string>> {
        const out = await this.run("ls-remote", "--tags", remote);
        return new Set(out.split("\n").map(line => line.split("refs/tags/")[1]?.replace(/\^\{\}$/, "")).filter((t): t is string => !!t));
    }

    async run(...args: Array<string>): Promise<string> {
        return (await $`git ${args}`.cwd(this.root).quiet().text()).trim();
    }

    async upstream(): Promise<string | null> {
        return this.run("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{upstream}").catch(() => null);
    }
}
