package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.Star;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.capability.EmcHolderItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ItemMagnumStar extends ItemPE implements IItemEmcHolder, IBarHelper {
    public static final long[] STAR_EMC = new long[12];

    static {
        long emc = 204_800_000L;

        for (int i = 0; i < STAR_EMC.length; i++) {
            STAR_EMC[i] = emc;
            emc *= 4L;
        }
    }

    public final Star tier;
    public final int type;

    public ItemMagnumStar(Star tier) { this(tier, 1); }
    public ItemMagnumStar(Star tier, int type) {
        super(new Properties().stacksTo(1).rarity(tier == Star.OMEGA ? Rarity.EPIC : type == 1 ? Rarity.UNCOMMON : Rarity.RARE));

        this.tier = tier;
        this.type = type;
        addItemCapability(EmcHolderItemCapabilityWrapper::new);
        addItemCapability("curios", IntegrationHelper.CURIO_CAP_SUPPLIER);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return stack.hasTag();
    }

    @Override
    public float getWidthForBar(ItemStack stack) {
        long starEmc = getEmc(stack);
        return starEmc == 0L ? 1.0F : (float)(1.0 - (double)starEmc / (double) getMaximumEmc(stack));
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return getScaledBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return getColorForBar(stack);
    }

    @Override
    public long insertEmc(@Nonnull ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
        if (toInsert < 0L) return extractEmc(stack, -toInsert, action);
        else {
            long toAdd = Math.min(getNeededEmc(stack), toInsert);
            if (action.execute()) addEmcToStack(stack, toAdd);

            return toAdd;
        }
    }

    @Override
    public long extractEmc(@Nonnull ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
        if (toExtract < 0L)
            return insertEmc(stack, -toExtract, action);
        else {
            long storedEmc = getStoredEmc(stack);
            long toRemove = Math.min(storedEmc, toExtract);
            if (action.execute()) setEmc(stack, storedEmc - toRemove);
            return toRemove;
        }
    }

    @Override
    public long getStoredEmc(@Nonnull ItemStack stack) {
        return getEmc(stack);
    }

    @Override
    public long getMaximumEmc(@Nonnull ItemStack stack) {
        return STAR_EMC[tier.ordinal()];
    }
}

