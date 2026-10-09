package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class BlockEntityAdvancedAlchemicalChest extends BlockEntityOwnable implements IChestLike, IHasColor {
	private final ChestLidController lidController = new ChestLidController();

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(World level, BlockPos pos, BlockState state) {
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_OPEN, SoundCategory.BLOCKS, 0.5F,
					level.random.nextFloat() * 0.1F + 0.9F);
		}

		@Override
		protected void onClose(World level, BlockPos pos, BlockState state) {
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F,
					level.random.nextFloat() * 0.1F + 0.9F);
		}

		@Override
		protected void openerCountChanged(World level, BlockPos pos, BlockState state, int oldCount, int openCount) {
			level.blockEvent(pos, state.getBlock(), 1, openCount);
		}

		@Override
		protected boolean isOwnContainer(PlayerEntity player) {
			return player.containerMenu instanceof ContainerAdvancedAlchemicalChest && ((ContainerAdvancedAlchemicalChest) player.containerMenu).blockEntityMatches(BlockEntityAdvancedAlchemicalChest.this);
		}
	};

	private class BagItemHandler implements IItemHandler {
		@Override
		public int getSlots() {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.getSlots();
			}
			return 0;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.getStackInSlot(slot);
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.insertItem(slot, stack, simulate);
			}
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.extractItem(slot, amount, simulate);
			}
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.getSlotLimit(slot);
			}
			return 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			IItemHandler bag = getBag();
			if(bag != null) {
				return bag.isItemValid(slot, stack);
			}
			return false;
		}
	}

	private DyeColor color;
	public BlockEntityAdvancedAlchemicalChest() {
		super(BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get());
		this.addCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, (side) -> new BagItemHandler());
	}

	@Override
	public DyeColor getColor() {
		BlockAdvancedAlchemicalChest block = (BlockAdvancedAlchemicalChest) getBlockState().getBlock();
		if (block.getColor() != color) {
			this.color = block.getColor();
		}
		return color;
	}

	public @Nullable IItemHandler getBag() {
		@Nullable ServerPlayerEntity player = Util.getPlayer(level, owner);
		if(player == null) {
			return null;
		}

		return player.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY).map(p -> p.getBag(color)).orElse(null);
	}

	public ActionResultType handleItemActivation(PlayerEntity player, ItemStack stack) {
		if(!super.handleActivation(player, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP)) {
			return ActionResultType.FAIL;
		}

		if(stack.isEmpty()) {
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_INVALID_ITEM.translate(), true);
			return ActionResultType.FAIL;
		}

		if(stack.getItem() instanceof AlchemicalBag) {
		    AlchemicalBag bag = (AlchemicalBag) stack.getItem();
			if(level != null) {
				BlockAdvancedAlchemicalChest block = AdvancedAlchemicalChest.getBlock(bag.color);

				BlockEntityAdvancedAlchemicalChest newBlockEntity = new BlockEntityAdvancedAlchemicalChest();
				newBlockEntity.owner = owner;
				newBlockEntity.ownerName = ownerName;
				level.setBlockAndUpdate(worldPosition, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).setValue(BlockStateProperties.WATERLOGGED, getBlockState().getValue(BlockStateProperties.WATERLOGGED)));
				level.removeBlockEntity(worldPosition);
				level.setBlockEntity(worldPosition, newBlockEntity);
				Util.markDirty(level, worldPosition);
			}
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_COLOR_SET.translate(bag.color.getName()), true);
		} else {
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_INVALID_ITEM.translate(), true);
		}

		return ActionResultType.CONSUME;
	}

	@SuppressWarnings("unused")
	public static void tickClient(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
		if(blockEntity instanceof BlockEntityAdvancedAlchemicalChest) {
		    BlockEntityAdvancedAlchemicalChest be = ((BlockEntityAdvancedAlchemicalChest) blockEntity);
			be.lidController.tickLid();
		}
	}

	@Override
	public boolean triggerEvent(int id, int type) {
		if (id == 1) {
			lidController.shouldBeOpen(type > 0);
			return true;
		}
		return super.triggerEvent(id, type);
	}

	public void startOpen(PlayerEntity player) {
		if (!isRemoved() && !player.isSpectator() && level != null) {
			openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void stopOpen(PlayerEntity player) {
		if (!isRemoved() && !player.isSpectator() && level != null) {
			openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void recheckOpen() {
		if (!isRemoved() && level != null) {
			openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
		}
	}

	@Override
	public float getOpenNess(float partialTicks) {
		return lidController.getOpenness(partialTicks);
	}

    @Override
    protected void clientTick() {
        tickClient(level, worldPosition, getBlockState(), this);
    }
}
