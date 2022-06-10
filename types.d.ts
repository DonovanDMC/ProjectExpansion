export interface SuccessResponse {
	success: true;
	data: {
		curseforgeID: number;
		curseforgeURL: string;
		modrinthID: string;
		modrinthURL: string;
		gitURL: string;
	};
}
export interface ErrorResponse {
	success: false;
	error: string | Array<string>;
	data: {
		gitlogID: string | null;
		changelogID: string | null;
	};
}
