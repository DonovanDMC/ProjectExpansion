package cool.furry.mc.neoforge.projectexpansion.util;

import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public record TransmutationContainerProvider(@Nullable InteractionHand hand) implements MenuProvider {
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        if (hand == null) {
            return new TransmutationContainer(windowId, playerInventory);
        } else {
            return new TransmutationContainer(windowId, playerInventory, hand, playerInventory.selected);
        }
    }

    public Component getDisplayName() {
        return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }
}
