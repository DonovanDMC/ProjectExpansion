package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketCreateTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketDeleteTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportBack;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportToDestination;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
    EditBox createName;
    LocalPlayer player;
    InteractionHand hand;
    ArrayList<ButtonTeleport> teleportButtons = new ArrayList<>();
    ArrayList<ButtonDelete> deleteButtons = new ArrayList<>();
    private @Nullable IKnowledgeProvider knowledgeProvider = null;
    private boolean canEdit;
    private final boolean acrossDimensions;
    public GUIAlchemicalBook(LocalPlayer player, InteractionHand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        super(Lang.ALCHEMICAL_BOOK.translate());
        this.player = player;
        this.hand = hand;
        this.locations.addAll(locations);
        this.canEdit = canEdit;
        this.acrossDimensions = getTier().isAcrossDimensions();
    }

    private IKnowledgeProvider getKnowledgeCapability() {
        if(knowledgeProvider == null) {
            LazyOptional<IKnowledgeProvider> lazyOptional = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
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
        addRenderableWidget(buttonCreate = new ButtonCreate(this.width / 2 - w, 20, w, h));
        addRenderableWidget(buttonClose = new ButtonClose(this.width / 2 - w - 50, 20));
        createName = new EditBox(font, buttonCreate.x + buttonCreate.getWidth() + 10, buttonCreate.y, w, h, Lang.ALCHEMICAL_BOOK_CREATE.translate());
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
        String biomeName = Objects.requireNonNull(player.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(player.getLevel().getBiome(player.getOnPos()).get())).getPath();
        createName.setValue(Arrays.stream(biomeName.split("_")).map(StringUtils::capitalize).reduce("", (a, b) -> a + " " + b).trim());
        addRenderableWidget(buttonBack = new ButtonBack(createName.x + createName.getWidth() + 8, buttonClose.y, w / 2, h));
        addRenderableWidget(createName);
        drawLocations();
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
            addRenderableWidget(teleportButton);
            addRenderableWidget(deleteButton);
            teleportButtons.add(teleportButton);
            deleteButtons.add(deleteButton);
        }

        if(!hasBack) buttonBack.updateLocation(null);
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
            super(x, y, w, h, Component.literal("X"), (button) -> PacketHandler.sendToServer(new PacketDeleteTeleportDestination(name, player, hand)));
        }
    }

    private class ButtonTeleport extends Button {
        CapabilityAlchemicalBookLocations.TeleportLocation location;
        boolean canTeleport;
        boolean hasEnoughEMC;
        public ButtonTeleport(int x, int y, int w, int h, CapabilityAlchemicalBookLocations.TeleportLocation location) {
            super(x, y, w, h, Component.literal(location.name()), (button) -> {
                PacketHandler.sendToServer(new PacketTeleportToDestination(location.name(), player, hand));
                player.closeContainer();
            });
            this.location = location;
            this.hasEnoughEMC = GUIAlchemicalBook.this.canTeleport(location);
            this.canTeleport = acrossDimensions || location.dimension().equals(player.getLevel().dimension());
            this.active = canTeleport && hasEnoughEMC;
        }

        @Override
        public void renderToolTip(PoseStack matrix, int pMouseX, int pMouseY) {
            if(isHoveredOrFocused()) {
                List<Component> tooltips = getTeleportationTooltips(location, canTeleport);
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
        public void renderToolTip(PoseStack matrix, int pMouseX, int pMouseY) {
            if(isHoveredOrFocused()) {
                if(location == null) {
                    renderComponentTooltip(matrix, Collections.singletonList(Lang.ALCHEMICAL_BOOK_NO_BACK_LOCATION.translate()), pMouseX, pMouseY);
                    return;
                }
                List<Component> tooltips = getTeleportationTooltips(location, canTeleport);
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
                this.canTeleport = acrossDimensions || location.dimension().equals(player.getLevel().dimension());
            }
        }
    }

    public ArrayList<Component> getTeleportationTooltips(CapabilityAlchemicalBookLocations.TeleportLocation location, boolean canTeleport) {
        ArrayList<Component> tooltips = new ArrayList<>();
        if(canTeleport) {
            tooltips.add(Component.literal(String.format("%d, %d, %d", location.x(), location.y(), location.z())));
            int distance = (int) location.distanceFrom(player.getOnPos());
            if(distance > 0) {
                tooltips.add(Lang.ALCHEMICAL_BOOK_DISTANCE.translate(distance));
            }
            int cost = location.getCost(getItemStack(), player);
            if (cost > 0) {
                tooltips.add(Lang.ALCHEMICAL_BOOK_COST.translate(EMCFormat.getComponent(cost).withStyle(ChatFormatting.YELLOW)));
            }
        } else {
            tooltips.add(Lang.ALCHEMICAL_BOOK_DIMENSION.translate(Component.translatable(location.dimension().location().toString())));
        }

        return tooltips;
    }
}
