package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What transferring a recipe into the arcane crafting grid will do, worked out without changing anything
 * @param emcRequired the emc needed for everything that has to come from transmutation
 * @param emcMissing how much more emc is needed to fill every slot
 * @param excludedSlots the recipe slots that won't be filled
 * @param excluded the item shown for each slot that won't be filled, cheapest learned option first
 */
public final class RecipeTransferPlan {
    private final BigInteger emcRequired;
    private final BigInteger emcMissing;
    private final Set<Integer> excludedSlots;
    private final List<Excluded> excluded;

    public RecipeTransferPlan(BigInteger emcRequired, BigInteger emcMissing, Set<Integer> excludedSlots, List<Excluded> excluded) {
        this.emcRequired = emcRequired;
        this.emcMissing = emcMissing;
        this.excludedSlots = excludedSlots;
        this.excluded = excluded;
    }

    public BigInteger emcRequired() {
        return emcRequired;
    }

    public BigInteger emcMissing() {
        return emcMissing;
    }

    public Set<Integer> excludedSlots() {
        return excludedSlots;
    }

    public List<Excluded> excluded() {
        return excluded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeTransferPlan that = (RecipeTransferPlan) o;
        return Objects.equals(emcRequired, that.emcRequired) && Objects.equals(emcMissing, that.emcMissing) && Objects.equals(excludedSlots, that.excludedSlots) && Objects.equals(excluded, that.excluded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emcRequired, emcMissing, excludedSlots, excluded);
    }

    @Override
    public String toString() {
        return "RecipeTransferPlan[emcRequired=" + emcRequired + ", emcMissing=" + emcMissing + ", excludedSlots=" + excludedSlots + ", excluded=" + excluded + "]";
    }

    public static final class Excluded {
        private final ItemStack stack;
        private final boolean learned;

        public Excluded(ItemStack stack, boolean learned) {
            this.stack = stack;
            this.learned = learned;
        }

        public ItemStack stack() {
            return stack;
        }

        public boolean learned() {
            return learned;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Excluded that = (Excluded) o;
            return Objects.equals(stack, that.stack) && Objects.equals(learned, that.learned);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stack, learned);
        }

        @Override
        public String toString() {
            return "Excluded[stack=" + stack + ", learned=" + learned + "]";
        }
    }

    public boolean isComplete() {
        return excludedSlots.isEmpty();
    }

    public List<ITextComponent> getTooltip() {
        List<ITextComponent> lines = new ArrayList<>();
        if (emcRequired.signum() > 0) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EMC_REQUIRED.translateColored(TextFormatting.GRAY, EMCFormat.format(emcRequired)));
        }
        if (emcMissing.signum() > 0) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EMC_MISSING.translateColored(TextFormatting.RED, EMCFormat.format(emcMissing)));
        }
        if (!excluded.isEmpty()) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EXCLUDED.translateColored(TextFormatting.RED));
            // group the same item together so it reads as a count
            List<ItemStack> counted = new ArrayList<>();
            List<Boolean> learned = new ArrayList<>();
            for (Excluded entry : excluded) {
                int index = -1;
                for (int i = 0; i < counted.size(); i++) {
                    if (Util.isSameItemSameTags(counted.get(i), entry.stack())) index = i;
                }
                if (index == -1) {
                    counted.add(ItemHandlerHelper.copyStackWithSize(entry.stack(), 1));
                    learned.add(entry.learned());
                } else {
                    counted.get(index).grow(1);
                }
            }
            for (int i = 0; i < counted.size(); i++) {
                ItemStack stack = counted.get(i);
                ITextComponent name = new StringTextComponent(stack.getCount() + "x ").append(stack.getHoverName());
                lines.add(new StringTextComponent(" - ").append(learned.get(i) ? name : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_NOT_LEARNED.translate(name)).withStyle(TextFormatting.GRAY));
            }
        }
        return lines;
    }
}
