package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

// yet again more "inspiration" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bd/src/main/java/moze_intel/projecte/gameObjs/container/EmcChestBlockEntityContainer.java
public class ContainerAdvancedAlchemicalChest extends AlchBagContainer {
	BlockEntityAdvancedAlchemicalChest blockEntity;
	public ContainerAdvancedAlchemicalChest(int windowId, Inventory playerInv, InteractionHand hand, IItemHandlerModifiable invBag, int selected, boolean immutable, BlockEntityAdvancedAlchemicalChest blockEntity) {
		super(windowId, playerInv, hand, invBag, selected, immutable);
		this.blockEntity = blockEntity;
		blockEntity.startOpen(playerInv.player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		blockEntity.stopOpen(player);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return true;
	}

	public boolean blockEntityMatches(BlockEntityAdvancedAlchemicalChest be) {
		return blockEntity == be;
	}
}
