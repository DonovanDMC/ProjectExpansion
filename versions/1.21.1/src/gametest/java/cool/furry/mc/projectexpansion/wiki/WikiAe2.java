package cool.furry.mc.projectexpansion.wiki;

import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.registries.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

// placing ae2's cables & parts, like a player would, for menus & scenes
public final class WikiAe2 {
    private WikiAe2() {}

    // a transmutation interface at pos with a storage bus on a cable facing it, a crafting terminal on a cable above that, then a drive, controller & creative energy cell
    // going west, which shows on the right from the front, and the owner knows stone with enough emc for as many as the interface gives
    public static void interfaceNetwork(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        WikiBlocks.place(level, pos, Blocks.TRANSMUTATION_INTERFACE.get());
        ((BlockEntityOwnable) Objects.requireNonNull(level.getBlockEntity(pos))).setOwner(player);
        WikiPlayer.learn(player, new ItemStack(Items.STONE));
        WikiPlayer.setEmc(player, BigInteger.TEN.pow(12));
        BlockPos bus = pos.west();
        part(player, bus, null, "fluix_glass_cable");
        part(player, bus, Direction.EAST, "storage_bus");
        part(player, bus.above(), null, "fluix_glass_cable");
        part(player, bus.above(), Direction.NORTH, "crafting_terminal");
        WikiBlocks.place(level, pos.west(2), block("drive"));
        WikiBlocks.place(level, pos.west(3), block("controller"));
        WikiBlocks.place(level, pos.west(4), block("creative_energy_cell"));
    }

    // where interfaceNetwork's terminal is
    public static BlockPos terminal(BlockPos pos) {
        return pos.west().above();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("ae2", id)).orElseThrow(() -> new IllegalStateException("ae2:" + id + " isn't there"));
    }

    // a part on a side of the cable bus at pos, or the cable itself with no side
    public static void part(ServerPlayer player, BlockPos pos, @Nullable Direction side, String id) {
        if (!(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", id)) instanceof IPartItem<?> item)) throw new IllegalStateException("ae2:" + id + " isn't an ae2 part");
        if (PartHelper.setPart(player.serverLevel(), pos, side, player, item) == null) throw new IllegalStateException("Couldn't place ae2:" + id + " at " + pos);
    }
}
