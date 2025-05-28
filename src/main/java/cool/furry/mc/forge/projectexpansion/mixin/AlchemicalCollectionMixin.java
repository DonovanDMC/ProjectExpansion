package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.Enchantments;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.forge.projectexpansion.util.AlchemicalCollectionCollector;
import cool.furry.mc.forge.projectexpansion.util.AtomicBigInteger;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// This mixin enables converting mined items with EMC into EMC and knowledge
@Mixin(Block.class)
public abstract class AlchemicalCollectionMixin {
    @Inject(at = @At("RETURN"), method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", cancellable = true)
    private static void getDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> cir) {
        if(!(entity instanceof ServerPlayer player)) return;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) return;
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
        if(!state.canHarvestBlock(level, pos, player) || !hasEnch) return;
        boolean enabled = stack.getOrCreateTag().getBoolean(TagNames.ALCHEMICAL_COLLECTION_ENABLED);
        if(!enabled) {
            return;
        }
        List<ItemStack> initialDrops = cir.getReturnValue();
        AtomicBigInteger addEMC = new AtomicBigInteger();
        List<ItemStack> knowledgeAdditions = new ArrayList<>();
        List<ItemStack> newDrops = initialDrops.stream()
            .map(drop -> {
                if(proxy.hasValue(drop)) {
                    addEMC.addAndGet(BigInteger.valueOf(proxy.getSellValue(drop)).multiply(BigInteger.valueOf(drop.getCount())));
                    if(!provider.hasKnowledge(drop) && !knowledgeAdditions.contains(drop)) knowledgeAdditions.add(drop);
                    return null;
                } else return drop;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (newDrops.size() < initialDrops.size() || addEMC.get().compareTo(BigInteger.ZERO) > 0) {
            AlchemicalCollectionCollector.add(player.getUUID(), addEMC.get(), knowledgeAdditions);
            cir.setReturnValue(newDrops);
        }
    }
}
