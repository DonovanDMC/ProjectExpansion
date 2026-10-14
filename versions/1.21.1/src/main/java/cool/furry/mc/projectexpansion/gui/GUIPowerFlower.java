package cool.furry.mc.projectexpansion.gui;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerPowerFlower;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIPowerFlower extends AbstractContainerScreen<ContainerPowerFlower> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/power_flower.png");
    private Button mode;

    public GUIPowerFlower(ContainerPowerFlower menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 90;
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(Component.empty(), button -> press(ContainerPowerFlower.BUTTON_MODE)).bounds(leftPos + 8, topPos + 52, 160, 20).build());
        updateButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // the server keeps the real mode, it comes back through the menu's data slot
    private void updateButtons() {
        mode.setMessage(Lang.GUI.POWER_FLOWER_MODE.translate(Lang.GUI.POWER_FLOWER_MODE.extend(menu.getMode().getSerializedName())));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Lang.OWNER.translate(Component.literal(menu.flower.ownerName)), 8, 18, 0x404040, false);
        graphics.drawString(font, Lang.GUI.POWER_FLOWER_STORED.translate(EMCFormat.getComponent(menu.flower.emc)), 8, 29, 0x404040, false);
        graphics.drawString(font, Lang.EMC_PER_SECOND.translate(EMCFormat.getComponent(menu.flower.getGeneratedEMC())), 8, 40, 0x404040, false);
        graphics.drawString(font, Lang.GUI.POWER_FLOWER_MODE.extend(menu.getMode().getSerializedName() + ".description"), 8, 76, 0x404040, false);
    }
}
