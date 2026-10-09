// Values shared by the templates, these mirror the enums in the mod

export const MOD_ID = "projectexpansion";

export const MATTER = [
    "basic", "dark", "red", "magenta",
    "pink", "purple", "violet", "blue",
    "cyan", "green", "lime", "yellow",
    "orange", "white", "fading", "final",
] as const;

// basic has no item, dark & red come from ProjectE and final uses the final star shard
export const MATTER_ITEMS = MATTER.filter(tier => !["basic", "dark", "red", "final"].includes(tier));
// matter blocks exist for every tier that has a matter item
export const MATTER_BLOCKS = MATTER_ITEMS;
export const FUEL = MATTER.filter(tier => !["basic", "dark", "red", "fading", "final"].includes(tier));

export const STAR_TYPES = ["magnum", "colossal", "gargantuan"] as const;
export const STAR_TIERS = ["ein", "zwei", "drei", "vier", "sphere", "omega"] as const;

export const DYES = [
    "white", "orange", "magenta", "light_blue",
    "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue",
    "brown", "green", "red", "black",
] as const;

// a matter tier's item, using ProjectE's or the final star shard where we don't have one
export function matterItem(tier: string): string {
    switch (tier) {
        case "dark":
        case "red": {
            return `projecte:${tier}_matter`;
        }
        case "final": {
            return `${MOD_ID}:final_star_shard`;
        }
        default: {
            return `${MOD_ID}:${tier}_matter`;
        }
    }
}
