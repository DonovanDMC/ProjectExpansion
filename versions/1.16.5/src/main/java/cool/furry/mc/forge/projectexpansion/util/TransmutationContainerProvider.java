package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class TransmutationContainerProvider implements INamedContainerProvider {
    @Nullable private final Hand hand;

    public TransmutationContainerProvider(@Nullable Hand hand) {
        this.hand = hand;
    }

    @Nullable public Hand hand() {
        return hand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransmutationContainerProvider that = (TransmutationContainerProvider) o;
        return Objects.equals(hand, that.hand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hand);
    }

    @Override
    public String toString() {
        return "TransmutationContainerProvider[hand=" + hand + "]";
    }

    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        if (hand == null) {
            return new TransmutationContainer(windowId, playerInventory);
        } else {
            return new TransmutationContainer(windowId, playerInventory, hand, playerInventory.selected);
        }
    }

    public ITextComponent getDisplayName() {
        return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }
}
