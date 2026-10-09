package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import net.minecraftforge.items.IItemHandlerModifiable;

// yet again more "inspiration" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bd/src/main/java/moze_intel/projecte/gameObjs/container/EmcChestBlockEntityContainer.java
public class ContainerAdvancedAlchemicalChest extends AlchBagContainer {
	final BlockEntityAdvancedAlchemicalChest blockEntity;
	public ContainerAdvancedAlchemicalChest(int windowId, PlayerInventory playerInv, Hand hand, IItemHandlerModifiable invBag, int selected, boolean immutable, BlockEntityAdvancedAlchemicalChest blockEntity) {
		super(windowId, playerInv, hand, invBag, selected, immutable);
		this.blockEntity = blockEntity;
		blockEntity.startOpen(playerInv.player);
	}

	@Override
	public void removed(PlayerEntity player) {
		super.removed(player);
		blockEntity.stopOpen(player);
	}

	public boolean blockEntityMatches(BlockEntityAdvancedAlchemicalChest be) {
		return blockEntity == be;
	}

	@Override
	public boolean stillValid(PlayerEntity player) {
		return ContainerBase.stillValid(player, blockEntity, () -> blockEntity.getBlockState().getBlock());
	}
}
