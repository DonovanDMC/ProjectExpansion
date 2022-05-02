package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayer getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    public static @Nullable
    ServerPlayer getPlayer(@Nullable Level level, UUID uuid) {
        return level == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageableItem()) stackCopy.setDamageValue(0);
        return NBTManager.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, Item rawItem, Item cleanItem) {
        return addKnowledge(player, provider, ItemInfo.fromItem(rawItem), ItemInfo.fromItem(cleanItem));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemStack rawStack, ItemStack cleanStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemInfo rawInfo, ItemInfo cleanInfo) {
        if (cleanInfo.createStack().isEmpty()) return AddKnowledgeResult.FAIL;

        if (!provider.hasKnowledge(cleanInfo)) {
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo)))
                return AddKnowledgeResult.FAIL;

            provider.addKnowledge(cleanInfo);
            return AddKnowledgeResult.SUCCESS;
        }

        return AddKnowledgeResult.UNKNOWN;
    }

    public static int safeIntValue(BigInteger val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static long safeLongValue(BigInteger val) {
        try {
            return val.longValueExact();
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    public static int mod(int a, int b) {
        a = a % b;
        return a < 0 ? a + b : a;
    }

    public static void markDirty(BlockEntity block) {
        if (block.getLevel() != null) markDirty(block.getLevel(), block);
    }

    public static void markDirty(Level level, BlockEntity block) {
        markDirty(level, block.getBlockPos());
    }

    public static void markDirty(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
    }

    public static NonNullSupplier<IllegalStateException> knowledgeProviderError = () -> new IllegalStateException("Failed to get knowledge provider.");

    public static <T extends Capability<C>, C> void usePlayerCapability(Player player, T cap, NonNullConsumer<C> consumer) {
        player.getCapability(cap).ifPresent(consumer);
    }

    public enum AddKnowledgeResult {
        FAIL,
        UNKNOWN,
        SUCCESS,
    }
}
