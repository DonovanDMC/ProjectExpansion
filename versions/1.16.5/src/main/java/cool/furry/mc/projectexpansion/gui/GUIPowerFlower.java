package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerPowerFlower;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GUIPowerFlower extends ContainerScreen<ContainerPowerFlower> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/power_flower.png");
    private Button mode;

    public GUIPowerFlower(ContainerPowerFlower menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 90;
    }

    @Override
    protected void init() {
        super.init();
        mode = addButton(new Button(leftPos + 8, topPos + 52, 160, 20, StringTextComponent.EMPTY, button -> press(ContainerPowerFlower.BUTTON_MODE)));
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
    public void tick() {
        super.tick();
        updateButtons();
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTick, int mouseX, int mouseY) {
        new GuiGraphics(poseStack, this).blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(MatrixStack poseStack, int mouseX, int mouseY) {
        GuiGraphics graphics = new GuiGraphics(poseStack, this);
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Lang.OWNER.translate(new StringTextComponent(menu.flower.ownerName)), 8, 18, 0x404040, false);
        graphics.drawString(font, Lang.GUI.POWER_FLOWER_STORED.translate(EMCFormat.getComponent(menu.flower.emc)), 8, 29, 0x404040, false);
        graphics.drawString(font, Lang.EMC_PER_SECOND.translate(EMCFormat.getComponent(menu.flower.getGeneratedEMC())), 8, 40, 0x404040, false);
        graphics.drawString(font, Lang.GUI.POWER_FLOWER_MODE.extend(menu.getMode().getSerializedName() + ".description"), 8, 76, 0x404040, false);
    }
}
