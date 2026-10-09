// Minecraft version comparisons, ex: compareVersions("1.20.1", "1.20.5") < 0

function parse(version: string): Array<number> {
    if (!/^\d+(\.\d+)*$/.test(version)) {
        throw new Error(`Invalid Minecraft version "${version}"`);
    }
    return version.split(".").map(Number);
}

export function compareVersions(a: string, b: string): number {
    const left = parse(a), right = parse(b);
    for (let i = 0; i < Math.max(left.length, right.length); i++) {
        const diff = (left[i] ?? 0) - (right[i] ?? 0);
        if (diff !== 0) {
            return Math.sign(diff);
        }
    }
    return 0;
}

const COMPARATORS = {
    ">=": (diff: number): boolean => diff >= 0,
    "<=": (diff: number): boolean => diff <= 0,
    ">": (diff: number): boolean => diff > 0,
    "<": (diff: number): boolean => diff < 0,
    "=": (diff: number): boolean => diff === 0,
} satisfies Record<string, (diff: number) => boolean>;

/**
 * Checks a version against a range. A range is a set of comparisons that all need to match,
 * and alternatives can be given with "||".
 * ex: "*", "1.20.1", ">=1.17 <1.20.5", "<1.17 || >=1.21"
 */
export function satisfies(version: string, range: string): boolean {
    return range.split("||").some((alternative) => {
        const parts = alternative.trim().split(/\s+/).filter(Boolean);
        if (parts.length === 0) {
            throw new Error(`Empty version range "${range}"`);
        }
        return parts.every((part) => {
            if (part === "*") {
                return true;
            }
            const match = /^(>=|<=|>|<|=)?(.+)$/.exec(part);
            if (match === null) {
                throw new Error(`Invalid version range "${range}"`);
            }
            const operator = (match[1] ?? "=") as keyof typeof COMPARATORS;
            return COMPARATORS[operator](compareVersions(version, match[2]!));
        });
    });
}
