package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static moze_intel.projecte.api.capabilities.tile.IEmcStorage.EmcAction;
@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayerEntity getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static @Nullable
    ServerPlayerEntity getPlayer(@Nullable World world, UUID uuid) {
        return world == null || world.getServer() == null ? null : world.getServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageable()) stackCopy.setDamage(0);
        return NBTManager.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, Item rawItem, Item cleanItem) {
        return addKnowledge(player, provider, ItemInfo.fromItem(rawItem), ItemInfo.fromItem(cleanItem));
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, ItemStack rawStack, ItemStack cleanStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack));
    }

    public static AddKnowledgeResult addKnowledge(PlayerEntity player, IKnowledgeProvider provider, ItemInfo rawInfo, ItemInfo cleanInfo) {
        if (cleanInfo.createStack().isEmpty()) return AddKnowledgeResult.FAIL;

        if (!provider.hasKnowledge(cleanInfo)) {
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo))) return AddKnowledgeResult.FAIL;

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

    public static BigInteger stepBigInteger(BigInteger value, Function<Long, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    public static BigInteger stepBigInteger(BigInteger value, Long step, Function<Long, Long> func) {
        return stepBigInteger(value, step, (a, b) -> func.apply(a));
    }

    public static BigInteger stepBigInteger(BigInteger value, BiFunction<Long, BigInteger, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList) {
        return spreadEMC(emc, storageList, null);
    }
    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList, @Nullable Long maxPer) {
        if(emc.equals(BigInteger.ZERO) || storageList.isEmpty()) return emc;
        List<IEmcStorage> notAccepting = new ArrayList<>();
        emc = stepBigInteger(emc, (val) -> {
            long div = val / storageList.size();
            if(maxPer != null && maxPer < div) div = maxPer;
            parentLoop: while (val > 0 && notAccepting.size() < storageList.size()) {
                for(IEmcStorage storage : storageList) {
                    if(notAccepting.contains(storage)) continue;
                    if(val == 0) break parentLoop;
                    if(val < div) div = val;
                    long oldVal = val;
                    val -= storage.insertEmc(div, EmcAction.EXECUTE);
                    if(val.equals(oldVal)) notAccepting.add(storage);
                }
            }
            return val;
        });
        return emc;
    }

    // consumer values: step, leftover
    // consumer return: leftover (from step)
    public static BigInteger stepBigInteger(BigInteger value, Long step, BiFunction<Long, BigInteger, Long> func) {
        if(value.equals(BigInteger.ZERO)) return value;
        long localValue;
        while((localValue = Math.min(step, safeLongValue(value))) > 0L) {
            value = value.subtract(BigInteger.valueOf(localValue));
            Long unused = func.apply(localValue, value);
            if(unused > 0L) {
                value = value.add(BigInteger.valueOf(unused));
                break;
            }
        }
        return value;
    }

    public enum AddKnowledgeResult {
        FAIL,
        UNKNOWN,
        SUCCESS,
    }
}