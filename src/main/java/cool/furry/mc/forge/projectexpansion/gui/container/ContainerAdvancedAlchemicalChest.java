package cool.furry.mc.forge.projectexpansion.gui.container;

import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import net.minecraftforge.items.IItemHandlerModifiable;

// yet again more "inspiration" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bd/src/main/java/moze_intel/projecte/gameObjs/container/EmcChestBlockEntityContainer.java
public class ContainerAdvancedAlchemicalChest extends AlchBagContainer {
	TileAdvancedAlchemicalChest blockEntity;
	public ContainerAdvancedAlchemicalChest(int windowId, PlayerInventory playerInv, Hand hand, IItemHandlerModifiable invBag, int selected, boolean immutable, TileAdvancedAlchemicalChest blockEntity) {
		super(windowId, playerInv, hand, invBag, selected, immutable);
		this.blockEntity = blockEntity;
		blockEntity.numPlayersUsing++;
	}

	@Override
	public void removed(PlayerEntity player) {
		super.removed(player);
		blockEntity.numPlayersUsing--;
	}
}
