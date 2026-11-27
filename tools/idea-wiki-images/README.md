# idea-wiki-images

An IntelliJ IDEA plugin that puts a run button beside every `@WikiImage` method, like a test's. Running it saves just
that image, with `runWikiImages -PwikiOnly=<its path>` in the method's Gradle project.

It's its own build, with its own wrapper, since the IntelliJ Platform Gradle Plugin needs Gradle 9.

```sh
./gradlew buildPlugin
```

Then in IntelliJ, Settings → Plugins → ⚙ → Install Plugin from Disk, and pick `build/distributions/idea-wiki-images-1.0.0.zip`.

It's built against the IntelliJ installed by Toolbox, `-PideaHome=<path>` builds against one somewhere else.
