package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.util.IHasCapability;
import cool.furry.mc.neoforge.projectexpansion.util.Star;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ItemStar extends ItemPE implements IItemEmcHolder, IBarHelper, IHasCapability {
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
        ).component(PEDataComponentTypes.STORED_EMC, 0L));

        this.type = type;
        this.tier = tier;
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        IntegrationHelper.registerCuriosCapability(event, this);
        event.registerItem(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY, (stack, dir) -> this, this);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEmc(stack) > 0;
    }

    @Override
    public float getWidthForBar(ItemStack stack) {
        long starEmc = getStoredEmc(stack);
        return starEmc == 0L ? 1.0F : (float)(1.0 - (double)starEmc / (double) getMaximumEmc(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return getScaledBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getColorForBar(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && !FMLEnvironment.production && player.isCreative()) {
            stack.set(PEDataComponentTypes.STORED_EMC, getMaximumEmc(stack));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public long insertEmc(ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
        if (toInsert < 0L) return extractEmc(stack, -toInsert, action);

        long maxEmc = getMaximumEmc(stack);
        long storedEmc = getStoredEmc(stack);
        if (storedEmc >= maxEmc) return 0L;

        long toAdd = Math.min(maxEmc - storedEmc, toInsert);
        if (action.execute()) {
            stack.set(PEDataComponentTypes.STORED_EMC, storedEmc + toAdd);
        }
        return toAdd;
    }

    @Override
    public long extractEmc(ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
        if (toExtract < 0L) return insertEmc(stack, -toExtract, action);
        long storedEmc = getStoredEmc(stack);
        long toRemove = Math.min(storedEmc, toExtract);
        if (action.execute()) {
            stack.set(PEDataComponentTypes.STORED_EMC, storedEmc - toRemove);
        }
        return toRemove;
    }

    @Override
    public long getStoredEmc(ItemStack stack) {
        return stack.getOrDefault(PEDataComponentTypes.STORED_EMC, 0L);
    }

    @Override
    public long getMaximumEmc(ItemStack stack) {
        return STAR_EMC[tier.ordinal() + type.getOffset()];
    }
}

