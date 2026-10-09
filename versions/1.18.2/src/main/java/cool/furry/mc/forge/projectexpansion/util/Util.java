package cool.furry.mc.forge.projectexpansion.util;

import com.mojang.serialization.Codec;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.nss.AbstractNSSTag;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.items.IFireProtector;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.SerializableUUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);
    public static final TagKey<Block> EMPTY_TAG = TagKey.create(Registry.BLOCK_REGISTRY, Main.rl("empty"));
    public static final Codec<BigInteger> BIG_INTEGER_CODEC = Codec.STRING.xmap(val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);
    public static final Codec<Player> PLAYER_CODEC = SerializableUUID.CODEC.xmap(Util::getPlayer, Player::getUUID);
    public static final Function<String, ResourceLocation> SUN_EXPOSURE_PROTECTION = slot -> Main.rl(String.format("sun_exposure_protection_%s", slot));
    public static final String WIKI = "https://github.com/DonovanDMC/ProjectExpansion/wiki";

    // CommandSourceStack#getPlayer was added in 1.19
    public static @Nullable ServerPlayer getPlayer(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    public static @Nullable ServerPlayer getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    public static @Nullable ServerPlayer getPlayer(@Nullable Level level, UUID uuid) {
        return level == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageableItem()) stackCopy.setDamageValue(0);
        return ProjectEAPI.getEMCProxy().getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemStack rawStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack(rawStack)));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemInfo rawInfo) {
        return addKnowledge(player, provider, rawInfo, rawInfo);
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
        @Nullable ServerPlayer player = getPlayer(uuid);
        if(player == null) {
            return null;
        }
        return getKnowledgeProvider(player);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(Player player) {
        return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
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
        SUCCESS;

        public boolean fail() { return this == FAIL; }
        public boolean unknown() { return this == UNKNOWN; }
        public boolean success() { return this == SUCCESS; }
    }

    public static @Nullable ServerLevel getDimension(ResourceKey<Level> dimension) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getLevel(dimension);
    }

    public static Style suggestCommand(Style style, String command) {
        return style
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(command).withStyle(ChatFormatting.RED)));
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
        float pfValue = (float) ProjectEAPI.getEMCProxy().getValue(Objects.requireNonNull(Matter.FINAL.getPowerFlower()));
        float sunValue = (float) ProjectEAPI.getEMCProxy().getValue(Blocks.COMPACT_SUN.get());
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
    public static EnumMatterType getMatterForProjectE(Matter matter) {
        return getMatterForProjectE(matter, Matter.RED);
    }

    public static EnumMatterType getMatterForProjectE(Matter matter, Matter max) {
        int start = Math.min(matter.ordinal(), max.ordinal());
        for (int i = start; i >= 0; i--) {
            EnumMatterType type = Matter.VALUES[i].getPeMatterType();
            if (type != null) return type;
        }
        for (int i = start + 1; i < Matter.VALUES.length; i++) {
            EnumMatterType type = Matter.VALUES[i].getPeMatterType();
            if (type != null) return type;
        }
        throw new IllegalStateException("No PE matter type found in Matter enum");
    }


    public static void openTransmutationTable(ServerPlayer player) {
        NetworkHooks.openGui(player, new TransmutationContainerProvider(null), (buf) -> {
            buf.writeBoolean(false);
        });
    }

    public static void openTransmutationTable(ServerPlayer player, InteractionHand hand) {
        NetworkHooks.openGui(player, new TransmutationContainerProvider(hand), (buf) -> {
            buf.writeBoolean(true);
            buf.writeEnum(hand);
            buf.writeByte(player.getInventory().selected);
        });
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) return false;
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        if (stack1.isEmpty() || stack2.isEmpty()) return false;

        CompoundTag stack1NBT = stack1.save(new CompoundTag());
        CompoundTag stack2NBT = stack2.save(new CompoundTag());
        stack1NBT.remove("Count");
        stack2NBT.remove("Count");

        return stack1NBT.toString().equals(stack2NBT.toString());
    }

    public static ItemStack returnToInventoryOrTransmutation(Inventory playerInv, Player player, IKnowledgeProvider provider, ItemStack stack) {
        return returnToInventoryOrTransmutation(playerInv, player, provider, stack, false, false);
    }

    public static ItemStack returnToInventoryOrTransmutation(Inventory playerInv, Player player, IKnowledgeProvider provider, ItemStack stack, boolean ignoreNBT) {
        return returnToInventoryOrTransmutation(playerInv, player, provider, stack, ignoreNBT, false);
    }

    // converting items back at full value is only fair when there's no covalence loss, otherwise it beats burning them normally
    public static boolean canConvertWithoutLoss(ItemStack stack) {
        return ProjectEConfig.server.difficulty.covalenceLoss.get() >= 1D && ProjectEAPI.getEMCProxy().hasValue(stack) && !EmcLock.isLocked(stack);
    }

    public static ItemStack returnToInventoryOrTransmutation(Inventory playerInv, Player player, IKnowledgeProvider provider, ItemStack stack, boolean ignoreNBT, boolean force) {
        if (!canConvertWithoutLoss(stack) || (!ignoreNBT && !areStacksEqual(stack, cleanStack(stack)))) return returnToInventory(playerInv, player, stack, force);
        addKnowledge(player, provider, stack);
        long value = ProjectEAPI.getEMCProxy().getValue(stack);
        BigInteger emc = provider.getEmc().add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(stack.getCount())));
        provider.setEmc(emc);
        if (player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        return ItemStack.EMPTY;
    }

    public static ItemStack returnToInventory(Inventory playerInv, Player player, ItemStack stack) {
        return returnToInventory(playerInv, player, stack, false);
    }

    public static ItemStack returnToInventory(Inventory playerInv, Player player, ItemStack stack, boolean force) {
        stack = stack.copy();
        while (true) { // placeItemBackInInventory drops excess items
            if (stack.isEmpty()) break;

            int slot = playerInv.getSlotWithRemainingSpace(stack);
            if (slot == -1) slot = playerInv.getFreeSlot();

            if (slot != -1) {
                int canMove = Math.min(stack.getCount(), stack.getMaxStackSize() - playerInv.getItem(slot).getCount());

                if (canMove > 0) {
                    ItemStack moved = stack.split(canMove);
                    playerInv.add(slot, moved);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                                ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0,
                                slot, playerInv.getItem(slot)
                        ));
                    }

                    continue;
                }
            }

            if (force) {
                player.drop(stack, false);
                stack = ItemStack.EMPTY;
                continue;
            }

            break;
        }

        return stack;
    }

    // ProjectE's active flag, a plain nbt boolean before data components
    public static boolean isActive(ItemStack stack, boolean fallback) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TagNames.ACTIVE) ? tag.getBoolean(TagNames.ACTIVE) : fallback;
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(TagNames.ACTIVE, active);
    }

    public static OwnerData getOwner(ItemStack stack) {
        OwnerData owner = OwnerData.get(stack);
        return owner == null ? new OwnerData(DUMMY_UUID, "None") : owner;
    }

    public static long reloadEMC(MinecraftServer server) {
        long start = System.currentTimeMillis();

        AbstractNSSTag.clearCreatedTags();
        CustomEMCParser.init();
        EMCMappingHandler.map(
                server.getServerResources().managers(),
                server.getServerResources().resourceManager()
        );

        moze_intel.projecte.network.PacketHandler.sendFragmentedEmcPacketToAll();

        return System.currentTimeMillis() - start;
    }

    public static Optional<BlockEntity> getBlockEntity(BlockGetter level, BlockPos pos) {
        return Optional.ofNullable(WorldHelper.getBlockEntity(level, pos));
    }

    public static <BE extends BlockEntity> Optional<BE> getBlockEntity(Class<BE> clazz, BlockGetter level, BlockPos pos) {
        return Optional.ofNullable(WorldHelper.getBlockEntity(clazz, level, pos));
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, BlockGetter level, BlockPos pos) {
        return getCapability(capability, level, pos, null);
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntity(level, pos).flatMap(be -> be.getCapability(capability, side).resolve());
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Class<? extends BlockEntity> clazz, Capability<CAPABILITY> capability, BlockGetter level, BlockPos pos) {
        return getCapability(clazz, capability, level, pos, null);
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Class<? extends BlockEntity> clazz, Capability<CAPABILITY> capability, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntity(clazz, level, pos).flatMap(be -> be.getCapability(capability, side).resolve());
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, Entity entity) {
        return getCapability(capability, entity, null);
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, Entity entity, @Nullable Direction side) {
        return entity.getCapability(capability, side).resolve();
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Class<? extends Entity> clazz, Capability<CAPABILITY> capability, Entity entity) {
        return getCapability(clazz, capability, entity, null);
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Class<? extends Entity> clazz, Capability<CAPABILITY> capability, Entity entity, @Nullable Direction side) {
        if (!clazz.isInstance(entity)) return Optional.empty();
        return entity.getCapability(capability, side).resolve();
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, ItemStack stack) {
        return getCapability(capability, stack, null);
    }

    public static <CAPABILITY> Optional<CAPABILITY> getCapability(Capability<CAPABILITY> capability, ItemStack stack, @Nullable Direction side) {
        return stack.getCapability(capability, side).resolve();
    }
}
