#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
if [ $# -eq 0 ]; then
	echo "Base Directory Is Required"
	exit 1
fi

cd "$DIR" || exit
. "$DIR/get-log.sh" "$0"
node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm "$DIR/../scripts/publish.ts" "$0" "$GITLOG_FILE" "$OTHERLOG_FILE"
