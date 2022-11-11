package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.IChestLike;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockEntityAdvancedAlchemicalChest extends BlockEntityOwnable implements IChestLike, IItemHandler {
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
	@SuppressWarnings("NullableProblems")
	private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(this::getBag);
	public final DyeColor color;
	public BlockEntityAdvancedAlchemicalChest(BlockPos pos, BlockState state, BlockEntityType<BlockEntityAdvancedAlchemicalChest> blockEntityType, DyeColor color) {
		super(blockEntityType, pos, state);
		this.color = color;
	}

	public @Nullable IItemHandler getBag() {
		@Nullable ServerPlayer player = Util.getPlayer(level, owner);
		if(player == null) {
			return null;
		}

		try {
			return player.getCapability(PECapabilities.ALCH_BAG_CAPABILITY).orElseThrow(NullPointerException::new).getBag(color);
		} catch (NullPointerException ignore) {
			return null;
		}
	}

	public InteractionResult handleActivation(Player player, InteractionHand hand) {
		if(!super.handleActivation(player, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP)) {
			return InteractionResult.FAIL;
		}

		ItemStack stack = player.getItemInHand(hand);
		if(stack.isEmpty()) {
			player.displayClientMessage(new TranslatableComponent("block.projectexpansion.advanced_alchemical_chest.color", color.getName()), true );
			return InteractionResult.FAIL;
		}

		if(stack.getItem() instanceof AlchemicalBag bag) {
			this.saveAdditional(new CompoundTag());
			if(level != null) {
				BlockAdvancedAlchemicalChest block = AdvancedAlchemicalChest.getBlock(bag.color);
				level.setBlockAndUpdate(worldPosition, block.defaultBlockState());
				Util.markDirty(level, worldPosition);
			}
			player.displayClientMessage(new TranslatableComponent("block.projectexpansion.advanced_alchemical_chest.color_set", bag.color.getName()), true);
		} else {
			player.displayClientMessage(new TranslatableComponent("block.projectexpansion.advanced_alchemical_chest.invalid_item"), true );
		}

		return InteractionResult.SUCCESS;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		return
			(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
				super.getCapability(cap, side);
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

	@Override
	public int getSlots() {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? 0 : bag.getSlots();
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? ItemStack.EMPTY : bag.getStackInSlot(slot);
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? stack : bag.insertItem(slot, stack, simulate);
	}

	@NotNull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? ItemStack.EMPTY : bag.extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? 0 : bag.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		@Nullable IItemHandler bag = getBag();
		return bag != null && bag.isItemValid(slot, stack);
	}
}
