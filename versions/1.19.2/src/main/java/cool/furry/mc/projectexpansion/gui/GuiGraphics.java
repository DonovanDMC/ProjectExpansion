package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// minecraft's GuiGraphics was added in 1.20, this covers what our screens use so they can stay the same across versions
public class GuiGraphics {
    private final PoseStack pose;
    @Nullable
    private final Screen screen;

    public GuiGraphics(PoseStack pose, @Nullable Screen screen) {
        this.pose = pose;
        this.screen = screen;
    }

    public GuiGraphics(PoseStack pose) {
        this(pose, null);
    }

    public PoseStack pose() {
        return pose;
    }

    public void blit(ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, uOffset, vOffset, uWidth, vHeight, 256, 256);
    }

    public void blit(ResourceLocation texture, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        GuiComponent.fill(pose, minX, minY, maxX, maxY, color);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public int drawString(Font font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY) {
        if (screen != null) screen.renderComponentTooltip(pose, screen.getTooltipFromItem(stack), mouseX, mouseY, stack);
    }

    public void renderTooltip(Font font, Component text, int mouseX, int mouseY) {
        if (screen != null) screen.renderTooltip(pose, text, mouseX, mouseY);
    }

    public void renderComponentTooltip(Font font, List<Component> lines, int mouseX, int mouseY) {
        if (screen != null) screen.renderTooltip(pose, lines, Optional.empty(), mouseX, mouseY, font);
    }
}
