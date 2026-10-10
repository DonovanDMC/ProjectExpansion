# copy to secrets.sh and fill in, secrets.sh is gitignored

# signing, used by sign-jar.sh
JAVA_SIGNING_STORE=
JAVA_SIGNING_PASS=
JAVA_SIGNING_ALIAS=

# publishing, used by publish.sh, only the platforms being published to need theirs
# https://legacy.curseforge.com/account/api-tokens
CURSEFORGE_TOKEN=
# https://modrinth.com/settings/pats, needs the create versions scope
MODRINTH_TOKEN=
# needs contents read & write on the repository
GITHUB_TOKEN=
DISCORD_WEBHOOK=
# optional, a bot that can publish in the webhook's announcement channel
DISCORD_TOKEN=
