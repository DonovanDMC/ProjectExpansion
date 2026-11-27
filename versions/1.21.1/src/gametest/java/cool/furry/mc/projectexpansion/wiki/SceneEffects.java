package cool.furry.mc.projectexpansion.wiki;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

// the overworld's sky for scenes, which float in an empty sky well above the horizon
// cutout: no sky, clouds, rain or snow, so nothing's drawn behind the scene & it's left see through, time & weather still change the light
// otherwise: the normal sky, but without the sun or moon while they're under the horizon, since there's no ground to hide them
final class SceneEffects extends DimensionSpecialEffects.OverworldEffects {
    // what the level had before scenes started, put back after
    private static @Nullable DimensionSpecialEffects previous;
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png"), MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    // swapped for a blank texture while they're under the horizon
    private static final Set<ResourceLocation> HIDDEN = new HashSet<>();
    // how far past the horizon the sun & moon are fully out of sight, as part of a day
    private static final float SUN_SIZE = 0.05F, MOON_SIZE = 0.035F;
    private final boolean cutout;

    private SceneEffects(boolean cutout) {
        this.cutout = cutout;
    }

    static void apply(ClientLevel level, boolean cutout) {
        if (previous == null) previous = level.effects();
        set(level, new SceneEffects(cutout));
    }

    static void restore(ClientLevel level) {
        if (previous != null) set(level, previous);
        previous = null;
        hide(SUN, false);
        hide(MOON, false);
    }

    // releasing the blank one has the game load the real one again the next time it's used
    private static void hide(ResourceLocation texture, boolean hidden) {
        if (hidden == HIDDEN.contains(texture)) return;
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (hidden) {
            textures.register(texture, new DynamicTexture(1, 1, true));
            HIDDEN.add(texture);
        } else {
            textures.release(texture);
            HIDDEN.remove(texture);
        }
    }

    // it's final & set when the level's made, and there's nothing else that changes it
    private static void set(ClientLevel level, DimensionSpecialEffects effects) {
        try {
            for (Field field : ClientLevel.class.getDeclaredFields()) {
                if (field.getType() != DimensionSpecialEffects.class) continue;
                field.setAccessible(true);
                field.set(level, effects);
                return;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("ClientLevel has no DimensionSpecialEffects field");
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        // 0 is noon & 0.5 is midnight, so the sun's under the horizon between 0.25 & 0.75, and the moon's the other way round
        float time = level.getTimeOfDay(partialTick);
        hide(SUN, !cutout && time > 0.25F + SUN_SIZE && time < 0.75F - SUN_SIZE);
        hide(MOON, !cutout && (time < 0.25F - MOON_SIZE || time > 0.75F + MOON_SIZE));
        return cutout;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return cutout;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return cutout;
    }

    // no splashes or rain sounds either
    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return cutout;
    }
}
