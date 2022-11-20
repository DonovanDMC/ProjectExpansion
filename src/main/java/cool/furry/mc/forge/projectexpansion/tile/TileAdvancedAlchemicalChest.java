package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileAdvancedAlchemicalChest extends TileOwnable implements IItemHandler, ITickableTileEntity {
	@SuppressWarnings("NullableProblems")
	private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(this::getBag);
	public final DyeColor color;

	private int ticksSinceSync;
	private float lidAngle;
	private float prevLidAngle;
	public int numPlayersUsing;
	public TileAdvancedAlchemicalChest(TileEntityType<TileAdvancedAlchemicalChest> blockEntityType, DyeColor color) {
		super(blockEntityType);
		this.color = color;
	}

	public @Nullable IItemHandler getBag() {
		@Nullable ServerPlayerEntity player = Util.getPlayer(level, owner);
		if(player == null) {
			return null;
		}

		try {
			return player.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY).orElseThrow(NullPointerException::new).getBag(color);
		} catch (NullPointerException ignore) {
			return null;
		}
	}

	public ActionResultType handleActivation(PlayerEntity player, Hand hand) {
		if(!super.handleActivation(player, TileOwnable.ActivationType.CHECK_OWNERSHIP)) {
			return ActionResultType.FAIL;
		}

		ItemStack stack = player.getItemInHand(hand);
		if(stack.isEmpty()) {
			player.displayClientMessage(new TranslationTextComponent("block.projectexpansion.advanced_alchemical_chest.color", color.getName()), true );
			return ActionResultType.FAIL;
		}

		if(stack.getItem() instanceof AlchemicalBag) {
			AlchemicalBag bag = (AlchemicalBag) stack.getItem();
			if(level != null) {
				BlockAdvancedAlchemicalChest block = AdvancedAlchemicalChest.getBlock(bag.color);
				TileEntityType<TileAdvancedAlchemicalChest> blockEntityType = AdvancedAlchemicalChest.getBlockEntityType(bag.color);

				TileAdvancedAlchemicalChest newBlockEntity = new TileAdvancedAlchemicalChest(blockEntityType, bag.color);
				newBlockEntity.owner = owner;
				newBlockEntity.ownerName = ownerName;
				newBlockEntity.save(new CompoundNBT());
				level.setBlockAndUpdate(worldPosition, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).setValue(BlockStateProperties.WATERLOGGED, getBlockState().getValue(BlockStateProperties.WATERLOGGED)));
				level.removeBlockEntity(worldPosition);
				level.setBlockEntity(worldPosition, newBlockEntity);
				Util.markDirty(level, worldPosition);
			}
			player.displayClientMessage(new TranslationTextComponent("block.projectexpansion.advanced_alchemical_chest.color_set", bag.color.getName()), true);
		} else {
			player.displayClientMessage(new TranslationTextComponent("block.projectexpansion.advanced_alchemical_chest.invalid_item"), true );
		}

		return ActionResultType.SUCCESS;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		return
			(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
				super.getCapability(cap, side);
	}
	@Override
	public int getSlots() {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? 0 : bag.getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? ItemStack.EMPTY : bag.getStackInSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		@Nullable IItemHandler bag = getBag();
		return bag == null ? stack : bag.insertItem(slot, stack, simulate);
	}

	@Nonnull
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
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		@Nullable IItemHandler bag = getBag();
		return bag != null && bag.isItemValid(slot, stack);
	}

	protected void updateChest() {
		if (level == null) {
			return;
		}
		if (++ticksSinceSync % 20 * 4 == 0) {
			level.blockEvent(worldPosition, getBlockState().getBlock(), 1, numPlayersUsing);
		}

		prevLidAngle = lidAngle;
		if (numPlayersUsing > 0 && lidAngle == 0.0F) {
			level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
		}

		if (numPlayersUsing == 0 && lidAngle > 0.0F || numPlayersUsing > 0 && lidAngle < 1.0F) {
			float angleIncrement = 0.1F;
			if (numPlayersUsing > 0) {
				lidAngle += angleIncrement;
			} else {
				lidAngle -= angleIncrement;
			}
			if (lidAngle > 1.0F) {
				lidAngle = 1.0F;
			}
			if (lidAngle < 0.5F && prevLidAngle >= 0.5F) {
				level.playSound(null, worldPosition, SoundEvents.CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
			}
			if (lidAngle < 0.0F) {
				lidAngle = 0.0F;
			}
		}
	}

	@Override
	public void tick() {
		if (level != null) {
			updateChest();
		}
	}

	@Override
	public boolean triggerEvent(int number, int arg) {
		if (number == 1) {
			numPlayersUsing = arg;
			return true;
		}
		return super.triggerEvent(number, arg);
	}

	public float getLidAngle(float partialTicks) {
		//Only used on the client
		return MathHelper.lerp(partialTicks, this.prevLidAngle, this.lidAngle);
	}
}
