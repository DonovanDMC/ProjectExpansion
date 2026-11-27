package cool.furry.mc.projectexpansion.wiki;

import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

// Jade's tooltip draws over the hud in a way that can't be cancelled, so it's turned off through Jade's own config while scenes are saved
// through reflection, since Jade's only there in dev and the mod doesn't depend on it
final class JadeTooltip {
    // Jade's setting from before, put back after
    private static @Nullable Boolean previous;

    private JadeTooltip() {}

    static void hide() {
        if (previous != null) return;
        Object general = general();
        if (general == null) return;
        try {
            previous = (Boolean) general.getClass().getMethod("shouldDisplayTooltip").invoke(general);
            set(general, false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't turn off Jade's tooltip", e);
        }
    }

    static void restore() {
        Object general = general();
        if (general != null && previous != null) set(general, previous);
        previous = null;
    }

    private static @Nullable Object general() {
        if (!ModList.get().isLoaded("jade")) return null;
        try {
            Object config = Class.forName("snownee.jade.api.config.IWailaConfig").getMethod("get").invoke(null);
            return config.getClass().getMethod("getGeneral").invoke(config);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't find Jade's config", e);
        }
    }

    private static void set(Object general, boolean display) {
        try {
            Method method = general.getClass().getMethod("setDisplayTooltip", boolean.class);
            method.invoke(general, display);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't change Jade's tooltip", e);
        }
    }
}
