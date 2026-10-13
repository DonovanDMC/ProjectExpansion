package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockEmcStorage extends Block implements EntityBlock {
    public static final Map<Direction, EnumProperty<StorageSide>> SIDES = new EnumMap<>(Direction.class);
    // where a block item keeps its block entity's data, which is loaded into the block entity when placed
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    static {
        for (Direction side : Direction.values()) SIDES.put(side, EnumProperty.create(side.getSerializedName(), StorageSide.class));
    }

    public final EmcStorageTier tier;

    public BlockEmcStorage(BlockBehaviour.Properties properties, EmcStorageTier tier) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (EnumProperty<StorageSide> property : SIDES.values()) state = state.setValue(property, StorageSide.INPUT);
        registerDefaultState(state);
    }

    public static BlockBehaviour.Properties getProperties() {
        return Platform.blockProperties(DyeColor.PURPLE).strength(5, 6).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    public static StorageSide getSide(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side));
    }

    // the item keeps what was stored, see the loot table's copy_nbt
    public static BigInteger getStoredEmc(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(BLOCK_ENTITY_TAG);
        if (tag == null || !tag.contains(TagNames.STORED_EMC)) return BigInteger.ZERO;
        try {
            return new BigInteger(tag.getString(TagNames.STORED_EMC));
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    public static void setStoredEmc(ItemStack stack, BigInteger emc) {
        stack.getOrCreateTagElement(BLOCK_ENTITY_TAG).putString(TagNames.STORED_EMC, emc.toString());
    }

    // for the upgrade recipe, the new storage gets what the one it's made from held
    public static void copyStoredEmc(Container from, ItemStack to) {
        BigInteger stored = BigInteger.ZERO;
        for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack stack = from.getItem(slot);
            if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BlockEmcStorage) stored = stored.add(getStoredEmc(stack));
        }
        if (stored.signum() > 0) setStoredEmc(to, stored);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Wrench.isWrench(player.getItemInHand(hand))) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Direction side = hit.getDirection();
            StorageSide current = getSide(state, side);
            StorageSide changed = player.isShiftKeyDown() ? current.previous() : current.next();
            level.setBlockAndUpdate(pos, state.setValue(SIDES.get(side), changed));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(Lang.Blocks.EMC_STORAGE_SIDE.extendColored(changed.getSerializedName(), ChatFormatting.GRAY, Platform.translatable("direction.projectexpansion." + side.getSerializedName())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityEmcStorage storage ? storage.getComparatorSignal() : 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityEmcStorage(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.EMC_STORAGE.get() && !level.isClientSide) return BlockEntityEmcStorage::tickServer;
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_STORAGE_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.EMC_STORAGE_TOOLTIP2.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.EMC_STORAGE_CAPACITY.translateColored(ChatFormatting.GRAY, amount(tier.getCapacity())));
        list.add(Lang.Blocks.EMC_STORAGE_RATE.translateColored(ChatFormatting.GRAY, amount(tier.getRate())));
        BigInteger stored = getStoredEmc(stack);
        if (stored.signum() > 0) list.add(Lang.Blocks.EMC_STORAGE_STORED.translateColored(ChatFormatting.GREEN, EMCFormat.getComponent(stored)));
    }

    private static Component amount(BigInteger value) {
        return value.compareTo(EmcStorageTier.UNLIMITED) >= 0 ? Platform.literal("∞") : EMCFormat.getComponent(value);
    }
}
