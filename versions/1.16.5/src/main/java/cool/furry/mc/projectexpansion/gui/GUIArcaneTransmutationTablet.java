package cool.furry.mc.projectexpansion.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.inventory.PXCraftingContainer;
import cool.furry.mc.projectexpansion.gui.container.slots.PXCraftingSlot;
import cool.furry.mc.projectexpansion.gui.container.slots.PXOutputSlot;
import cool.furry.mc.projectexpansion.gui.container.slots.PXResultSlot;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletSelectRecipe;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.SearchType;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class GUIArcaneTransmutationTablet extends PEContainerScreen<ContainerArcaneTransmutationTablet> {
    public static final ResourceLocation texture = Main.rl("textures/gui/arcane_transmutation_tablet.png");
    // public static final ResourceLocation texture = Main.rl("textures/gui/arcane_transmutation_tablet_layout.png");

    private static final int PICKER_COLUMNS = 4;
    private static final int PICKER_ROWS = 3;

    private final TransmutationInventory inv;
    private TextFieldWidget textBoxFilter;
    // shown under the crafting panel when the grid matches recipes with different outputs
    private List<RecipeOption> recipeOptions = ImmutableList.of();
    private List<ItemStack> lastCraftingItems = ImmutableList.of();
    private int lastCraftingWidth = 0;
    private int recipeScroll = 0;

    private static final class RecipeOption {
        private final ResourceLocation id;
        private final ItemStack result;

        public RecipeOption(ResourceLocation id, ItemStack result) {
            this.id = id;
            this.result = result;
        }

        public ResourceLocation id() {
            return id;
        }

        public ItemStack result() {
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecipeOption that = (RecipeOption) o;
            return Objects.equals(id, that.id) && Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, result);
        }

        @Override
        public String toString() {
            return "RecipeOption[id=" + id + ", result=" + result + "]";
        }
    }

    public GUIArcaneTransmutationTablet(ContainerArcaneTransmutationTablet container, PlayerInventory invPlayer, ITextComponent title) {
        super(container, invPlayer, title);
        this.inv = container.transmutationInventory;
        this.imageWidth = 176;
        this.imageHeight = 217;
    }

    private final DeferredTooltip deferredTooltip = new DeferredTooltip();

    protected void setTooltipForNextRenderPass(ITextComponent tooltip) {
        deferredTooltip.set(tooltip);
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        deferredTooltip.render(this, poseStack, mouseX, mouseY);
    }

    private void updateFilter(String text) {
        String search = text.trim().toLowerCase(Locale.ROOT);
        if (!inv.filter.equals(search)) {
            inv.filter = search;
            inv.searchpage = 0;
            inv.updateClientTargets();
        }
        Config.client.searchType.get().sync(text);
    }

    // ProjectE's inventory only has these helpers in newer versions
    private boolean hasPreviousPage() {
        return inv.searchpage > 0;
    }

    private boolean hasNextPage() {
        return inv.getKnowledgeSize() > 12;
    }

    private void previousPage() {
        if (hasPreviousPage()) {
            inv.searchpage--;
            inv.updateClientTargets();
        }
    }

    private void nextPage() {
        if (hasNextPage()) {
            inv.searchpage++;
            inv.updateClientTargets();
        }
    }

    private Button previous, next, rotate, balance, search, clear;
    @Override
    public void init() {
        super.init();

        this.textBoxFilter = addWidget(new TextFieldWidget(this.font, leftPos + 7, topPos + 6, 162, 12, new StringTextComponent("")));
        textBoxFilter.setResponder(this::updateFilter);

        if (Config.client.searchType.get().autoFocus) {
            setFocused(textBoxFilter);
        }

        this.previous = addWidget(new Button(leftPos + 7, topPos + 20, 18, 18, new StringTextComponent(""), (button) -> previousPage()));
        this.next = addWidget(new Button(leftPos + 151, topPos + 20, 18, 18, new StringTextComponent(""), (button) -> nextPage()));
        this.rotate = addWidget(new Button(leftPos - 71, topPos + 16, 9, 9, new StringTextComponent(""), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.ROTATE_CC : PacketArcaneTransmutationTabletSmallButton.Action.ROTATE);
                }));
        this.balance = addWidget(new Button(leftPos - 71, topPos + 26, 9, 9, new StringTextComponent(""), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.SPREAD : PacketArcaneTransmutationTabletSmallButton.Action.BALANCE);
                }));
        this.search = addWidget(new Button(leftPos - 71, topPos + 36, 9, 9, new StringTextComponent(""), (button) -> {
                    ForgeConfigSpec.ConfigValue<SearchType> config = Config.client.searchType;
                    config.set(config.get().next());
                    config.save();
                }));
        this.clear = addWidget(new Button(leftPos - 71, topPos + 61, 9, 9, new StringTextComponent(""), (button) -> {
                    menu.action(Screen.hasShiftDown() ? PacketArcaneTransmutationTabletSmallButton.Action.CLEAR_FORCE : PacketArcaneTransmutationTabletSmallButton.Action.CLEAR);
                }));
        updateButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!recipeOptions.isEmpty() && getPickerArea().contains((int) mouseX, (int) mouseY)) {
            int maxScroll = Math.max(0, (recipeOptions.size() + PICKER_COLUMNS - 1) / PICKER_COLUMNS - PICKER_ROWS);
            recipeScroll = MathHelper.clamp(recipeScroll - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        if (scrollY < 0) nextPage();
        if (scrollY > 0) previousPage();
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String filter = this.textBoxFilter.getValue();
        init(minecraft, width, height);
        this.textBoxFilter.setValue(filter);
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
        updateRecipeOptions();
    }

    private void updateRecipeOptions() {
        PXCraftingContainer input = menu.getCraftingInput();
        if (input.getWidth() == lastCraftingWidth && listMatches(input.getItems(), lastCraftingItems)) return;
        lastCraftingItems = input.getItems().stream().map(ItemStack::copy).collect(Collectors.toList());
        lastCraftingWidth = input.getWidth();

        List<RecipeOption> options = new ArrayList<>();
        for (ICraftingRecipe recipe : menu.getMatchingRecipes()) {
            ItemStack result = recipe.assemble(input);
            // recipes with the same output aren't worth choosing between
            if (result.isEmpty() || options.stream().anyMatch(option -> ItemStack.matches(option.result(), result))) continue;
            options.add(new RecipeOption(recipe.getId(), result));
        }
        recipeOptions = options.size() > 1 ? options : ImmutableList.of();
        recipeScroll = 0;
    }

    private static boolean listMatches(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private int getPickerRows() {
        return Math.min(PICKER_ROWS, (recipeOptions.size() + PICKER_COLUMNS - 1) / PICKER_COLUMNS);
    }

    private Rectangle2d getPickerArea() {
        return new Rectangle2d(leftPos - 75, topPos + 100, 76, getPickerRows() * 18 + 4);
    }

    @Nullable
    private RecipeOption getHoveredOption(double mouseX, double mouseY) {
        if (recipeOptions.isEmpty()) return null;
        Rectangle2d area = getPickerArea();
        int column = MathHelper.floor((mouseX - area.getX() - 2) / 18);
        int row = MathHelper.floor((mouseY - area.getY() - 2) / 18);
        if (column < 0 || column >= PICKER_COLUMNS || row < 0 || row >= getPickerRows()) return null;
        int index = (row + recipeScroll) * PICKER_COLUMNS + column;
        return index < recipeOptions.size() ? recipeOptions.get(index) : null;
    }

    private void renderRecipePicker(GuiGraphics graphics, int mouseX, int mouseY) {
        if (recipeOptions.isEmpty()) return;
        Rectangle2d area = getPickerArea();
        int x = area.getX(), y = area.getY(), width = area.getWidth(), height = area.getHeight();
        // colors are taken from the crafting panel's texture so they match
        graphics.fill(x, y, x + width, y + height, 0xFF0B0B0B);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF313131);
        graphics.fill(x + 1, y + 1, x + width - 2, y + height - 2, 0xFF7C7C7C);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF5A5A5A);

        ItemStack current = menu.getResultItem();
        RecipeOption hovered = getHoveredOption(mouseX, mouseY);
        for (int i = 0; i < getPickerRows() * PICKER_COLUMNS; i++) {
            int index = i + recipeScroll * PICKER_COLUMNS;
            if (index >= recipeOptions.size()) break;
            RecipeOption option = recipeOptions.get(index);
            int slotX = x + 2 + (i % PICKER_COLUMNS) * 18;
            int slotY = y + 2 + (i / PICKER_COLUMNS) * 18;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF212121);
            graphics.fill(slotX + 1, slotY + 1, slotX + 18, slotY + 18, 0xFF7C7C7C);
            graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF4E4E4E);
            graphics.renderItem(option.result(), slotX + 1, slotY + 1);
            graphics.renderItemDecorations(font, option.result(), slotX + 1, slotY + 1);
            if (ItemStack.matches(option.result(), current)) graphics.renderOutline(slotX, slotY, 18, 18, 0xFFFFFFFF);
            if (option == hovered) renderSlotHighlight(graphics.pose(), slotX + 1, slotY + 1);
        }
    }

    private void updateButtons() {
        if (previous != null) {
            previous.active = hasPreviousPage();
        }
        if (next != null) {
            next.active = hasNextPage();
        }
    }

    // the crafting panel and recipe picker sit outside of the main gui bounds, so recipe viewers need to be told about them
    public List<Rectangle2d> getExtraAreas() {
        List<Rectangle2d> areas = new ArrayList<>();
        areas.add(new Rectangle2d(leftPos - 75, topPos + 10, 76, 89));
        if (!recipeOptions.isEmpty()) areas.add(getPickerArea());
        return areas;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        // clicking the crafting panel or recipe picker shouldn't drop the held item
        for (Rectangle2d area : getExtraAreas()) {
            if (area.contains((int) mouseX, (int) mouseY)) return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTicks, int x, int y) {
        renderBg(new GuiGraphics(poseStack, this), partialTicks, x, y);
    }

    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(texture, leftPos - 75, topPos + 10, 180, 32, 76, 89);
        this.textBoxFilter.render(graphics.pose(), mouseX, mouseY, partialTicks);

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

        renderRecipePicker(graphics, mouseX, mouseY);
    }

    private void renderIfHovered(GuiGraphics graphics, int mouseX, int mouseY, Button button, int uOffset, int vOffset) {
        if (button.isMouseOver(mouseX, mouseY)) {
            graphics.blit(texture, button.x, button.y, uOffset, vOffset, button.getWidth(), button.getHeight());
        }
    }

    // renderSlot is private before 1.20.2, so this is drawn from renderLabels which runs right after the slots
    private void renderSlotOverlay(GuiGraphics graphics, Slot slot) {
        MatrixStack pose = graphics.pose();
        if (slot instanceof PXCraftingSlot && slot.hasItem()) {
            int ghosts = menu.getGhostCount(slot.getSlotIndex());
            if (ghosts <= 0) return;
            pose.pushPose();
            // above the item but below the count
            pose.translate(0, 0, 190);
            if (ghosts >= slot.getItem().getCount()) {
                // nothing in the slot has been paid for, so fade the whole item
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xA04E4E4E);
            } else {
                // otherwise show how many of them are ghost items
                pose.translate(slot.x, slot.y, 100);
                pose.scale(0.5F, 0.5F, 1F);
                graphics.drawString(font, String.valueOf(ghosts), 1, 1, 0xFF55FFFF, true);
            }
            pose.popPose();
        } else if (slot instanceof PXResultSlot && slot.hasItem() && !menu.canAffordCraft()) {
            pose.pushPose();
            pose.translate(0, 0, 190);
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FF0000);
            pose.popPose();
        }
    }

    @Override
    public List<ITextComponent> getTooltipFromItem(ItemStack stack) {
        List<ITextComponent> tooltip = new ArrayList<>(super.getTooltipFromItem(stack));
        if (hoveredSlot instanceof PXCraftingSlot && menu.getGhostCount(((PXCraftingSlot) hoveredSlot).getSlotIndex()) > 0) {
            PXCraftingSlot slot = ((PXCraftingSlot) hoveredSlot);
            int index = slot.getSlotIndex();
            tooltip.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_GHOST.translateColored(TextFormatting.AQUA, menu.getGhostCount(index), EMCFormat.format(menu.getGhostCost(index))));
        } else if (hoveredSlot instanceof PXResultSlot && !menu.canAffordCraft()) {
            tooltip.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_CANNOT_AFFORD.translateColored(TextFormatting.RED));
        }
        return tooltip;
    }

    @Override
    protected void renderLabels(MatrixStack poseStack, int x, int y) {
        renderLabels(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            renderSlotOverlay(graphics, slot);
        }
        BigInteger emc = inv.getAvailableEmc();
        String emcFmt = EMCFormat.format(emc);
        graphics.drawString(font, emcFmt, (imageWidth - font.width(emcFmt)) / 2, -9, 0xFFB5B5B5);

        if (inv.learnFlag > 0) {
            ITextComponent learned = Lang.GUI.ARCANE_TRANSMUTATION_TABLET_LEARNED.translate();
            graphics.drawString(font, learned, 170 - font.width(learned), 60, 0xFFB5B5B5, false);
            inv.learnFlag--;
        }

        if (inv.unlearnFlag > 0) {
            graphics.drawString(font, Lang.GUI.ARCANE_TRANSMUTATION_TABLET_UNLEARNED.translate(), 6, 60, 0xFFB5B5B5, false);
            inv.unlearnFlag--;
        }

        MatrixStack pose = graphics.pose();
        for (PXOutputSlot slot : menu.getOutputSlots()) {
            long value = ProjectEAPI.getEMCProxy().getValue(slot.getItem());
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
                textBoxFilter.setFocus(false);
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
        RecipeOption option = getHoveredOption(x, y);
        if (option != null && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            PacketHandler.sendToServer(new PacketArcaneTransmutationTabletSelectRecipe(option.id()));
            Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (textBoxFilter.isMouseOver(x, y)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                //Note: Clearing filter will be handled by the text box's responder
                this.textBoxFilter.setValue("");
            }
        } else if (textBoxFilter.isFocused()) {
            if (hoveredSlot == null || (!hoveredSlot.hasItem() && Objects.requireNonNull(minecraft).player.inventory.getCarried().isEmpty())) {
                textBoxFilter.setFocus(false);
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
    protected void renderTooltip(MatrixStack poseStack, int x, int y) {
        renderTooltip(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            super.renderTooltip(graphics.pose(), mouseX, mouseY);
            return;
        }

        RecipeOption option = getHoveredOption(mouseX, mouseY);
        if (option != null) {
            graphics.renderTooltip(font, option.result(), mouseX, mouseY);
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
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE.translate().append(new StringTextComponent(": ")).append(
                    Screen.hasShiftDown() ? Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE_COUNTER_CLOCKWISE.translateColored(TextFormatting.GRAY) : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_ROTATE_CLOCKWISE.translateColored(TextFormatting.GRAY)
            ));
        } else if (balance.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Screen.hasShiftDown() ? Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SPREAD.translate() : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_BALANCE.translate());
        } else if (search.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE.translate().append(new StringTextComponent(": ")).append(
                    Config.client.searchType.get().translateColored(TextFormatting.GRAY)
            ));
        } else if (clear.isMouseOver(mouseX, mouseY)) {
            IFormattableTextComponent tooltip = Lang.GUI.ARCANE_TRANSMUTATION_TABLET_CLEAR.translate();
            setTooltipForNextRenderPass(tooltip);
        } else {
            super.renderTooltip(graphics.pose(), mouseX, mouseY);
        }
    }

    // AbstractContainerScreen.renderSlotHighlight was added in 1.17
    private void renderSlotHighlight(MatrixStack pose, int x, int y) {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        fillGradient(pose, x, y, x + 16, y + 16, 0x80FFFFFF, 0x80FFFFFF);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }
}
