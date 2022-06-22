package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemKnowledgeSharingBook extends Item {
    public ItemKnowledgeSharingBook() {
        super(new Item.Properties().maxStackSize(1).rarity(Rarity.RARE).group(Main.group));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(player.isSneaking()) {
            if(!world.isRemote) {
                CompoundNBT nbt = stack.getOrCreateTag();
                nbt.putUniqueId(NBTNames.OWNER, player.getUniqueID());
                nbt.putString(NBTNames.OWNER_NAME, player.getScoreboardName());
                world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.KNOWLEDGE_SHARING_BOOK_STORE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + world.rand.nextFloat() * 0.4F);
                player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.stored").setStyle(ColorStyle.GREEN), true);
            }

            return ActionResult.func_233538_a_(stack, world.isRemote);
        } else {
            CompoundNBT nbt = stack.getOrCreateTag();
            if(nbt.hasUniqueId(NBTNames.OWNER)) {
                UUID owner = nbt.getUniqueId(NBTNames.OWNER);
                if(player.getUniqueID().equals(nbt.getUniqueId(NBTNames.OWNER))) {
                    player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.self").setStyle(ColorStyle.RED), true);
                    return ActionResult.resultFail(stack);
                }
                if(!world.isRemote) {
                    IKnowledgeProvider ownerProvider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
                    IKnowledgeProvider learnerProvider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
                    long learned = 0;
                    for(ItemInfo info : ownerProvider.getKnowledge()) {
                        if(!learnerProvider.hasKnowledge(info)) {
                            if(Config.notifyKnowledgeBookGains.get() && learned < 100) {
                                player.sendMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.learned", info.createStack().getTextComponent()).setStyle(ColorStyle.GREEN), Util.DUMMY_UUID);
                            }
                            learnerProvider.addKnowledge(info);
                            learned++;
                        }
                    }
                    nbt.putLong(NBTNames.LAST_USED, world.getGameTime());
                    nbt.putLong(NBTNames.KNOWLEDGE_GAINED, learned);
                    if(learned > 0) {
                        learnerProvider.sync((ServerPlayerEntity) player);
                        if(learned > 100) {
                            player.sendMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.learned_over_100", learned - 100).setStyle(ColorStyle.GREEN), Util.DUMMY_UUID);
                        }
                        player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.learned_total", learned, new StringTextComponent(nbt.getString(NBTNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)).setStyle(ColorStyle.GREEN), true);
                        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.KNOWLEDGE_SHARING_BOOK_USE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + world.rand.nextFloat() * 0.4F);
                    } else {
                        player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.no_new_knowledge").setStyle(ColorStyle.RED), true);
                        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.KNOWLEDGE_SHARING_BOOK_USE_NONE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + world.rand.nextFloat() * 0.4F);
                    }
                } else {
                    long gained = nbt.getLong(NBTNames.KNOWLEDGE_GAINED);
                    for(int i = 0; i < 5; i++) {
                        Vector3d v1 = new Vector3d(((double) world.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                            .rotatePitch(-player.rotationPitch * 0.017453292F)
                            .rotateYaw(-player.rotationYaw * 0.017453292F);
                        Vector3d v2 = new Vector3d(((double) world.rand.nextFloat() - 0.5D) * 0.3D, (double) (-world.rand.nextFloat()) * 0.6D - 0.3D, 0.6D)
                            .rotatePitch(-player.rotationPitch * 0.017453292F)
                            .rotateYaw(-player.rotationYaw * 0.017453292F)
                            .add(player.getPosX(), player.getPosY() + (double) player.getEyeHeight(), player.getPosZ());
                        world.addParticle(gained > 0 ? new ItemParticleData(ParticleTypes.ITEM, stack) : ParticleTypes.SMOKE, v2.x, v2.y, v2.z, v1.x, v1.y + 0.05D, v1.z);
                    }
                }

                stack.shrink(1);
                return ActionResult.func_233538_a_(stack, world.isRemote);
            } else {
                player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.no_owner").setStyle(ColorStyle.RED), true);
                return ActionResult.resultFail(stack);
            }
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return stack.getOrCreateTag().hasUniqueId(NBTNames.OWNER);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        CompoundNBT nbt = stack.getOrCreateTag();
        if(nbt.hasUniqueId(NBTNames.OWNER)) {
            tooltip.add(new TranslationTextComponent("item.projectexpansion.knowledge_sharing_book.selected", new StringTextComponent(nbt.getString(NBTNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)).setStyle(ColorStyle.GRAY));
        }
    }
}
