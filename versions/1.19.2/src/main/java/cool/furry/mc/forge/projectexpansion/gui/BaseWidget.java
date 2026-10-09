package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BaseWidget extends AbstractWidget {
    private final int uOffset, vOffset, uWidth, vHeight;
    private final ResourceLocation texture;
    public BaseWidget(ResourceLocation texture, int x, int y, int imageWidth, int imageHeight) {
        this(texture, x, y, imageWidth, imageHeight, x, y, imageWidth, imageHeight);
    }

    public BaseWidget(ResourceLocation texture, int x, int y, int imageWidth, int imageHeight, int uOffset, int vOffset, int uWidth, int vHeight) {
        super(x, y, imageWidth, imageHeight, Component.empty());
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
        this.texture = texture;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderWidget(new GuiGraphics(poseStack), mouseX, mouseY, partialTicks);
    }

    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}
}
