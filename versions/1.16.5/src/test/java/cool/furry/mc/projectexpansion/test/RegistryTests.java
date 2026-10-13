package cool.furry.mc.projectexpansion.test;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// unit tests run in a live server, see MCJUnitLib
public class RegistryTests {
    @Test
    public void compactSunIsRegistered() {
        assertEquals(Main.rl("compact_sun"), Blocks.COMPACT_SUN.get().getRegistryName());
    }
}
