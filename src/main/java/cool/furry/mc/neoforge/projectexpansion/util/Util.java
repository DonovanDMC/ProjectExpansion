package cool.furry.mc.neoforge.projectexpansion.util;

import com.mojang.serialization.Codec;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.Blocks;
import io.netty.buffer.ByteBuf;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.items.IFireProtector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);
    public static final TagKey<Block> EMPTY_TAG = TagKey.create(Registries.BLOCK, Main.rl("empty"));
    public static final Codec<BigInteger> BIG_INTEGER_CODEC = Codec.STRING.xmap(val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);
    public static final StreamCodec<ByteBuf, BigInteger> BIG_INTEGER_STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);
    public static final Codec<Player> PLAYER_CODEC = UUIDUtil.CODEC.xmap(Util::getPlayer, Player::getUUID);
    public static final StreamCodec<ByteBuf, Player> PLAYER_STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(Util::getPlayer, Player::getUUID);
    public static final Codec<ServerPlayer> SERVER_PLAYER_CODEC = UUIDUtil.CODEC.xmap(Util::getServerPlayer, ServerPlayer::getUUID);
    public static final StreamCodec<ByteBuf, ServerPlayer> SERVER_PLAYER_STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(Util::getServerPlayer, ServerPlayer::getUUID);
    public static final Function<String, ResourceLocation> SUN_EXPOSURE_PROTECTION = slot -> Main.rl(String.format("sun_exposure_protection_%s", slot));
    public static final String WIKI = "https://github.com/DonovanDMC/ProjectExpansion/wiki";

    public static @Nullable Player getPlayer(UUID uuid) {
        if (EffectiveSide.get() == LogicalSide.SERVER) {
            return getServerPlayer(uuid);
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.getUUID().equals(uuid)) {
                return player;
            }

            return null;
        }
    }

    public static @Nullable ServerPlayer getServerPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    public static @Nullable ServerPlayer getServerPlayer(@Nullable Level level, UUID uuid) {
        return level == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = stack.copyWithCount(1);
        if (stackCopy.isDamageableItem()) stackCopy.setDamageValue(0);
        return IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
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
            if (NeoForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo)).isCanceled())
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

    public static int safeIntValue(BigDecimal val) {
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
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(UUID uuid) {
        @Nullable ServerPlayer player = getServerPlayer(uuid);
        if(player == null) {
            return null;
        }
        return getKnowledgeProvider(player);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(Player player) {
        return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
    }

    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList) {
        return spreadEMC(emc, storageList, null);
    }

    /**
     * Spreads EMC evenly across multiple targets
     * @param emc EMC to spread
     * @param storageList List of targets
     * @param maxPer Maximum amount to insert per target
     * @return The remaining EMC
     */
    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList, @Nullable BigInteger maxPer) {
        if (emc.equals(BigInteger.ZERO) || storageList.isEmpty()) return emc;
        List<IEmcStorage> notAccepting = new ArrayList<>();
        Supplier<List<IEmcStorage>> getStorages = () -> storageList.stream().filter(storage -> !notAccepting.contains(storage)).toList();
        BigInteger original = emc;
        while (emc.compareTo(BigInteger.ZERO) > 0) {
            List<IEmcStorage> acceptingStorages = getStorages.get();
            if (acceptingStorages.isEmpty()) break;
            BigInteger perStorage = emc.divide(BigInteger.valueOf(acceptingStorages.size())).max(BigInteger.ONE);
            if (maxPer != null && maxPer.compareTo(perStorage) < 0) perStorage = maxPer;
            for (IEmcStorage storage : acceptingStorages) {
                if (emc.equals(BigInteger.ZERO)) break;
                BigInteger toAdd = perStorage;
                if (emc.compareTo(perStorage) < 0) toAdd = emc;
                BigInteger accepted;
                if (storage instanceof IEmcStorageBigInteger bigStorage) {
                    accepted = bigStorage.insertEmcBigInteger(toAdd, EmcAction.EXECUTE);
                } else {
                    BigInteger remaining = stepBigInteger(toAdd, (value) -> {
                        long added = storage.insertEmc(value, EmcAction.EXECUTE);
                        return value - added;
                    });
                    accepted = toAdd.subtract(remaining);
                }
                if (accepted.compareTo(toAdd) < 0) notAccepting.add(storage);
                emc = emc.subtract(accepted);
            }
        }

        return emc;
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

    // consumer values: step, leftover
    // consumer return: leftover (from step)
    public static BigInteger stepBigInteger(BigInteger value, Long step, BiFunction<Long, BigInteger, Long> func) {
        if (value.equals(BigInteger.ZERO)) return value;
        long localValue;
        while ((localValue = Math.min(step, safeLongValue(value))) > 0L) {
            value = value.subtract(BigInteger.valueOf(localValue));
            Long unused = func.apply(localValue, value);
            if (unused > 0L) {
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

    public static @Nullable ServerLevel getDimension(ResourceKey<Level> dimension) {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dimension);
    }

    public static Style suggestCommand(Style style, String command) {
        return style
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command).withStyle(ChatFormatting.RED)));
    }

    public static BigDecimal divide(BigInteger dividend, BigInteger divisor) {
        return new BigDecimal(dividend).divide(new BigDecimal(divisor), 2, RoundingMode.HALF_EVEN);
    }

    public static int scaleToRedstone(long currentAmount, long max) {
        return scaleToRedstone(BigInteger.valueOf(currentAmount), BigInteger.valueOf(max));
    }

    /**
     * Scales this proportion into redstone, where 0 means none, 15 means full, and the rest are an appropriate scaling.
     */
    public static int scaleToRedstone(BigInteger currentAmount, BigInteger max) {
        double proportion = divide(currentAmount, max).doubleValue();
        if (currentAmount.compareTo(BigInteger.ZERO) <= 0) {
            return 0;
        }
        if (currentAmount.compareTo(max) >= 0) {
            return 15;
        }
        return (int) Math.round(proportion * 13 + 1);
    }

    public static boolean isImmuneToFire(ServerPlayer player) {
        return isImmuneToFire(player, 0);
    }

    /**
     * Checks if the player is immune to fire damage
     * @param player The player to check
     * @param timeTolerance If the player is immune to fire for this amount of time or less, they will be considered not immune
     * @return If the player is immune to fire
     */
    public static boolean isImmuneToFire(ServerPlayer player, int timeTolerance) {
        ItemStack chestplate = player.getInventory().getArmor(EquipmentSlot.CHEST.getIndex());
        if (chestplate.getItem() instanceof IFireProtector protector && protector.canProtectAgainstFire(chestplate, player)) {
            return true;
        }

        @Nullable MobEffectInstance fireResistance = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (fireResistance != null && fireResistance.getDuration() > timeTolerance) {
            return true;
        }

        return player.fireImmune();
    }

    public static String ucwords(String str) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalize = true;
            } else if (capitalize) {
                c = Character.toTitleCase(c);
                capitalize = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static int getSunMultiplier() {
        float pfValue = (float) IEMCProxy.INSTANCE.getValue(Objects.requireNonNull(Matter.FINAL.getPowerFlower()));
        float sunValue = (float) IEMCProxy.INSTANCE.getValue(Blocks.COMPACT_SUN.get());
        if (pfValue == 0 || sunValue == 0) return Config.server.compactSunBonus.get();
        int diff = (int) Math.ceil(sunValue / pfValue) + 1;
        // round to nearest 10
        return ((int) Math.ceil(diff / 10.0) * 10);
    }

    public static int getSunBonus() {
        if (Config.server.sunMultiplierPriceCompensation.get()) {
            int priceBonus = getSunMultiplier();
            int staticBonus = Config.server.compactSunBonus.get();
            return Math.max(priceBonus, staticBonus);
        } else {
            return Config.server.compactSunBonus.get();
        }
    }

    // since we don't have tools we're forced to cap everything at red matter
    public static Matter getMatterForProjectE(Matter matter) {
        return getMatterForProjectE(matter, Matter.RED);
    }

    public static Matter getMatterForProjectE(Matter matter, Matter max) {
        if (matter.getMatterTier() > max.getMatterTier()) {
            return max;
        }

        return matter;
    }
}
