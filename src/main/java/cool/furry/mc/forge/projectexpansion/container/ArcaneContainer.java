package cool.furry.mc.forge.projectexpansion.container;

import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

public class ArcaneContainer implements INamedContainerProvider {
    public ArcaneContainer() {
    }

    @Override
    public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
        return new TransmutationContainer(windowId, playerInventory);
    }

    @Override
    @Nonnull
    public TranslationTextComponent getDisplayName() {
        return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }


}
