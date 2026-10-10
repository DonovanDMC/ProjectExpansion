package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// any wrench in the common tags works, which includes Pipez's, and ours is in them too
public class Wrench {
    public static final TagKey<Item> WRENCH = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "wrenches"));

    public static boolean isWrench(ItemStack stack) {
        return stack.is(WRENCH) || stack.is(WRENCHES);
    }

    public static boolean isHoldingWrench(Player player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }
}
