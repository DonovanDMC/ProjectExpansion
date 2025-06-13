package cool.furry.mc.neoforge.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Input;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GUICondenserMK3Input extends PEContainerScreen<ContainerCondenserMK3Input> {
    public GUICondenserMK3Input(ContainerCondenserMK3Input container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.imageWidth = 255;
        this.imageHeight = 233;
    }

    protected ResourceLocation getTexture() {
        return Main.rl("textures/gui/condenser_mk3_input.png");
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());

        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgressScaled();
        graphics.blit(getTexture(), leftPos + 33, topPos + 10, 0, 235, progress, 10);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int x, int y) {
        //Don't render title or inventory as we don't have space
        long toDisplay = Math.min(menu.displayEmc.get(), menu.requiredEmc.get());
        Component emc = TransmutationEMCFormatter.formatEMC(toDisplay);
        graphics.drawString(font, emc, 140, 10, 0x404040, false);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        long toDisplay = Math.min(menu.displayEmc.get(), menu.requiredEmc.get());

        if (toDisplay < 1e12) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int emcLeft = 140 + leftPos;
        int emcRight = emcLeft + 110;
        int emcTop = 6 + topPos;
        int emcBottom = emcTop + 15;

        if (mouseX > emcLeft && mouseX < emcRight && mouseY > emcTop && mouseY < emcBottom) {
            setTooltipForNextRenderPass(PELang.EMC_TOOLTIP.translate(EMCHelper.formatEmc(toDisplay)));
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
