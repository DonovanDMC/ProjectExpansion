package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public record ArcaneTransmutationContainerProvider(@Nullable InteractionHand hand, Component title) implements MenuProvider {
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        IKnowledgeProvider provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (provider == null) return null;
        return new ContainerArcaneTransmutationTablet(windowId, inventory, provider, hand, inventory.selected);
    }

    @Override
    public Component getDisplayName() {
        return title;
    }
}
