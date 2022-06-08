package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.registries.Enchantments;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Mixin(Block.class)
public abstract class EnchantmentMixin {
    @Inject(at = @At("RETURN"), method = "getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", cancellable = true)
    private static void getDrops(BlockState state, ServerWorld world, BlockPos pos, TileEntity tileEntity, Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> cir) {
        if(!(entity instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) entity;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        boolean hasEnch = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
        if(!state.canHarvestBlock(world, pos, player) || !hasEnch) return;
        List<ItemStack> initialDrops = cir.getReturnValue();
        AtomicLong addEMC = new AtomicLong();
        List<ItemStack> knowledgeAdditions = new ArrayList<>();
        List<ItemStack> newDrops = initialDrops.stream()
            .map(drop -> {
                if(proxy.hasValue(drop)) {
                    addEMC.addAndGet(proxy.getValue(drop));
                    if(!provider.hasKnowledge(drop)) knowledgeAdditions.add(drop);
                    return null;
                } else return drop;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (newDrops.size() < initialDrops.size() || addEMC.get() > 0) {
            provider.setEmc(provider.getEmc().add(BigInteger.valueOf(addEMC.get())));
            if(knowledgeAdditions.size() > 0) {
                knowledgeAdditions.forEach(knowledge -> {
                    if(provider.addKnowledge(knowledge)) provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromStack(stack)), true);
                });
            }
            provider.syncEmc(player);
            world.playSound(null, pos, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.BLOCKS, 1f, 0.75f);
            cir.setReturnValue(newDrops);
        }
    }
}
