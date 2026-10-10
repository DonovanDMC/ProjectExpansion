package cool.furry.mc.projectexpansion.util.pipe;

import cool.furry.mc.projectexpansion.util.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// any wrench in forge's tags works, which includes Pipez's, and ours is in them too
public class Wrench {
    public static final TagKey<Item> WRENCH = TagKey.create(Platform.ITEM, new ResourceLocation("forge", "tools/wrench"));
    public static final TagKey<Item> WRENCHES = TagKey.create(Platform.ITEM, new ResourceLocation("forge", "wrenches"));

    public static boolean isWrench(ItemStack stack) {
        return stack.is(WRENCH) || stack.is(WRENCHES);
    }

    public static boolean isHoldingWrench(Player player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }
}
