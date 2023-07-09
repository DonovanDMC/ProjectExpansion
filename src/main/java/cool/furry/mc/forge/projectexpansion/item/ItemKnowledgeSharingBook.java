package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemKnowledgeSharingBook extends Item {
    public ItemKnowledgeSharingBook() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(player.isCrouching()) {
            if(!level.isClientSide) {
                CompoundTag nbt = stack.getOrCreateTag();
                nbt.putUUID(TagNames.OWNER, player.getUUID());
                nbt.putString(TagNames.OWNER_NAME, player.getScoreboardName());
                level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_STORE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_STORED.translateColored(ChatFormatting.GREEN), true);
            }
            
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            CompoundTag nbt = stack.getOrCreateTag();
            if(nbt.hasUUID(TagNames.OWNER)) {
                UUID owner = nbt.getUUID(TagNames.OWNER);
                if(player.getUUID().equals(nbt.getUUID(TagNames.OWNER))) {
                    player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_SELF.translateColored(ChatFormatting.RED), true);
                    return InteractionResultHolder.fail(stack);
                }
                if(!level.isClientSide) {
                    @Nullable IKnowledgeProvider ownerProvider = Util.getKnowledgeProvider(owner);
                    @Nullable IKnowledgeProvider learnerProvider = Util.getKnowledgeProvider(player);
                    if(ownerProvider == null) {
                        player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, Util.getPlayer(owner) == null ? owner : Objects.requireNonNull(Util.getPlayer(owner)).getDisplayName()), true);
                        return InteractionResultHolder.fail(stack);
                    }
                    if(learnerProvider == null) {
                        player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, player.getDisplayName()), true);
                        return InteractionResultHolder.fail(stack);
                    }
                    long learned = 0;
                    for(ItemInfo info : ownerProvider.getKnowledge()) {
                        if(!learnerProvider.hasKnowledge(info)) {
                            if(Config.notifyKnowledgeBookGains.get() && learned < 100) {
                                player.sendSystemMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED.translateColored(ChatFormatting.GREEN, info.createStack().getDisplayName()));
                            }
                            learnerProvider.addKnowledge(info);
                            learned++;
                        }
                    }
                    nbt.putLong(TagNames.LAST_USED, level.getGameTime());
                    nbt.putLong(TagNames.KNOWLEDGE_GAINED, learned);
                    if(learned > 0) {
                        learnerProvider.sync((ServerPlayer) player);
                        if(learned > 100) {
                            player.sendSystemMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_OVER_100.translateColored(ChatFormatting.GREEN, learned - 100));
                        }
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_TOTAL.translateColored(ChatFormatting.GREEN, learned, Component.literal(nbt.getString(TagNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    } else {
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_NO_NEW_KNOWLEDGE.translateColored(ChatFormatting.RED), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE_NONE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    }
                } else {
                    long gained = nbt.getLong(TagNames.KNOWLEDGE_GAINED);
                    for(int i = 0; i < 5; i++) {
                        Vec3 v1 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F);
                        Vec3 v2 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.3D, (double) (-level.random.nextFloat()) * 0.6D - 0.3D, 0.6D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F)
                            .add(player.position().x, player.position().y + (double) player.getEyeHeight(), player.position().z);
                        level.addParticle(gained > 0 ? new ItemParticleOption(ParticleTypes.ITEM, stack) : ParticleTypes.SMOKE, v2.x, v2.y, v2.z, v1.x, v1.y + 0.05D, v1.z);
                    }
                }

                stack.shrink(1);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            } else {
                player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_NO_OWNER.translateColored(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().hasUUID(TagNames.OWNER);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag nbt = stack.getOrCreateTag();
        if(nbt.hasUUID(TagNames.OWNER)) {
            tooltip.add(Lang.Items.KNOWLEDGE_SHARING_BOOK_SELECTED.translateColored(ChatFormatting.GRAY, Component.literal(nbt.getString(TagNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)));
        }
    }
}