package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketCreateTeleportLocation;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketDeleteTeleportLocation;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketTeleportBack;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketTeleportToLocation;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class GUIAlchemicalBook extends Screen {
    private final List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = new ArrayList<>();
    private @Nullable CapabilityAlchemicalBookLocations.TeleportLocation backLocation = null;
    ButtonCreate buttonCreate;
    ButtonBack buttonBack;
    ButtonClose buttonClose;
    @Nullable ButtonPrev buttonPrev = null;
    @Nullable ButtonNext buttonNext = null;
    TextFieldWidget createName;
    final ClientPlayerEntity player;
    final Hand hand;
    final ArrayList<ButtonTeleport> teleportButtons = new ArrayList<>();
    final ArrayList<ButtonDelete> deleteButtons = new ArrayList<>();
    private @Nullable IKnowledgeProvider knowledgeProvider = null;
    private boolean canEdit;
    private final boolean acrossDimensions;
    private int page = 1;
    private int maxPages = 1;
    public GUIAlchemicalBook(ClientPlayerEntity player, Hand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        super(Lang.GUI.ALCHEMICAL_BOOK.translate());
        this.player = player;
        this.hand = hand;
        this.locations.addAll(locations);
        this.canEdit = canEdit;
        this.acrossDimensions = getTier().isAcrossDimensions();
        extractBackLocation();
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

    private IKnowledgeProvider getKnowledgeCapability() {
        if(knowledgeProvider == null) {
            IKnowledgeProvider provider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
            if (provider == null) {
                throw new IllegalStateException("Player does not have knowledge capability");
            }
            knowledgeProvider = provider;
        }

        return knowledgeProvider;
    }

    private ItemStack getItemStack() {
        return player.getItemInHand(hand);
    }

    private ItemAlchemicalBook.Tier getTier() {
        return ((ItemAlchemicalBook) getItemStack().getItem()).getTier();
    }

    private boolean canTeleport(CapabilityAlchemicalBookLocations.TeleportLocation location) {
        return getKnowledgeCapability()
                .getEmc()
                .compareTo(BigInteger.valueOf(location.getCost(getItemStack(), player))) >= 0;
    }

    private final int w = 90, h = 20;
    // screens keep buttons and children separately before 1.17
    private <T extends Widget> T addRenderableWidget(T widget) {
        return addButton(widget);
    }

    private void removeWidget(Widget widget) {
        buttons.remove(widget);
        children.remove(widget);
    }

    private void clearWidgets() {
        buttons.clear();
        children.clear();
    }

    @Override
    protected void init() {
        addRenderableWidget(buttonCreate = new ButtonCreate(this.width / 2 - w, 20, w, h));
        addRenderableWidget(buttonClose = new ButtonClose(this.width / 2 - w - 50, 20));
        createName = new TextFieldWidget(font, buttonCreate.x + buttonCreate.getWidth() + 10, buttonCreate.y, w, h, Lang.GUI.ALCHEMICAL_BOOK_CREATE.translate());
        createName.setMaxLength(20);
        createName.setFocus(true);
        if(!canEdit) {
            buttonCreate.active = false;
            createName.setEditable(false);
        } else {
            createName.setResponder((str) -> {
                buttonCreate.active = !str.isEmpty();
                buttonCreate.setName(str);
            });
        }
        String biomeName = Objects.requireNonNull(player.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(player.level.getBiome(player.blockPosition()))).getPath();
        createName.setValue(Arrays.stream(biomeName.split("_")).map(StringUtils::capitalize).reduce("", (a, b) -> a + " " + b).trim());
        addRenderableWidget(buttonBack = new ButtonBack(createName.x + createName.getWidth() + 8, buttonClose.y, w / 2, h));
        addRenderableWidget(createName);
        drawLocations();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public void drawLocations() {
        int rowpad = 1, delete_w = 20, delpad = 2, colpad = 8;
        teleportButtons.forEach(this::removeWidget);
        teleportButtons.clear();
        deleteButtons.forEach(this::removeWidget);
        deleteButtons.clear();
        if (this.buttonPrev != null) {
            this.removeWidget(this.buttonPrev);
            this.buttonPrev = null;
        }
        if (this.buttonNext != null) {
            this.removeWidget(this.buttonNext);
            this.buttonNext = null;
        }
        // we take up 70% of the height
        int midHeight = (int) Math.floor(this.height * 0.7);
        // we take up 85% of the width
        int midWidth = (int) Math.floor(this.width * 0.85);
        int maxRows = midHeight / (h + rowpad), maxColumns = midWidth / (w + colpad + delpad + delete_w);
        // left calculates the total space the max amount of columns would take up, divides the remaining space by two
        int boxLeft = ((this.width - (((delete_w + delpad + w) * maxColumns) + (colpad * 2))) / 2), boxTop = 46;
        // adds the width of a delete button and padding due to the xStart applying to the teleport button, which then draws the delete button to the left
        int yStart = boxTop, xStart = boxLeft + (delete_w + delpad),
            x = xStart, y = yStart,
            xIndex = 1, yIndex = 1,
            perPage = maxRows * maxColumns;

        this.maxPages = (int) Math.ceil((double) locations.size() / perPage);
        for (CapabilityAlchemicalBookLocations.TeleportLocation loc : locations.subList(perPage * (page - 1), Math.min(locations.size(), perPage * page))) {
            if (xIndex > maxColumns) {
                x = xStart;
                xIndex = 2;
                yIndex++;
                y += h + rowpad;
            } else {
                if (xIndex > 1) {
                    x += w + delete_w + colpad;
                }
                xIndex++;
            }
            if (yIndex > maxRows) {
                break;
            }
            ButtonTeleport teleportButton = new ButtonTeleport(x, y, w, h, loc);
            ButtonDelete deleteButton = new ButtonDelete(x - delete_w - delpad, y, delete_w, h, loc.name());
            deleteButton.active = canEdit;
            addRenderableWidget(teleportButton);
            addRenderableWidget(deleteButton);
            teleportButtons.add(teleportButton);
            deleteButtons.add(deleteButton);
        }

        int prevY = boxTop + (h + rowpad) * yIndex, prevX = boxLeft,
            nextY = boxTop + (h + rowpad) * yIndex, nextX = boxLeft + ((w + colpad + delete_w + delpad) * maxColumns) - w + colpad;

        if (this.maxPages > 1) {
            this.buttonPrev = new ButtonPrev(prevX, prevY, w - delete_w, h, page > 1);
            this.buttonNext = new ButtonNext(nextX, nextY, w - delete_w, h, page < maxPages);
            addRenderableWidget(this.buttonPrev);
            addRenderableWidget(this.buttonNext);
        }

        if(this.backLocation != null) buttonBack.updateLocation(this.backLocation);
    }

    public void setLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        this.locations.clear();
        this.locations.addAll(locations);
        extractBackLocation();
        if (canEdit != this.canEdit) {
            this.canEdit = canEdit;
            this.clearWidgets();
            this.init();
        } else {
            drawLocations();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void extractBackLocation() {
        this.backLocation = locations.stream().filter(CapabilityAlchemicalBookLocations.TeleportLocation::isBack).findFirst().orElse(null);
        if (backLocation != null) {
            this.locations.remove(backLocation);
        }
    }

    public void previousPage() {
        this.setPage(this.page - 1);
    }

    public void nextPage() {
        this.setPage(this.page + 1);
    }

    public void setPage(int page) {
        this.page = Math.max(1, Math.min(this.maxPages, page));
        this.drawLocations();
    }

    private class ButtonClose extends Button {
        public ButtonClose(int x, int y) {
            super(x, y, 40, 20, Lang.GUI.ALCHEMICAL_BOOK_CLOSE.translate(), (button) -> player.closeContainer());
        }
    }

    private class ButtonCreate extends Button {
        String name;
        public ButtonCreate(int x, int y, int w, int h) {
            super(x, y, w, h, Lang.GUI.ALCHEMICAL_BOOK_CREATE.translate(), (button) -> {});
        }

        @Override
        public void onClick(double p_93371_, double p_93372_) {
            if (name == null) {
                return;
            }
            PacketHandler.sendToServer(new PacketCreateTeleportLocation(name, hand));
        }

        private void setName(String name) {
            this.name = name;
        }
    }

    private class ButtonDelete extends Button {
        public ButtonDelete(int x, int y, int w, int h, String name) {
            super(x, y, w, h, new StringTextComponent("X"), (button) -> PacketHandler.sendToServer(new PacketDeleteTeleportLocation(name, hand)));
        }
    }

    private class ButtonTeleport extends Button {
        final CapabilityAlchemicalBookLocations.TeleportLocation location;
        final boolean canTeleport;
        final boolean hasEnoughEMC;
        public ButtonTeleport(int x, int y, int w, int h, CapabilityAlchemicalBookLocations.TeleportLocation location) {
            super(x, y, w, h, new StringTextComponent(location.name()), (button) -> {
                PacketHandler.sendToServer(new PacketTeleportToLocation(location.name(), hand));
                player.closeContainer();
            });
            this.location = location;
            this.hasEnoughEMC = GUIAlchemicalBook.this.canTeleport(location);
            this.canTeleport = acrossDimensions || location.dimension().equals(player.level.dimension());
            this.active = canTeleport && hasEnoughEMC;
        }

        @Override
        public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks) {
            renderWidget(new GuiGraphics(poseStack), mouseX, mouseY, partialTicks);
        }

        protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float unknown) {
            if(isHovered()) {
                setTooltipForNextRenderPass(getTeleportationTooltip(location, canTeleport));
            }
            super.renderButton(graphics.pose(), pMouseX, pMouseY, unknown);
        }
    }

    private class ButtonBack extends Button {
        private @Nullable CapabilityAlchemicalBookLocations.TeleportLocation location;
        private boolean canTeleport;
        public ButtonBack(int x, int y, int w, int h) {
            super(x, y, w, h, Lang.GUI.ALCHEMICAL_BOOK_BACK.translate(), (button) -> PacketHandler.sendToServer(new PacketTeleportBack(hand)));
        }

        @Override
        public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks) {
            renderWidget(new GuiGraphics(poseStack), mouseX, mouseY, partialTicks);
        }

        protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float unknown) {
            if(isHovered()) {
                if(location == null) {
                    setTooltipForNextRenderPass(Lang.GUI.ALCHEMICAL_BOOK_NO_BACK_LOCATION.translate());
                    super.renderButton(graphics.pose(), pMouseX, pMouseY, unknown);
                    return;
                }
                setTooltipForNextRenderPass(getTeleportationTooltip(location, canTeleport));
            }
            super.renderButton(graphics.pose(), pMouseX, pMouseY, unknown);
        }

        public void updateLocation(@Nullable CapabilityAlchemicalBookLocations.TeleportLocation location) {
            if(location == null) {
                this.active = false;
                this.location = null;
                this.canTeleport = false;
            } else {
                this.active = true;
                this.location = location;
                this.canTeleport = acrossDimensions || location.dimension().equals(player.level.dimension());
            }
        }
    }

    private class ButtonPrev extends Button {
        public ButtonPrev(int x, int y, int w, int h, boolean active) {
            super(x, y, w, h, Lang.PREVIOUS.translate(), (button) -> GUIAlchemicalBook.this.previousPage());
            this.active = active;
        }
    }

    private class ButtonNext extends Button {
        public ButtonNext(int x, int y, int w, int h, boolean active) {
            super(x, y, w, h, Lang.NEXT.translate(), (button) -> GUIAlchemicalBook.this.nextPage());
            this.active = active;
        }
    }

    public ITextComponent getTeleportationTooltip(CapabilityAlchemicalBookLocations.TeleportLocation location, boolean canTeleport) {
        ArrayList<ITextComponent> tooltips = new ArrayList<>();
        if(canTeleport) {
            tooltips.add(new StringTextComponent(String.format("%d, %d, %d", location.x(), location.y(), location.z())));
            int distance = (int) location.distanceFrom(player.level, player.blockPosition());
            if(distance > 0) {
                tooltips.add(Lang.GUI.ALCHEMICAL_BOOK_DISTANCE.translate(distance));
            }
            int cost = location.getCost(getItemStack(), player);
            if (cost > 0) {
                tooltips.add(Lang.GUI.ALCHEMICAL_BOOK_COST.translate(EMCFormat.getComponent(cost).withStyle(TextFormatting.YELLOW)));
            }
        } else {
            tooltips.add(Lang.GUI.ALCHEMICAL_BOOK_DIMENSION.translate(new TranslationTextComponent(location.dimension().location().toString())));
        }

        IFormattableTextComponent tooltip = new StringTextComponent("");
        for (int i = 0; i < tooltips.size(); i++) {
            tooltip = tooltip.append(tooltips.get(i));
            if (i < tooltips.size() - 1) {
                tooltip = tooltip.append(new StringTextComponent("\n"));
            }
        }

        return tooltip;
    }
}
