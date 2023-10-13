package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemKnowledgeSharingBook extends Item {
    public ItemKnowledgeSharingBook() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand){
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide) {
                CompoundNBT nbt = stack.getOrCreateTag();
                nbt.putUUID(NBTNames.OWNER, player.getUUID());
                nbt.putString(NBTNames.OWNER_NAME, player.getScoreboardName());
                level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_STORE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_STORED.translateColored(TextFormatting.GREEN), true);
            }

            return ActionResult.sidedSuccess(stack, level.isClientSide);
        } else {
            CompoundNBT nbt = stack.getOrCreateTag();
            if (nbt.hasUUID(NBTNames.OWNER)) {
                UUID owner = nbt.getUUID(NBTNames.OWNER);
                if (player.getUUID().equals(nbt.getUUID(NBTNames.OWNER))) {
                    player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_SELF.translateColored(TextFormatting.RED), true);
                    return ActionResult.fail(stack);
                }
                if (!level.isClientSide) {
                    @Nullable IKnowledgeProvider ownerProvider = Util.getKnowledgeProvider(owner);
                    @Nullable IKnowledgeProvider learnerProvider = Util.getKnowledgeProvider(player);
                    if(ownerProvider == null) {
                        player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, Util.getPlayer(owner) == null ? owner : Objects.requireNonNull(Util.getPlayer(owner)).getDisplayName()), true);
                        return ActionResult.fail(stack);
                    }
                    if(learnerProvider == null) {
                        player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, player.getDisplayName()), true);
                        return ActionResult.fail(stack);
                    }
                    long learned = 0;
                    for (ItemInfo info : ownerProvider.getKnowledge()) {
                        if (!learnerProvider.hasKnowledge(info)) {
                            if (Config.notifyKnowledgeBookGains.get() && learned < 100) {
                                player.sendMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED.translateColored(TextFormatting.GREEN, info.createStack().getDisplayName()), Util.DUMMY_UUID);
                            }
                            learnerProvider.addKnowledge(info);
                            learned++;
                        }
                    }
                    nbt.putLong(NBTNames.LAST_USED, level.getGameTime());
                    nbt.putLong(NBTNames.KNOWLEDGE_GAINED, learned);
                    if (learned > 0) {
                        learnerProvider.sync((ServerPlayerEntity) player);
                        if (learned > 100) {
                            player.sendMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_OVER_100.translateColored(TextFormatting.GREEN, learned - 100), Util.DUMMY_UUID);
                        }
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_LEARNED_TOTAL.translateColored(TextFormatting.GREEN, learned, new StringTextComponent(nbt.getString(NBTNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    } else {
                        player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_NO_NEW_KNOWLEDGE.translateColored(TextFormatting.RED), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE_NONE.get(), SoundCategory.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    }
                } else {
                    long gained = nbt.getLong(NBTNames.KNOWLEDGE_GAINED);
                    for (int i = 0; i < 5; i++) {
                        Vector3d v1 = new Vector3d(((double) level.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F);
                        Vector3d v2 = new Vector3d(((double) level.random.nextFloat() - 0.5D) * 0.3D, (double) (-level.random.nextFloat()) * 0.6D - 0.3D, 0.6D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F)
                            .add(player.position().x, player.position().y + (double) player.getEyeHeight(), player.position().z);
                        level.addParticle(gained > 0 ? new ItemParticleData(ParticleTypes.ITEM, stack) : ParticleTypes.SMOKE, v2.x, v2.y, v2.z, v1.x, v1.y + 0.05D, v1.z);
                    }
                }

                stack.shrink(1);
                return ActionResult.sidedSuccess(stack, level.isClientSide);
            } else {
                player.displayClientMessage(Lang.Items.KNOWLEDGE_SHARING_BOOK_NO_OWNER.translateColored(TextFormatting.RED), true);
                return ActionResult.fail(stack);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().hasUUID(NBTNames.OWNER);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        CompoundNBT nbt = stack.getOrCreateTag();
        if(nbt.hasUUID(NBTNames.OWNER)) {
            tooltip.add(Lang.Items.KNOWLEDGE_SHARING_BOOK_SELECTED.translateColored(TextFormatting.GRAY, new StringTextComponent(nbt.getString(NBTNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)));
        }
    }
}
