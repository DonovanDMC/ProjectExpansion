package cool.furry.mc.projectexpansion.util;

import com.mojang.serialization.Codec;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// what the shared code needs that differs between versions, every version has its own copy
public class Platform {
    public static final ResourceKey<Registry<Level>> DIMENSION = Registries.DIMENSION;
    public static final ResourceKey<Registry<Block>> BLOCK = Registries.BLOCK;
    public static final Codec<UUID> UUID_CODEC = UUIDUtil.CODEC;
    public static final Capability<IItemHandler> ITEM_HANDLER = ForgeCapabilities.ITEM_HANDLER;
    public static final Capability<IFluidHandler> FLUID_HANDLER = ForgeCapabilities.FLUID_HANDLER;

    // darkness only exists from 1.19
    public static final MobEffect SUN_BLINDING = MobEffects.DARKNESS;

    public static MutableComponent empty() {
        return Component.empty();
    }

    public static ItemStack assemble(CraftingRecipe recipe, CraftingContainer container, Level level) {
        return recipe.assemble(container, level.registryAccess());
    }

    // ProjectE's mapper also takes the registries from 1.19
    public static void mapEmc(MinecraftServer server) {
        EMCMappingHandler.map(
                server.getServerResources().managers(),
                server.registryAccess(),
                server.getServerResources().resourceManager()
        );
    }

    // Material was removed in 1.20, blocks get their base properties and map colour from here instead
    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of();
    }

    public static BlockBehaviour.Properties blockProperties(@Nullable Matter matter) {
        MapColor color = mapColor(matter);
        return color == null ? BlockBehaviour.Properties.of() : BlockBehaviour.Properties.of().mapColor(color);
    }

    public static BlockBehaviour.Properties metalBlockProperties(Matter matter) {
        MapColor color = mapColor(matter);
        return color == null ? BlockBehaviour.Properties.of() : BlockBehaviour.Properties.of().mapColor(color);
    }

    // matter without its own colour uses the colour of ProjectE's block for it
    private static @Nullable MapColor mapColor(@Nullable Matter matter) {
        if (matter == null) return null;
        if (matter.mapColor != null) return matter.mapColor.getMapColor();
        return matter.existingBlock == null ? null : matter.existingBlock.get().defaultMapColor();
    }

    public static BlockBehaviour.Properties blockProperties(DyeColor color) {
        return BlockBehaviour.Properties.of().mapColor(color);
    }

    public static BlockBehaviour.Properties stoneInstrument(BlockBehaviour.Properties properties) {
        return properties.instrument(NoteBlockInstrument.BASEDRUM);
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        return stack.copyWithCount(count);
    }

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Level level(Entity entity) {
        return entity.level();
    }

    public static IEMCProxy emcProxy() {
        return IEMCProxy.INSTANCE;
    }

    // feedback that's always shown to whoever ran the command
    public static void sendMessage(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
    }

    public static void sendSuccess(CommandSourceStack source, Supplier<Component> message, boolean notify) {
        source.sendSuccess(message, notify);
    }

    // stacks hold components instead of nbt from 1.20.5
    public static boolean isSameItemSameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        NetworkHooks.openScreen(player, provider, extraData);
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        openScreen(player, provider, (buf) -> buf.writeBlockPos(pos));
    }

    public static ItemStack loadItemStackOptional(CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    public static CompoundTag saveItemStackOptional(ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }
}
