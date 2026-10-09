package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.neoforge.projectexpansion.util.BasicDataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.ColorStyle;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
    @SuppressWarnings("unused")
    public ItemKnowledgeSharingBook() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(player.isCrouching()) {
            if(!level.isClientSide) {
                DataComponentTypes.OwnerData data = new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString());
                stack.set(DataComponentTypes.OWNER, data);
                level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_STORE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_STORED.translateColored(ChatFormatting.GREEN), true);
            }
            
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            DataComponentTypes.OwnerData data = stack.get(DataComponentTypes.OWNER);
            if(data != null) {
                UUID owner = data.uuid();
                if(player.getUUID().equals(owner)) {
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
                            if(Config.server.notifyKnowledgeBookGains.get() && learned < 100) {
                                player.sendSystemMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED.translateColored(ChatFormatting.GREEN, info.createStack().getDisplayName()));
                            }
                            learnerProvider.addKnowledge(info);
                            learned++;
                        }
                    }
                    stack.set(DataComponentTypes.LAST_USED, new BasicDataComponentTypes.LongValue(level.getGameTime()));
                    stack.set(DataComponentTypes.KNOWLEDGE_GAINED, new BasicDataComponentTypes.LongValue(learned));
                    if(learned > 0) {
                        learnerProvider.sync((ServerPlayer) player);
                        if(learned > 100) {
                            player.sendSystemMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_OVER_100.translateColored(ChatFormatting.GREEN, learned - 100));
                        }
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_TOTAL.translateColored(ChatFormatting.GREEN, learned, Component.literal(Util.getOwner(stack).name()).setStyle(ColorStyle.AQUA)), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    } else {
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_NO_NEW_KNOWLEDGE.translateColored(ChatFormatting.RED), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE_NONE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    }

                    ServerLevel serverLevel = (ServerLevel) level;
                    for (int i = 0; i < 5; i++) {
                        Vec3 v1 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                                .xRot(-player.getRotationVector().x * 0.017453292F)
                                .yRot(-player.getRotationVector().y * 0.017453292F);
                        Vec3 v2 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.3D, (double) (-level.random.nextFloat()) * 0.6D - 0.3D, 0.6D)
                                .xRot(-player.getRotationVector().x * 0.017453292F)
                                .yRot(-player.getRotationVector().y * 0.017453292F)
                                .add(player.position().x, player.position().y + (double) player.getEyeHeight(), player.position().z);
                        serverLevel.sendParticles(learned > 0 ? new ItemParticleOption(ParticleTypes.ITEM, stack) : ParticleTypes.SMOKE, v2.x, v2.y, v2.z, 1, v1.x, v1.y + 0.05D, v1.z, 0.0D);
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
        return stack.has(DataComponentTypes.OWNER);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if(stack.has(DataComponentTypes.OWNER)) {
            tooltip.add(Lang.Items.KNOWLEDGE_SHARING_BOOK_SELECTED.translateColored(ChatFormatting.GRAY, Component.literal(Objects.requireNonNull(stack.get(DataComponentTypes.OWNER)).name()).setStyle(ColorStyle.AQUA)));
        }
    }
}