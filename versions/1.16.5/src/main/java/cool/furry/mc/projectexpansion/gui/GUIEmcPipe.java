package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GUIEmcPipe extends ContainerScreen<ContainerEmcPipe> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/emc_pipe.png");
    private final Map<Button, ITextComponent> lockedTooltips = new HashMap<>();
    private Button redstone;
    private Button distribution;

    public GUIEmcPipe(ContainerEmcPipe menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        redstone = addButton(new Button(leftPos + 8, topPos + 28, 160, 20, StringTextComponent.EMPTY, button -> press(ContainerEmcPipe.BUTTON_REDSTONE), this::renderButtonTooltip));
        distribution = addButton(new Button(leftPos + 8, topPos + 50, 160, 20, StringTextComponent.EMPTY, button -> press(ContainerEmcPipe.BUTTON_DISTRIBUTION), this::renderButtonTooltip));
        updateButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void renderButtonTooltip(Button button, MatrixStack poseStack, int mouseX, int mouseY) {
        ITextComponent tooltip = lockedTooltips.get(button);
        if (tooltip != null) renderTooltip(poseStack, tooltip, mouseX, mouseY);
    }

    // the server keeps the real values, they come back through the menu's data slots
    private void updateButtons() {
        PipeUpgrade upgrade = menu.getUpgrade();
        redstone.setMessage(Lang.GUI.EMC_PIPE_REDSTONE.translate(Lang.GUI.EMC_PIPE_REDSTONE.extend(menu.getRedstoneMode().getSerializedName())));
        distribution.setMessage(Lang.GUI.EMC_PIPE_DISTRIBUTION.translate(Lang.GUI.EMC_PIPE_DISTRIBUTION.extend(menu.getDistribution().getSerializedName())));
        lock(redstone, PipeUpgrade.canChangeRedstoneMode(upgrade), PipeUpgrade.BASIC);
        lock(distribution, PipeUpgrade.canChangeDistribution(upgrade), PipeUpgrade.IMPROVED);
    }

    private void lock(Button button, boolean unlocked, PipeUpgrade needed) {
        button.active = unlocked;
        if (unlocked) lockedTooltips.remove(button);
        else lockedTooltips.put(button, Lang.GUI.EMC_PIPE_LOCKED.translate(Lang.GUI.EMC_PIPE_LOCKED.extend(needed.getSerializedName())));
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
        graphics.drawString(font, title.copy().append(" (" + menu.side.getSerializedName() + ")"), titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Lang.GUI.EMC_PIPE_RATE.translate(rate(menu.getUpgrade())), 8, 18, 0x404040, false);
        graphics.drawString(font, inventory.getDisplayName(), inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private static ITextComponent rate(@Nullable PipeUpgrade upgrade) {
        long rate = PipeUpgrade.getRate(upgrade);
        return rate == Long.MAX_VALUE ? new StringTextComponent("∞") : EMCFormat.getComponent(rate);
    }
}
