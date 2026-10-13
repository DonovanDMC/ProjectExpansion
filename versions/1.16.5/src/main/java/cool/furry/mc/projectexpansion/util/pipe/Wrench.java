package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

// any wrench in forge's tags works, which includes Pipez's, and ours is in them too
public class Wrench {
    public static final ITag.INamedTag<Item> WRENCH = ItemTags.createOptional(new ResourceLocation("forge", "tools/wrench"));
    public static final ITag.INamedTag<Item> WRENCHES = ItemTags.createOptional(new ResourceLocation("forge", "wrenches"));

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem().is(WRENCH) || stack.getItem().is(WRENCHES);
    }

    public static boolean isHoldingWrench(PlayerEntity player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }
}
