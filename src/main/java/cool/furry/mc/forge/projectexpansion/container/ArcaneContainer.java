package cool.furry.mc.forge.projectexpansion.container;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

public class ArcaneContainer implements INamedContainerProvider {
    private final Hand hand;

    public ArcaneContainer(Hand hand) {
        this.hand = hand;
    }

    @Override
    public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
        return new TransmutationContainer(windowId, playerInventory, this.hand);
    }

    @Override
    @Nonnull
    public TranslationTextComponent getDisplayName() {
        return new TranslationTextComponent("pe.transmutation.transmute");
    }


}
