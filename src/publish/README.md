# Publishing

Releases every enabled version in one go, with the `mod_version` from the root `gradle.properties`. Run it with `scripts/publish.sh`, which loads `scripts/secrets.sh` (see `scripts/secrets.example.sh`).

1. Checks the working tree is clean, not behind its upstream, and that the `<minecraft>-<mod version>` tags don't exist yet.
2. Runs `releaseJar` for each version, which runs the GameTests, then builds and signs the jar.
3. Opens the changelog in `$VISUAL`/`$EDITOR` (nano otherwise), drafted from the commits since the last release.
4. Adds the releases to `updates.json`, commits it as the version, tags every version and pushes.
5. Uploads each version to CurseForge, Modrinth and a GitHub release, then posts one announcement to Discord.

Each version's platform settings are in `versions/<minecraft>/publish.json`, publishing a version can be turned off there with `enabled`.

```
scripts/publish.sh --dry-run --skip-build      # check everything without changing anything
scripts/publish.sh --versions 1.20.1,1.21.1    # only some versions
scripts/publish.sh --help                      # everything else
```

If an upload fails it stops there and prints the command to retry the rest, with `--skip-git` since the tags are already pushed. A retried run only announces the versions it uploaded.
