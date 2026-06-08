package cool.furry.mc.neoforge.projectexpansion.util;

import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public record SearchSync(String modId, Consumer<String> action) {
    private static final Map<String, SearchSync> REGISTRY = new HashMap<>();

    public static void register(SearchSync sync) {
        REGISTRY.put(sync.modId, sync);
    }

    public static @Nullable SearchSync forMod(String modId) {
        return REGISTRY.get(modId);
    }

    public static boolean hasAny() {
        return !REGISTRY.isEmpty();
    }

    public static void syncAll(String text) {
        REGISTRY.values().forEach(sync -> sync.sync(text));
    }

    public boolean isAvailable() {
        return ModList.get().isLoaded(modId);
    }

    public void sync(String text) {
        action.accept(text);
    }
}
