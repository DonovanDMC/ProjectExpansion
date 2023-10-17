package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketCreateTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketDeleteTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportBack;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportToDestination;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class GUIAlchemicalBook extends Screen {
    private final List<CapabilityAlchemicalBookLocations.TeleportLocation> locations = new ArrayList<>();
    public static final int BUTTONS_PER_COLUMN = 8;
    ButtonCreate buttonCreate;
    ButtonBack buttonBack;
    ButtonClose buttonClose;
    TextFieldWidget createName;
    ClientPlayerEntity player;
    Hand hand;
    ArrayList<ButtonTeleport> teleportButtons = new ArrayList<>();
    ArrayList<ButtonDelete> deleteButtons = new ArrayList<>();
    private @Nullable IKnowledgeProvider knowledgeProvider = null;
    private boolean canEdit;
    private final boolean acrossDimensions;
    public GUIAlchemicalBook(ClientPlayerEntity player, Hand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        super(Lang.ALCHEMICAL_BOOK.translate());
        this.player = player;
        this.hand = hand;
        this.locations.addAll(locations);
        this.canEdit = canEdit;
        this.acrossDimensions = getTier().isAcrossDimensions();
    }

    private IKnowledgeProvider getKnowledgeCapability() {
        if(knowledgeProvider == null) {
            LazyOptional<IKnowledgeProvider> lazyOptional = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY);
            knowledgeProvider = lazyOptional.orElseThrow(() -> new IllegalStateException("Player does not have knowledge capability"));
            lazyOptional.addListener((optional) -> knowledgeProvider = null);
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
    @Override
    protected void init() {
        addButton(buttonCreate = new ButtonCreate(this.width / 2 - w, 20, w, h));
        addButton(buttonClose = new ButtonClose(this.width / 2 - w - 50, 20));
        createName = new TextFieldWidget(font, buttonCreate.x + buttonCreate.getWidth() + 10, buttonCreate.y, w, h, Lang.ALCHEMICAL_BOOK_CREATE.translate());
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
        String biomeName = Objects.requireNonNull(player.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(player.level.getBiome(player.getOnPos()))).getPath();
        createName.setValue(Arrays.stream(biomeName.split("_")).map(StringUtils::capitalize).reduce("", (a, b) -> a + " " + b).trim());
        addButton(buttonBack = new ButtonBack(createName.x + createName.getWidth() + 8, buttonClose.y, w / 2, h));
        addWidget(createName);
        drawLocations();
    }

    @Override
    public void render(MatrixStack stack, int x, int y, float z) {
        createName.render(stack, x, y, z);
        super.render(stack, x, y, z);
    }

    private void removeWidget(IGuiEventListener widget) {
        if(widget instanceof Button) {
            this.buttons.remove(widget);
        }

        this.children.remove(widget);
    }

    public void drawLocations() {
        int ypad = 1, delete_w = 20, rowpad = 8;
        teleportButtons.forEach(this::removeWidget);
        teleportButtons.clear();
        deleteButtons.forEach(this::removeWidget);
        deleteButtons.clear();
        int yStart = 45, xStart = this.width / 10, x = xStart, y = yStart;

        boolean hasBack = false;
        for (CapabilityAlchemicalBookLocations.TeleportLocation loc : locations) {
            if(loc.isBack()) {
                buttonBack.updateLocation(loc);
                hasBack = true;
                continue;
            }
            if((loc.index() % BUTTONS_PER_COLUMN) == 0) {
                x += w + delete_w + rowpad;
                y = yStart;
            } else {
                y += h + ypad;
            }
            ButtonTeleport teleportButton = new ButtonTeleport(x, y, w, h, loc);
            ButtonDelete deleteButton = new ButtonDelete(x - delete_w - 2, y, delete_w, h, loc.name());
            deleteButton.active = canEdit;
            addButton(teleportButton);
            addButton(deleteButton);
            teleportButtons.add(teleportButton);
            deleteButtons.add(deleteButton);
        }

        if(!hasBack) buttonBack.updateLocation(null);
    }

    // no rebuildWidgets/clearWidgets function in 1.16 this mimics the behavior
    private void rebuildWidgets() {
        this.buttons.clear();
        this.children.clear();
        this.setFocused(null);
        this.init();
    }

    public void setLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        this.locations.clear();
        this.locations.addAll(locations);
        if(canEdit != this.canEdit) {
            this.canEdit = canEdit;
            this.rebuildWidgets();
        } else {
            drawLocations();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class ButtonClose extends Button {
        public ButtonClose(int x, int y) {
            super(x, y, 40, 20, Lang.ALCHEMICAL_BOOK_CLOSE.translate(), (button) -> player.closeContainer());
        }
    }

    private class ButtonCreate extends Button {
        String name;
        public ButtonCreate(int x, int y, int w, int h) {
            super(x, y, w, h, Lang.ALCHEMICAL_BOOK_CREATE.translate(), (button) -> {});
        }

        @Override
        public void onClick(double p_93371_, double p_93372_) {
            if (name == null) {
                return;
            }
            PacketHandler.sendToServer(new PacketCreateTeleportDestination(name, player, hand));
        }

        private void setName(String name) {
            this.name = name;
        }
    }

    private class ButtonDelete extends Button {
        public ButtonDelete(int x, int y, int w, int h, String name) {
            super(x, y, w, h, new StringTextComponent("X"), (button) -> PacketHandler.sendToServer(new PacketDeleteTeleportDestination(name, player, hand)));
        }
    }

    private class ButtonTeleport extends Button {
        CapabilityAlchemicalBookLocations.TeleportLocation location;
        boolean canTeleport;
        boolean hasEnoughEMC;
        public ButtonTeleport(int x, int y, int w, int h, CapabilityAlchemicalBookLocations.TeleportLocation location) {
            super(x, y, w, h, new StringTextComponent(location.name()), (button) -> {
                PacketHandler.sendToServer(new PacketTeleportToDestination(location.name(), player, hand));
                player.closeContainer();
            });
            this.location = location;
            this.hasEnoughEMC = GUIAlchemicalBook.this.canTeleport(location);
            this.canTeleport = acrossDimensions || location.dimension().equals(player.level.dimension());
            this.active = canTeleport && hasEnoughEMC;
        }

        @Override
        public void renderToolTip(MatrixStack matrix, int pMouseX, int pMouseY) {
            if(this.isHovered() || this.isFocused()) {
                List<ITextComponent> tooltips = getTeleportationTooltips(location, canTeleport);
                renderComponentTooltip(matrix, tooltips, pMouseX, pMouseY);
            }
            super.renderToolTip(matrix, pMouseX, pMouseY);
        }
    }

    private class ButtonBack extends Button {
        private @Nullable CapabilityAlchemicalBookLocations.TeleportLocation location;
        private boolean canTeleport;
        public ButtonBack(int x, int y, int w, int h) {
            super(x, y, w, h, Lang.ALCHEMICAL_BOOK_BACK.translate(), (button) -> PacketHandler.sendToServer(new PacketTeleportBack(player, hand)));
        }

        @Override
        public void renderToolTip(MatrixStack matrix, int pMouseX, int pMouseY) {
            if(this.isHovered() || this.isFocused()) {
                if(location == null) {
                    renderComponentTooltip(matrix, Collections.singletonList(Lang.ALCHEMICAL_BOOK_NO_BACK_LOCATION.translate()), pMouseX, pMouseY);
                    return;
                }
                List<ITextComponent> tooltips = getTeleportationTooltips(location, canTeleport);
                renderComponentTooltip(matrix, tooltips, pMouseX, pMouseY);
            }
            super.renderToolTip(matrix, pMouseX, pMouseY);
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

    public ArrayList<ITextComponent> getTeleportationTooltips(CapabilityAlchemicalBookLocations.TeleportLocation location, boolean canTeleport) {
        ArrayList<ITextComponent> tooltips = new ArrayList<>();
        if(canTeleport) {
            tooltips.add(new StringTextComponent(String.format("%d, %d, %d", location.x(), location.y(), location.z())));
            int distance = (int) location.distanceFrom(player.getOnPos());
            if(distance > 0) {
                tooltips.add(Lang.ALCHEMICAL_BOOK_DISTANCE.translate(distance));
            }
            int cost = location.getCost(getItemStack(), player);
            if (cost > 0) {
                tooltips.add(Lang.ALCHEMICAL_BOOK_COST.translate(EMCFormat.getComponent(cost).withStyle(TextFormatting.YELLOW)));
            }
        } else {
            tooltips.add(Lang.ALCHEMICAL_BOOK_DIMENSION.translate(new TranslationTextComponent(location.dimension().location().toString().replace(":", "."))));
        }

        return tooltips;
    }
}
