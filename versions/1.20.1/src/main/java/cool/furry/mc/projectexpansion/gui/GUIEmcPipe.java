package cool.furry.mc.projectexpansion.gui;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class GUIEmcPipe extends AbstractContainerScreen<ContainerEmcPipe> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/emc_pipe.png");
    private Button redstone;
    private Button distribution;

    public GUIEmcPipe(ContainerEmcPipe menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(Platform.empty(), button -> press(ContainerEmcPipe.BUTTON_REDSTONE)).bounds(leftPos + 8, topPos + 28, 160, 20).build());
        distribution = addRenderableWidget(Button.builder(Platform.empty(), button -> press(ContainerEmcPipe.BUTTON_DISTRIBUTION)).bounds(leftPos + 8, topPos + 50, 160, 20).build());
        updateButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // the server keeps the real values, they come back through the menu's data slots
    private void updateButtons() {
        PipeUpgrade upgrade = menu.getUpgrade();
        redstone.setMessage(Lang.GUI.EMC_PIPE_REDSTONE.translate(Lang.GUI.EMC_PIPE_REDSTONE.extend(menu.getRedstoneMode().getSerializedName())));
        distribution.setMessage(Lang.GUI.EMC_PIPE_DISTRIBUTION.translate(Lang.GUI.EMC_PIPE_DISTRIBUTION.extend(menu.getDistribution().getSerializedName())));
        lock(redstone, PipeUpgrade.canChangeRedstoneMode(upgrade), PipeUpgrade.BASIC);
        lock(distribution, PipeUpgrade.canChangeDistribution(upgrade), PipeUpgrade.IMPROVED);
    }

    private static void lock(Button button, boolean unlocked, PipeUpgrade needed) {
        button.active = unlocked;
        button.setTooltip(unlocked ? null : Tooltip.create(Lang.GUI.EMC_PIPE_LOCKED.translate(Lang.GUI.EMC_PIPE_LOCKED.extend(needed.getSerializedName()))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title.copy().append(" (" + menu.side.getSerializedName() + ")"), titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Lang.GUI.EMC_PIPE_RATE.translate(rate(menu.getUpgrade())), 8, 18, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private static Component rate(@Nullable PipeUpgrade upgrade) {
        long rate = PipeUpgrade.getRate(upgrade);
        return rate == Long.MAX_VALUE ? Platform.literal("∞") : EMCFormat.getComponent(rate);
    }
}
