package cool.furry.mc.neoforge.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Output;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUICondenserMK3Output extends PEContainerScreen<ContainerCondenserMK3Output> {
    public GUICondenserMK3Output(ContainerCondenserMK3Output container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.imageWidth = 382;
        this.imageHeight = 252;
    }

    protected ResourceLocation getTexture() {
        return Main.rl("textures/gui/condenser_mk3_output.png");
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());

        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}
