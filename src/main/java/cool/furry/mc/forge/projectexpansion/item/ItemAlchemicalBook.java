package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketOpenAlchemicalBookGUI;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAlchemicalBook extends Item {
    private final Tier tier;
    public ItemAlchemicalBook(Tier tier) {
        super(new Properties().tab(Main.tab).rarity(tier.getRarity()).stacksTo(1).fireResistant());
        this.tier = tier;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP.translateColored(ChatFormatting.GRAY));
        switch(tier) {
            case BASIC -> {
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BASIC.translateColored(ChatFormatting.RED, CapabilityAlchemicalBookLocations.BASIC_DISTANCE_RATIO));
            }
            case ADVANCED -> {
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ADVANCED.translateColored(ChatFormatting.RED, CapabilityAlchemicalBookLocations.ADVANCED_DISTANCE_RATIO));
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(ChatFormatting.GREEN));
            }
            case MASTER -> {
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_MASTER.translateColored(ChatFormatting.RED, CapabilityAlchemicalBookLocations.MASTER_DISTANCE_RATIO));
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(ChatFormatting.GREEN));
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ACROSS_DIMENSIONS.translateColored(ChatFormatting.GREEN));
            }
            case ARCANE -> {
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ARCANE.translateColored(ChatFormatting.RED, CapabilityAlchemicalBookLocations.ARCANE_DISTANCE_RATIO));
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(ChatFormatting.GREEN));
                list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ACROSS_DIMENSIONS.translateColored(ChatFormatting.GREEN));
            }
        }
        if(getMode(stack) == Mode.PLAYER) {
            Player player = getPlayer(stack);
            list.add(Lang.Items.ALCHEMICAL_BOOK_BOUND_TO.translateColored(ChatFormatting.RED, player == null ? Component.literal(stack.getOrCreateTag().getString(TagNames.OWNER_NAME)).withStyle(ChatFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
        }
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    public @Nullable ServerPlayer getPlayer(ItemStack stack) {
        if(stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == Mode.STACK) return null;
        return Util.getPlayer(stack.getOrCreateTag().getUUID(TagNames.OWNER));
    }

    public enum Mode {
        PLAYER,
        STACK
    }
    public Mode getMode(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().contains(TagNames.OWNER) ? Mode.PLAYER : Mode.STACK;
    }

    public enum Tier {
        BASIC,
        ADVANCED,
        MASTER,
        ARCANE;

        public boolean isAcrossDimensions() {
            return this == MASTER || this == ARCANE;
        }

        public boolean canBindToPlayer() {
            return this == ADVANCED || this == MASTER || this == ARCANE;
        }

        public float distanceRatio() {
            switch(this) {
                case BASIC -> {
                    return CapabilityAlchemicalBookLocations.BASIC_DISTANCE_RATIO;
                }
                case ADVANCED -> {
                    return CapabilityAlchemicalBookLocations.ADVANCED_DISTANCE_RATIO;
                }
                case MASTER -> {
                    return CapabilityAlchemicalBookLocations.MASTER_DISTANCE_RATIO;
                }
                case ARCANE -> {
                    return CapabilityAlchemicalBookLocations.ARCANE_DISTANCE_RATIO;
                }
                default -> {
                    throw new IllegalStateException("Unexpected value: " + this);
                }
            }
        }

        public Rarity getRarity() {
            switch(this) {
                case BASIC -> {
                    return Rarity.COMMON;
                }
                case ADVANCED -> {
                    return Rarity.UNCOMMON;
                }
                case MASTER -> {
                    return Rarity.RARE;
                }
                case ARCANE -> {
                    return Rarity.EPIC;
                }
                default -> {
                    throw new IllegalStateException("Unexpected value: " + this);
                }
            }
        }
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(!level.isClientSide) {
            if(player.isCrouching() && getTier().canBindToPlayer()) {
                CompoundTag tag = stack.getOrCreateTag();
                if(getMode(stack) == Mode.PLAYER) {
                    if(!tag.getUUID(TagNames.OWNER).equals(player.getUUID())) {
                        player.sendSystemMessage(Lang.NOT_OWNER.translateColored(ChatFormatting.RED, Component.literal(tag.getString(TagNames.OWNER_NAME)).withStyle(ChatFormatting.DARK_AQUA)));
                        return InteractionResultHolder.fail(stack);
                    } else {
                        tag.remove(TagNames.OWNER);
                        tag.remove(TagNames.OWNER_NAME);
                        player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_NO_LONGER_BOUND.translateColored(ChatFormatting.GREEN, tag.getString(TagNames.OWNER_NAME)));
                    }
                } else {
                    tag.putUUID(TagNames.OWNER, player.getUUID());
                    tag.putString(TagNames.OWNER_NAME, player.getName().getString());
                    player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_NOW_BOUND.translateColored(ChatFormatting.GREEN, tag.getString(TagNames.OWNER_NAME)));
                }
            } else {
                try {
                    PacketHandler.sendTo(new PacketOpenAlchemicalBookGUI(hand, CapabilityAlchemicalBookLocations.from(stack).getLocations(), getMode(stack), canEdit(stack, (ServerPlayer) player)), (ServerPlayer) player);
                } catch (CapabilityAlchemicalBookLocations.BookError.OwnerOfflineError ignore) {
                    player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_OWNER_NOT_ONLINE.translateColored(ChatFormatting.RED));
                    return InteractionResultHolder.fail(stack);
                }
            }
        }

        return InteractionResultHolder.success(stack);
    }

    public static boolean canEdit(ItemStack stack, ServerPlayer player) {
        if(!(stack.getItem() instanceof ItemAlchemicalBook book)) return false;
        @Nullable ServerPlayer owner = book.getPlayer(stack);
        if(owner == null) return true;
        return canEdit(player, owner);
    }

    public static boolean canEdit(ServerPlayer player, ServerPlayer owner) {
        Config.AlchemicalBookEditLevel editLevel = Config.editOthersAlchemicalBooks();

        if(editLevel == Config.AlchemicalBookEditLevel.ENABLED) return true;
        if(player.hasPermissions(Commands.LEVEL_GAMEMASTERS) && editLevel == Config.AlchemicalBookEditLevel.OP_ONLY) return true;
        return owner.equals(player);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().contains(TagNames.OWNER);
    }
}
