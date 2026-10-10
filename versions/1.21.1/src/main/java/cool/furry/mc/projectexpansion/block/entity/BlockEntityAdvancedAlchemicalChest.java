package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class BlockEntityAdvancedAlchemicalChest extends BlockEntityOwnable implements IChestLike, IHasColor {
	public static final ICapabilityProvider<BlockEntityAdvancedAlchemicalChest, @Nullable Direction, IItemHandler> ITEM_HANDLER_CAPABILITY = (be, side) -> be.getBagItemHandler();
	private final ChestLidController lidController = new ChestLidController();

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
				level.random.nextFloat() * 0.1F + 0.9F);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
				level.random.nextFloat() * 0.1F + 0.9F);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int openCount) {
			level.blockEvent(pos, state.getBlock(), 1, openCount);
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ContainerAdvancedAlchemicalChest chest && chest.blockEntityMatches(BlockEntityAdvancedAlchemicalChest.this);
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
	public BlockEntityAdvancedAlchemicalChest(BlockPos pos, BlockState state) {
		super(BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get(), pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get(), ITEM_HANDLER_CAPABILITY);
	}

	BagItemHandler getBagItemHandler() {
		return new BagItemHandler();
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
		@Nullable ServerPlayer player = Util.getPlayer(level, owner);
		if(player == null) {
			return null;
		}

        @Nullable IAlchBagProvider provider = player.getCapability(PECapabilities.ALCH_BAG_CAPABILITY);
        if (provider == null) return null;
        return provider.getBag(getColor());
	}

	public ItemInteractionResult handleItemActivation(Player player, ItemStack stack) {
		if(!super.handleActivation(player, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP)) {
			return ItemInteractionResult.FAIL;
		}

		if(stack.isEmpty()) {
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_INVALID_ITEM.translate(), true);
			return ItemInteractionResult.FAIL;
		}

		if(stack.getItem() instanceof AlchemicalBag bag) {
			if(level != null) {
				BlockAdvancedAlchemicalChest block = AdvancedAlchemicalChest.getBlock(bag.color);

				BlockEntityAdvancedAlchemicalChest newBlockEntity = new BlockEntityAdvancedAlchemicalChest(worldPosition, getBlockState());
				newBlockEntity.owner = owner;
				newBlockEntity.ownerName = ownerName;
				newBlockEntity.saveAdditional(new CompoundTag(), player.registryAccess());
				level.setBlockAndUpdate(worldPosition, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).setValue(BlockStateProperties.WATERLOGGED, getBlockState().getValue(BlockStateProperties.WATERLOGGED)));
				level.removeBlockEntity(worldPosition);
				level.setBlockEntity(newBlockEntity);
				Util.markDirty(level, worldPosition);
			}
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_COLOR_SET.translate(bag.color.getName()), true);
		} else {
			player.displayClientMessage(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_INVALID_ITEM.translate(), true);
		}

		return ItemInteractionResult.CONSUME;
	}

	@SuppressWarnings("unused")
	public static void tickClient(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if(blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) {
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

	public void startOpen(Player player) {
		if (!isRemoved() && !player.isSpectator() && level != null) {
			openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void stopOpen(Player player) {
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
}
