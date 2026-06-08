package cool.furry.mc.neoforge.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.container.slots.PXOutputSlot;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.SearchType;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;

public class GUIArcaneTransmutationTablet extends PEContainerScreen<ContainerArcaneTransmutationTablet> {
    public static final ResourceLocation texture = Main.rl("textures/gui/arcane_transmutation_tablet.png");
    // public static final ResourceLocation texture = Main.rl("textures/gui/arcane_transmutation_tablet_layout.png");

    private final TransmutationInventory inv;
    private EditBox textBoxFilter;

    public GUIArcaneTransmutationTablet(ContainerArcaneTransmutationTablet container, Inventory invPlayer, Component title) {
        super(container, invPlayer, title);
        this.inv = container.transmutationInventory;
        this.imageWidth = 176;
        this.imageHeight = 217;
    }

    private void updateFilter(String text) {
        inv.updateFilter(text);
        Config.client.searchType.get().sync(text);
    }

    private Button previous, next, rotate, balance, search, clear;
    @Override
    public void init() {
        super.init();

        this.textBoxFilter = addWidget(new EditBox(this.font, leftPos + 7, topPos + 6, 162, 12, Component.empty()));
        textBoxFilter.setResponder(this::updateFilter);

        if (Config.client.searchType.get().autoFocus) {
            setFocused(textBoxFilter);
        }

        this.previous = addWidget(Button.builder(Component.empty(), (button) -> inv.previousPage())
                .pos(leftPos + 7, topPos + 20)
                .size(18, 18)
                .build());
        this.next = addWidget(Button.builder(Component.empty(), (button) -> inv.nextPage())
                .pos(leftPos + 151, topPos + 20)
                .size(18, 18)
                .build());
        this.rotate = addWidget(Button.builder(Component.empty(), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.ROTATE_CC : PacketArcaneTransmutationTabletSmallButton.Action.ROTATE);
                })
                .pos(leftPos - 71, topPos + 16)
                .size(9, 9)
                .build());
        this.balance = addWidget(Button.builder(Component.empty(), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.SPREAD : PacketArcaneTransmutationTabletSmallButton.Action.BALANCE);
                })
                .pos(leftPos - 71, topPos + 26)
                .size(9, 9)
                .build());
        this.search = addWidget(Button.builder(Component.empty(), (button) -> {
                    ModConfigSpec.ConfigValue<SearchType> config = Config.client.searchType;
                    config.set(config.get().next());
                    config.save();
                })
                .pos(leftPos - 71, topPos + 36)
                .size(9, 9)
                .build());
        this.clear = addWidget(Button.builder(Component.empty(), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.CLEAR_FORCE : PacketArcaneTransmutationTabletSmallButton.Action.CLEAR);
                })
                .pos(leftPos - 71, topPos + 61)
                .size(9, 9)
                .build());
        updateButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && inv.hasNextPage()) inv.nextPage();
        if (scrollY > 0 && inv.hasPreviousPage()) inv.previousPage();
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String filter = this.textBoxFilter.getValue();
        init(minecraft, width, height);
        this.textBoxFilter.setValue(filter);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        if (previous != null) {
            previous.active = inv.hasPreviousPage();
        }
        if (next != null) {
            next.active = inv.hasNextPage();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(texture, leftPos - 75, topPos + 10, 180, 32, 76, 89);
        this.textBoxFilter.render(graphics, mouseX, mouseY, partialTicks);

        renderIfHovered(graphics, mouseX, mouseY, previous, 196, 0);
        renderIfHovered(graphics, mouseX, mouseY, next, 215, 0);
        renderIfHovered(graphics, mouseX, mouseY, rotate, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, balance, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, search, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, clear, 234, 0);

        if (menu.isCrafting) { // fill in the crafting arrow
            graphics.blit(texture, leftPos - 50, topPos + 76, 177, 19, 18, 12);
        }

        if (Screen.hasShiftDown()) { // make the clear box red when hovering and holding shift
            renderIfHovered(graphics, mouseX, mouseY, clear, 234, 10);
        }
    }

    private void renderIfHovered(GuiGraphics graphics, int mouseX, int mouseY, Button button, int uOffset, int vOffset) {
        if (button.isMouseOver(mouseX, mouseY)) {
            graphics.blit(texture, button.getX(), button.getY(), uOffset, vOffset, button.getWidth(), button.getHeight());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BigInteger emc = inv.getAvailableEmc();
        String emcFmt = EMCFormat.format(emc);
        graphics.drawString(font, emcFmt, (imageWidth - font.width(emcFmt)) / 2, -9, 0xFFB5B5B5);

        if (inv.learnFlag > 0) {
            Component learned = Lang.GUI.ARCANE_TRANSMUTATION_TABLET_LEARNED.translate();
            graphics.drawString(font, learned, 170 - font.width(learned), 60, 0xFFB5B5B5, false);
            inv.learnFlag--;
        }

        if (inv.unlearnFlag > 0) {
            graphics.drawString(font, Lang.GUI.ARCANE_TRANSMUTATION_TABLET_UNLEARNED.translate(), 6, 60, 0xFFB5B5B5, false);
            inv.unlearnFlag--;
        }

        PoseStack pose = graphics.pose();
        for (PXOutputSlot slot : menu.getOutputSlots()) {
            long value = IEMCProxy.INSTANCE.getValue(slot.getItem());
            if (value <= 0) continue;
            BigInteger count = emc.equals(BigInteger.ZERO) ? emc : emc.divide(BigInteger.valueOf(value));
            String countFmt = EMCFormat.formatForceShort(count);
            pose.pushPose();
            pose.translate(slot.x + 17, slot.y + 12, 1000F);
            pose.scale(0.5F, 0.5F, 1F);
            graphics.drawString(font, countFmt, -font.width(countFmt), 0, 0xFFFFFF, true);
            pose.popPose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textBoxFilter.isFocused()) {
            //Manually make it so that hitting escape when the filter is focused will exit the focus
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                textBoxFilter.setFocused(false);
                return true;
            }
            //Otherwise have it handle the key press
            //This is where key combos and deletion is handled, and where we bypass the inventory key closing the screen
            return textBoxFilter.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouseButton) {
        if (textBoxFilter.isMouseOver(x, y)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                //Note: Clearing filter will be handled by the text box's responder
                this.textBoxFilter.setValue("");
            }
        } else if (textBoxFilter.isFocused()) {
            if (hoveredSlot == null || (!hoveredSlot.hasItem() && menu.getCarried().isEmpty())) {
                textBoxFilter.setFocused(false);
            }
        }
        return super.mouseClicked(x, y, mouseButton);
    }

    @Override
    public void removed() {
        super.removed();
        inv.learnFlag = 0;
        inv.unlearnFlag = 0;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int width = 16, height = 16,
                lockX = leftPos + menu.lockX, lockY = topPos + menu.lockY,
                consumeX = leftPos + menu.consumeX, consumeY = topPos + menu.consumeY,
                unlearnX  = leftPos + menu.unlearnX, unlearnY = topPos + menu.unlearnY;

        if (mouseX > lockX && mouseX < (lockX + width) && mouseY > lockY && mouseY < (lockY + height)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_LOCK.translate());
        } else if (mouseX > consumeX && mouseX < (consumeX + width) && mouseY > consumeY && mouseY < (consumeY + height)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_CONSUME.translate());
        } else if (mouseX > unlearnX && mouseX < (unlearnX + width) && mouseY > unlearnY && mouseY < (unlearnY + height)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_UNLEARN.translate());
        } else if (rotate.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE.translate().append(Component.literal(": ")).append(
                    Screen.hasShiftDown() ? Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE_COUNTER_CLOCKWISE.translateColored(ChatFormatting.GRAY) : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE_CLOCKWISE.translateColored(ChatFormatting.GRAY)
            ));
        } else if (balance.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Screen.hasShiftDown() ? Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SPREAD.translate() : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_BALANCE.translate());
        } else if (search.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE.translate().append(Component.literal(": ")).append(
                    Config.client.searchType.get().translateColored(ChatFormatting.GRAY)
            ));
        } else if (clear.isMouseOver(mouseX, mouseY)) {
            MutableComponent tooltip = Lang.GUI.ARCANE_TRANSMUTATION_TABLET_CLEAR.translate();
            setTooltipForNextRenderPass(tooltip);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
