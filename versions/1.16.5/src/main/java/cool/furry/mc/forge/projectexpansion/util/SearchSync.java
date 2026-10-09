package cool.furry.mc.forge.projectexpansion.util;

import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class SearchSync {
    private final String modId;
    private final Consumer<String> action;

    public SearchSync(String modId, Consumer<String> action) {
        this.modId = modId;
        this.action = action;
    }

    public String modId() {
        return modId;
    }

    public Consumer<String> action() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchSync that = (SearchSync) o;
        return Objects.equals(modId, that.modId) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, action);
    }

    @Override
    public String toString() {
        return "SearchSync[modId=" + modId + ", action=" + action + "]";
    }

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
