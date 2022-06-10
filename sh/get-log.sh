#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
if [ $# -eq 0 ]; then
	echo "Base Directory Is Required"
	exit 1
fi
EDIT_ID=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 64)
GITLOG_FILE=$(node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm "$DIR/../scripts/get-git-log.ts" "$1" "$EDIT_ID")
nano "$GITLOG_FILE"
node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm "$DIR/../scripts/remove-comments.ts" "$GITLOG_FILE"
OTHERLOG_FILE=$(node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm "$DIR/../scripts/get-other-log.ts" "$EDIT_ID" "$1" "$GITLOG_FILE")
nano "$OTHERLOG_FILE"
node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm "$DIR/../scripts/remove-comments.ts" "$OTHERLOG_FILE"
export GITLOG_FILE
export OTHERLOG_FILE
