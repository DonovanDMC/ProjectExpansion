package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Output;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GUICondenserMK3Output extends PEContainerScreen<ContainerCondenserMK3Output> {
    public GUICondenserMK3Output(ContainerCondenserMK3Output container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        this.imageWidth = 382;
        this.imageHeight = 252;
    }

    protected ResourceLocation getTexture() {
        return Main.rl("textures/gui/condenser_mk3_output.png");
    }

    @Override
    protected void renderLabels(MatrixStack poseStack, int x, int y) {
        renderLabels(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTicks, int x, int y) {
        renderBg(new GuiGraphics(poseStack, this), partialTicks, x, y);
    }

    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {

        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}
