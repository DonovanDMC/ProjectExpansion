package cool.furry.mc.projectexpansion.block;

import moze_intel.projecte.gameObjs.EnumMatterType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TieredItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

public class BlockIanthiumOre extends Block {
    public BlockIanthiumOre(AbstractBlock.Properties properties) {
        super(properties);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader level, BlockPos pos, PlayerEntity player) {
        return isMatterPickaxe(player.getMainHandItem());
    }

    // there are no tool tags before 1.17, and netherite has the same harvest level as dark matter, so the tool's tier is checked instead
    public static boolean isMatterPickaxe(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof TieredItem && ((TieredItem) item).getTier() instanceof EnumMatterType && item.getToolTypes(stack).contains(ToolType.PICKAXE);
    }
}
