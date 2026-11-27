package cool.furry.mc.projectexpansion.wiki;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// draws into a texture off screen and saves it, trimmed down to what was drawn
final class WikiRenderer {
    // image pixels per gui pixel, so every image comes out at the same size
    static final int SCALE = 4;

    private WikiRenderer() {}

    interface Drawing {
        void draw(GuiGraphics graphics);
    }

    // width & height are in gui pixels, onlyOpaque drops anything see through, like the dimmed background behind a menu
    static void save(Path file, int width, int height, boolean onlyOpaque, Drawing drawing) throws IOException {
        save(file, width, height, onlyOpaque, null, drawing);
    }

    // only keeps what's inside area (x, y, width, height in gui pixels), like a menu without the item lists beside it
    static void save(Path file, int width, int height, boolean onlyOpaque, @Nullable int[] area, Drawing drawing) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        int w = width * SCALE, h = height * SCALE;
        TextureTarget target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        NativeImage image = new NativeImage(w, h, false);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        try {
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            // the same setup the game uses for its gui, just sized to the texture
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.pushMatrix();
            modelView.translation(0, 0, -11000);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            drawing.draw(graphics);
            graphics.flush();
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0, false);
            image.flipY();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
        try (image; NativeImage trimmed = trim(image, onlyOpaque, area)) {
            Files.createDirectories(file.getParent());
            trimmed.writeToFile(file);
        }
    }

    // the game's last frame with its transparency, which a normal screenshot throws away
    // anything not drawn over is left see through, since the game clears to a transparent color
    static NativeImage readFrame() {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        NativeImage image = new NativeImage(target.width, target.height, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);
        image.flipY();
        return image;
    }

    // a frame from readFrame trimmed down to what was drawn, crop is {x, y, width, height} in its pixels to cut it to instead
    // see through things like glass & water are blended over the black it was cleared to, so their color's taken back out of that
    static void saveCutout(Path file, NativeImage image, int @Nullable [] crop) throws IOException {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setPixelRGBA(x, y, unblend(image.getPixelRGBA(x, y)));
        }
        try (NativeImage trimmed = crop == null ? trim(image, false, null) : crop(image, crop)) {
            Files.createDirectories(file.getParent());
            trimmed.writeToFile(file);
        }
    }

    private static NativeImage crop(NativeImage image, int[] area) {
        int x0 = Math.max(0, area[0]), y0 = Math.max(0, area[1]);
        int x1 = Math.min(image.getWidth(), area[0] + area[2]), y1 = Math.min(image.getHeight(), area[1] + area[3]);
        NativeImage cropped = new NativeImage(x1 - x0, y1 - y0, false);
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) cropped.setPixelRGBA(x - x0, y - y0, image.getPixelRGBA(x, y));
        }
        return cropped;
    }

    // a color blended over black, as it was before
    private static int unblend(int pixel) {
        int alpha = pixel >>> 24;
        if (alpha == 0) return 0;
        if (alpha == 255) return pixel;
        int color = 0;
        for (int shift = 0; shift < 24; shift += 8) color |= Math.min(255, (pixel >> shift & 0xFF) * 255 / alpha) << shift;
        return alpha << 24 | color;
    }

    // what onlyOpaque keeps, which drops the dark backdrop behind a menu (at most 0xD0) but keeps tooltips (0xF0)
    private static final int OPAQUE = 0xE0;

    private static NativeImage trim(NativeImage image, boolean onlyOpaque, @Nullable int[] area) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getPixelRGBA(x, y) >>> 24;
                boolean outside = area != null && (x < area[0] * SCALE || y < area[1] * SCALE || x >= (area[0] + area[2]) * SCALE || y >= (area[1] + area[3]) * SCALE);
                if (outside || (onlyOpaque ? alpha < OPAQUE : alpha == 0)) {
                    image.setPixelRGBA(x, y, 0);
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) throw new IllegalStateException("Nothing was drawn");
        NativeImage trimmed = new NativeImage(maxX - minX + 1, maxY - minY + 1, false);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) trimmed.setPixelRGBA(x - minX, y - minY, image.getPixelRGBA(x, y));
        }
        return trimmed;
    }

    /*********
     * Parts *
     *********/

    // vanilla's window, a black outline with rounded corners, light top & left edges and dark bottom & right ones
    static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        graphics.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + h - 3, 0xFFFFFFFF);
        graphics.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    // vanilla's slot, dark top & left edges and a light bottom & right
    static void slot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + size, y + size, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B8B8B);
    }

    // spacing for panels: padding inside the edge, a slot's size, and the gap between parts
    static final int PAD = 4, SLOT = 18, GAP = 6;
    static final int ARROW_WIDTH = 22, ARROW_HEIGHT = 15;

    static void arrow(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 5, x + 15, y + 10, 0xFF8B8B8B);
        for (int i = 0; i < 8; i++) graphics.fill(x + 15 + i, y + i, x + 16 + i, y + ARROW_HEIGHT - i, 0xFF8B8B8B);
    }

    static void item(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    // an item drawn bigger than in an inventory, scale 1 is the normal 16 pixels
    static void bigItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }
}
