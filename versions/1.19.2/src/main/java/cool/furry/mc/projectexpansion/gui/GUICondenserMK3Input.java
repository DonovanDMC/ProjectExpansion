package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Input;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUICondenserMK3Input extends PEContainerScreen<ContainerCondenserMK3Input> {
    public GUICondenserMK3Input(ContainerCondenserMK3Input container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.imageWidth = 255;
        this.imageHeight = 233;
    }

    private final DeferredTooltip deferredTooltip = new DeferredTooltip();

    protected void setTooltipForNextRenderPass(Component tooltip) {
        deferredTooltip.set(tooltip);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        deferredTooltip.render(this, poseStack, mouseX, mouseY);
    }

    protected ResourceLocation getTexture() {
        return Main.rl("textures/gui/condenser_mk3_input.png");
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int x, int y) {
        renderBg(new GuiGraphics(poseStack, this), partialTicks, x, y);
    }

    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());

        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgressScaled();
        graphics.blit(getTexture(), leftPos + 33, topPos + 10, 0, 235, progress, 10);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int x, int y) {
        renderLabels(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderLabels(GuiGraphics graphics, int x, int y) {
        //Don't render title or inventory as we don't have space
        long toDisplay = Math.min(menu.displayEmc.get(), menu.requiredEmc.get());
        Component emc = TransmutationEMCFormatter.formatEMC(toDisplay);
        graphics.drawString(font, emc, 140, 10, 0x404040, false);
    }

    @Override
    protected void renderTooltip(PoseStack poseStack, int x, int y) {
        renderTooltip(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        long toDisplay = Math.min(menu.displayEmc.get(), menu.requiredEmc.get());

        if (toDisplay < 1e12) {
            super.renderTooltip(graphics.pose(), mouseX, mouseY);
            return;
        }

        int emcLeft = 140 + leftPos;
        int emcRight = emcLeft + 110;
        int emcTop = 6 + topPos;
        int emcBottom = emcTop + 15;

        if (mouseX > emcLeft && mouseX < emcRight && mouseY > emcTop && mouseY < emcBottom) {
            setTooltipForNextRenderPass(PELang.EMC_TOOLTIP.translate(Constants.EMC_FORMATTER.format(toDisplay)));
        } else {
            super.renderTooltip(graphics.pose(), mouseX, mouseY);
        }
    }
}
