package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// minecraft's GuiGraphics was added in 1.20, this covers what our screens use so they can stay the same across versions
public class GuiGraphics {
    private final MatrixStack pose;
    @Nullable
    private final Screen screen;

    public GuiGraphics(MatrixStack pose, @Nullable Screen screen) {
        this.pose = pose;
        this.screen = screen;
    }

    public GuiGraphics(MatrixStack pose) {
        this(pose, null);
    }

    public MatrixStack pose() {
        return pose;
    }

    public void blit(ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(texture);
        AbstractGui.blit(pose, x, y, uOffset, vOffset, uWidth, vHeight, 256, 256);
    }

    public void blit(ResourceLocation texture, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(texture);
        AbstractGui.blit(pose, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        AbstractGui.fill(pose, minX, minY, maxX, maxY, color);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, String text, int x, int y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(FontRenderer font, IReorderingProcessor text, float x, float y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(FontRenderer font, ItemStack stack, int mouseX, int mouseY) {
        if (screen != null) screen.renderComponentTooltip(pose, screen.getTooltipFromItem(stack), mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, ITextComponent text, int mouseX, int mouseY) {
        if (screen != null) screen.renderTooltip(pose, text, mouseX, mouseY);
    }

    public void renderComponentTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY) {
        if (screen != null) screen.renderComponentTooltip(pose, lines, mouseX, mouseY);
    }
}
