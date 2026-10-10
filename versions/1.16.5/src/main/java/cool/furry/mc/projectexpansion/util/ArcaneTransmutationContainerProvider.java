package cool.furry.mc.projectexpansion.util;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ArcaneTransmutationContainerProvider implements INamedContainerProvider {
    @Nullable private final Hand hand;
    private final ITextComponent title;

    public ArcaneTransmutationContainerProvider(@Nullable Hand hand, ITextComponent title) {
        this.hand = hand;
        this.title = title;
    }

    @Nullable public Hand hand() {
        return hand;
    }

    public ITextComponent title() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArcaneTransmutationContainerProvider that = (ArcaneTransmutationContainerProvider) o;
        return Objects.equals(hand, that.hand) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hand, title);
    }

    @Override
    public String toString() {
        return "ArcaneTransmutationContainerProvider[hand=" + hand + ", title=" + title + "]";
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory inventory, PlayerEntity player) {
        IKnowledgeProvider provider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (provider == null) return null;
        return new ContainerArcaneTransmutationTablet(windowId, inventory, provider, hand, inventory.selected);
    }

    @Override
    public ITextComponent getDisplayName() {
        return title;
    }
}
