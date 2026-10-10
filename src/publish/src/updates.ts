import { links } from "./config.ts";

export type UpdatesJson = Record<string, Record<string, string>> & { promos: Record<string, string> };

// Forge's update checker format, https://docs.minecraftforge.net/en/latest/misc/updatechecker/
export function addRelease(updates: UpdatesJson, minecraft: string, modVersion: string): UpdatesJson {
    const version = `${minecraft}-${modVersion}`;
    // promos stays last
    const { promos, ...rest } = updates;
    return {
        ...rest,
        [minecraft]: { ...updates[minecraft], [version]: links.githubRelease(version) },
        promos: { ...promos, [`${minecraft}-latest`]: version, [`${minecraft}-recommended`]: version },
    };
}

// keeps the existing four space formatting
export function formatUpdates(updates: UpdatesJson): string {
    return JSON.stringify(updates, null, 4);
}
