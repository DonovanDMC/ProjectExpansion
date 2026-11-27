package cool.furry.mc.projectexpansion.wiki;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// everything that moves on its own, held still while images are saved, so the same image comes out every time
// animated textures (like water) are put back on their first frame and stop, and the enchantment glint stops
final class WikiFreeze {
    // what was stopped, started again after
    private static final List<Tickable> STOPPED = new ArrayList<>();
    private static double glintSpeed = -1;

    private WikiFreeze() {}

    static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (glintSpeed < 0) glintSpeed = mc.options.glintSpeed().get();
        mc.options.glintSpeed().set(0D);
        Set<Tickable> tickable = tickable(mc.getTextureManager());
        for (AbstractTexture texture : textures(mc.getTextureManager()).values()) {
            if (!(texture instanceof TextureAtlas atlas)) continue;
            if (tickable.remove(atlas)) STOPPED.add(atlas);
            firstFrame(atlas);
        }
    }

    static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (glintSpeed >= 0) mc.options.glintSpeed().set(glintSpeed);
        glintSpeed = -1;
        tickable(mc.getTextureManager()).addAll(STOPPED);
        STOPPED.clear();
    }

    // each animated sprite back on its first frame, with its ticker counting from there
    private static void firstFrame(TextureAtlas atlas) {
        RenderSystem.bindTexture(atlas.getId());
        for (Object ticker : (List<?>) get(TextureAtlas.class, "animatedTextures", atlas)) {
            Object sprite = get(ticker.getClass(), "this$0", ticker), spriteTicker = get(ticker.getClass(), "val$spriteticker", ticker);
            ((TextureAtlasSprite) sprite).uploadFirstFrame();
            set(spriteTicker.getClass(), "frame", spriteTicker, 0);
            set(spriteTicker.getClass(), "subFrame", spriteTicker, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Tickable> tickable(TextureManager textures) {
        return (Set<Tickable>) get(TextureManager.class, "tickableTextures", textures);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, AbstractTexture> textures(TextureManager textures) {
        return (Map<Object, AbstractTexture>) get(TextureManager.class, "byPath", textures);
    }

    // none of these are public
    private static Object get(Class<?> type, String name, Object instance) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't get " + type.getSimpleName() + "." + name, e);
        }
    }

    private static void set(Class<?> type, String name, Object instance, int value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't set " + type.getSimpleName() + "." + name, e);
        }
    }
}
