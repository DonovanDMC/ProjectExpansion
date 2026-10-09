package cool.furry.mc.projectexpansion

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

// The generator installs its packages into the shared src/generation folder, so only one can run at a time
abstract class GeneratorLock implements BuildService<BuildServiceParameters.None> {}
