package cool.furry.mc.projectexpansion.gui;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.ContainerEMCRefinery;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIEMCRefinery extends AbstractContainerScreen<ContainerEMCRefinery> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/emc_refinery.png");
    // where the progress arrow is drawn, and the filled version sits on the texture
    private static final int ARROW_X = 79, ARROW_Y = 34, ARROW_U = 176, ARROW_V = 0, ARROW_WIDTH = 24, ARROW_HEIGHT = 17;
    // same for the stored emc meter, which fills from the bottom
    private static final int METER_X = 11, METER_Y = 17, METER_U = 176, METER_V = 17, METER_WIDTH = 10, METER_HEIGHT = 32;

    public GUIEMCRefinery(ContainerEMCRefinery menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(METER_X, METER_Y, METER_WIDTH, METER_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Lang.GUI.EMC_REFINERY_STORED.translate(EMCFormat.getComponent(menu.emc.get()), EMCFormat.getComponent(BlockEntityEMCRefinery.CAPACITY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int filled = (int) Math.round(menu.getProgress() * ARROW_WIDTH);
        if (filled > 0) graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, filled, ARROW_HEIGHT);
        int stored = (int) Math.ceil(Math.min(1, (double) menu.emc.get() / BlockEntityEMCRefinery.CAPACITY) * METER_HEIGHT);
        if (stored > 0) {
            int offset = METER_HEIGHT - stored;
            graphics.blit(TEXTURE, leftPos + METER_X, topPos + METER_Y + offset, METER_U, METER_V + offset, METER_WIDTH, stored);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.cost.get() > 0) graphics.drawString(font, Lang.GUI.EMC_REFINERY_COST.translate(EMCFormat.getComponent(menu.cost.get())), 30, 58, 0x404040, false);
    }
}
