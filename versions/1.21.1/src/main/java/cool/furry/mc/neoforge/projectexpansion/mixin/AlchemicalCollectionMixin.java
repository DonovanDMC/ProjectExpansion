package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.registries.Enchantments;
import cool.furry.mc.neoforge.projectexpansion.util.AlchemicalCollectionCollector;
import cool.furry.mc.neoforge.projectexpansion.util.AtomicBigInteger;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// This mixin enables converting mined items with EMC into EMC and knowledge
@Mixin(value = Block.class, remap = false)
public abstract class AlchemicalCollectionMixin {
    @Inject(at = @At("RETURN"), method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", cancellable = true)
    private static void getDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(entity instanceof ServerPlayer player)) return;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) return;
        IEMCProxy proxy = IEMCProxy.INSTANCE;
        boolean hasEnch = EnchantmentHelper.getTagEnchantmentLevel(level.registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION), stack) > 0;
        if (!state.canHarvestBlock(level, pos, player) || !hasEnch) return;
        boolean enabled = stack.getOrDefault(PEDataComponentTypes.ACTIVE, false);
        if (!enabled) {
            return;
        }
        List<ItemStack> initialDrops = cir.getReturnValue();
        AtomicBigInteger addEMC = new AtomicBigInteger();
        List<ItemInfo> knowledgeAdditions = new ArrayList<>();
        List<ItemStack> newDrops = initialDrops.stream()
            .map(drop -> {
                ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(drop));
                if(proxy.hasValue(info)) {
                    addEMC.addAndGet(BigInteger.valueOf(proxy.getSellValue(info)).multiply(BigInteger.valueOf(drop.getCount())));
                    if(!provider.hasKnowledge(info) && !knowledgeAdditions.contains(info)) knowledgeAdditions.add(info);
                    return null;
                } else return drop;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (newDrops.size() < initialDrops.size() || addEMC.get().compareTo(BigInteger.ZERO) > 0) {
            @Nullable IItemHandler handler = WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack slotStack = handler.getStackInSlot(i);
                    if (!slotStack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slotStack);
                    }
                }
            }

            AlchemicalCollectionCollector.add(player.getUUID(), addEMC.get(), knowledgeAdditions);
            cir.setReturnValue(newDrops);
        }
    }
}
