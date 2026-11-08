package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.registries.Enchantments;
import cool.furry.mc.projectexpansion.util.AlchemicalCollectionCollector;
import cool.furry.mc.projectexpansion.util.AtomicBigInteger;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
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
@Mixin(Block.class)
public abstract class AlchemicalCollectionMixin {
    @Inject(at = @At("RETURN"), method = "getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", cancellable = true)
    private static void getDrops(BlockState state, ServerWorld level, BlockPos pos, TileEntity blockEntity, Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(entity instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = ((ServerPlayerEntity) entity);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) return;
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
        if (!state.canHarvestBlock(level, pos, player) || !hasEnch) return;
        boolean enabled = Util.isActive(stack, false);
        if (!enabled) {
            return;
        }
        List<ItemStack> initialDrops = cir.getReturnValue();
        AtomicBigInteger addEMC = new AtomicBigInteger();
        List<ItemInfo> knowledgeAdditions = new ArrayList<>();
        List<ItemStack> newDrops = initialDrops.stream()
            .map(drop -> {
                ItemInfo raw = ItemInfo.fromStack(drop);
                ItemInfo info = ProjectEAPI.getEMCProxy().getPersistentInfo(raw);
                // anything carrying more than projecte keeps, like a filled shulker box or stored emc, is dropped as it is so that isn't lost
                if(info.equals(raw) && proxy.hasValue(info)) {
                    addEMC.addAndGet(BigInteger.valueOf(proxy.getSellValue(info)).multiply(BigInteger.valueOf(drop.getCount())));
                    if(!provider.hasKnowledge(info) && !knowledgeAdditions.contains(info)) knowledgeAdditions.add(info);
                    return null;
                } else return drop;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // a block's inventory is dropped by the block itself, what an item handler reports isn't always real (like an emc link's)
        if (newDrops.size() < initialDrops.size() || addEMC.get().compareTo(BigInteger.ZERO) > 0) {
            AlchemicalCollectionCollector.add(player.getUUID(), addEMC.get(), knowledgeAdditions);
            cir.setReturnValue(newDrops);
        }
    }
}
