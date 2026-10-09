package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What transferring a recipe into the arcane crafting grid will do, worked out without changing anything
 * @param emcRequired the emc needed for everything that has to come from transmutation
 * @param emcMissing how much more emc is needed to fill every slot
 * @param excludedSlots the recipe slots that won't be filled
 * @param excluded the item shown for each slot that won't be filled, cheapest learned option first
 */
public record RecipeTransferPlan(BigInteger emcRequired, BigInteger emcMissing, Set<Integer> excludedSlots, List<Excluded> excluded) {
    public record Excluded(ItemStack stack, boolean learned) {}

    public boolean isComplete() {
        return excludedSlots.isEmpty();
    }

    public List<Component> getTooltip() {
        List<Component> lines = new ArrayList<>();
        if (emcRequired.signum() > 0) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EMC_REQUIRED.translateColored(ChatFormatting.GRAY, EMCFormat.format(emcRequired)));
        }
        if (emcMissing.signum() > 0) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EMC_MISSING.translateColored(ChatFormatting.RED, EMCFormat.format(emcMissing)));
        }
        if (!excluded.isEmpty()) {
            lines.add(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_EXCLUDED.translateColored(ChatFormatting.RED));
            // group the same item together so it reads as a count
            List<ItemStack> counted = new ArrayList<>();
            List<Boolean> learned = new ArrayList<>();
            for (Excluded entry : excluded) {
                int index = -1;
                for (int i = 0; i < counted.size(); i++) {
                    if (ItemStack.isSameItemSameTags(counted.get(i), entry.stack())) index = i;
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
                Component name = new TextComponent(stack.getCount() + "x ").append(stack.getHoverName());
                lines.add(new TextComponent(" - ").append(learned.get(i) ? name : Lang.GUI.ARCANE_TRANSMUTATION_TABLET_TRANSFER_NOT_LEARNED.translate(name)).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }
}
