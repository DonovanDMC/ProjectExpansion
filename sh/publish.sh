#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
if [ $# -eq 0 ]; then
	echo "Base Directory Is Required"
	exit 1
fi

cd "$DIR" || exit
. "$DIR/get-log.sh" "$1"
bun "$DIR/../scripts/publish.ts" "$1" "$GITLOG_FILE" "$OTHERLOG_FILE"
