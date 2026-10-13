package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcStorage;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ToolType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.EnumProperty;
import net.minecraft.util.math.BlockRayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockEmcStorage extends Block {
    public static final Map<Direction, EnumProperty<StorageSide>> SIDES = new EnumMap<>(Direction.class);
    // where a block item keeps its block entity's data, which is loaded into the block entity when placed
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    static {
        for (Direction side : Direction.values()) SIDES.put(side, EnumProperty.create(side.getSerializedName(), StorageSide.class));
    }

    public final EmcStorageTier tier;

    public BlockEmcStorage(AbstractBlock.Properties properties, EmcStorageTier tier) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (EnumProperty<StorageSide> property : SIDES.values()) state = state.setValue(property, StorageSide.INPUT);
        registerDefaultState(state);
    }

    public static AbstractBlock.Properties getProperties() {
        return AbstractBlock.Properties.of(Material.METAL, MaterialColor.COLOR_PURPLE).strength(5, 6).requiresCorrectToolForDrops().harvestTool(ToolType.PICKAXE).harvestLevel(1).sound(SoundType.METAL);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    public static StorageSide getSide(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side));
    }

    // the item keeps what was stored, see the loot table's copy_nbt
    public static BigInteger getStoredEmc(ItemStack stack) {
        CompoundNBT tag = stack.getTagElement(BLOCK_ENTITY_TAG);
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
    public static void copyStoredEmc(IInventory from, ItemStack to) {
        BigInteger stored = BigInteger.ZERO;
        for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack stack = from.getItem(slot);
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof BlockEmcStorage) stored = stored.add(getStoredEmc(stack));
        }
        if (stored.signum() > 0) setStoredEmc(to, stored);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!Wrench.isWrench(player.getItemInHand(hand))) return ActionResultType.PASS;
        if (!level.isClientSide) {
            Direction side = hit.getDirection();
            StorageSide current = getSide(state, side);
            StorageSide changed = player.isShiftKeyDown() ? current.previous() : current.next();
            level.setBlockAndUpdate(pos, state.setValue(SIDES.get(side), changed));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(Lang.Blocks.EMC_STORAGE_SIDE.extendColored(changed.getSerializedName(), TextFormatting.GRAY, new TranslationTextComponent("direction.projectexpansion." + side.getSerializedName())), true);
        }
        return ActionResultType.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
        TileEntity storage = level.getBlockEntity(pos);
        return storage instanceof BlockEntityEmcStorage ? ((BlockEntityEmcStorage) storage).getComparatorSignal() : 0;
    }

    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityEmcStorage();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_STORAGE_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.EMC_STORAGE_TOOLTIP2.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.EMC_STORAGE_CAPACITY.translateColored(TextFormatting.GRAY, amount(tier.getCapacity())));
        list.add(Lang.Blocks.EMC_STORAGE_RATE.translateColored(TextFormatting.GRAY, amount(tier.getRate())));
        BigInteger stored = getStoredEmc(stack);
        if (stored.signum() > 0) list.add(Lang.Blocks.EMC_STORAGE_STORED.translateColored(TextFormatting.GREEN, EMCFormat.getComponent(stored)));
    }

    private static ITextComponent amount(BigInteger value) {
        return value.compareTo(EmcStorageTier.UNLIMITED) >= 0 ? new StringTextComponent("∞") : EMCFormat.getComponent(value);
    }
}
