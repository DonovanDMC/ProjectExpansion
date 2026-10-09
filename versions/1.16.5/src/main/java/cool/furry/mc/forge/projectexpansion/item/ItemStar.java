package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.Star;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.capability.EmcHolderItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class ItemStar extends ItemPE implements IItemEmcHolder {
    public static final long[] STAR_EMC = new long[18];

    static {
        long emc = 204_800_000L;

        for (int i = 0; i < STAR_EMC.length; i++) {
            STAR_EMC[i] = emc;
            emc *= 4L;
        }
    }

    public final Star tier;
    public final Star.StarType type;

    public ItemStar(Star.StarType type, Star tier) {
        super(new Properties().stacksTo(1).rarity(
                tier == Star.OMEGA ? Rarity.EPIC :
                        type == Star.StarType.COLOSSAL ? Rarity.UNCOMMON :
                                type == Star.StarType.GARGANTUAN ? Rarity.RARE : Rarity.COMMON
        ));

        this.type = type;
        this.tier = tier;
        addItemCapability(EmcHolderItemCapabilityWrapper::new);
        addItemCapability("curios", IntegrationHelper.CURIO_CAP_SUPPLIER);
    }

    // durability bars were reworked in 1.17
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getStoredEmc(stack) > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        long starEmc = getStoredEmc(stack);
        return starEmc == 0L ? 1.0 : 1.0 - (double) starEmc / (double) getMaximumEmc(stack);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && !FMLEnvironment.production && player.isCreative()) {
            setEmc(stack, getMaximumEmc(stack));
            return ActionResult.success(stack);
        }
        return ActionResult.pass(stack);
    }

    @Override
    public long insertEmc(ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
        if (toInsert < 0L) return extractEmc(stack, -toInsert, action);

        long maxEmc = getMaximumEmc(stack);
        long storedEmc = getStoredEmc(stack);
        if (storedEmc >= maxEmc) return 0L;

        long toAdd = Math.min(maxEmc - storedEmc, toInsert);
        if (action.execute()) {
            setEmc(stack, storedEmc + toAdd);
        }
        return toAdd;
    }

    @Override
    public long extractEmc(ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
        if (toExtract < 0L) return insertEmc(stack, -toExtract, action);
        long storedEmc = getStoredEmc(stack);
        long toRemove = Math.min(storedEmc, toExtract);
        if (action.execute()) {
            setEmc(stack, storedEmc - toRemove);
        }
        return toRemove;
    }

    @Override
    public long getStoredEmc(ItemStack stack) {
        return getEmc(stack);
    }

    @Override
    public long getMaximumEmc(ItemStack stack) {
        return STAR_EMC[tier.ordinal() + type.getOffset()];
    }
}

