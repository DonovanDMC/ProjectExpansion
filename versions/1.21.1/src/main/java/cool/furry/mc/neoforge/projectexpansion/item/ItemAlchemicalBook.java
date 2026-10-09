package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketOpenAlchemicalBookGUI;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAlchemicalBook extends Item {
    private final Tier tier;
    public ItemAlchemicalBook(Tier tier) {
        super(new Properties().rarity(tier.getRarity()).stacksTo(1).fireResistant());
        this.tier = tier;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP.translateColored(ChatFormatting.GRAY));
        switch(tier) {
            case BASIC -> list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BASIC.translateColored(ChatFormatting.RED, CapabilityAlchemicalBookLocations.BASIC_DISTANCE_RATIO));
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
            list.add(Lang.Items.ALCHEMICAL_BOOK_BOUND_TO.translateColored(ChatFormatting.RED, player == null ? Component.literal(Util.getOwner(stack).name()).withStyle(ChatFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(ChatFormatting.DARK_AQUA)));
        }
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    public @Nullable ServerPlayer getPlayer(ItemStack stack) {
        if (stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == Mode.STACK) return null;
        DataComponentTypes.OwnerData owner = Util.getOwner(stack);
        if (owner.isNone()) return null;
        return Util.getPlayer(owner.uuid());
    }

    public enum Mode {
        PLAYER,
        STACK;

        public static final StreamCodec<FriendlyByteBuf, Mode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Mode.class);
    }
    public Mode getMode(ItemStack stack) {
        return stack.has(DataComponentTypes.OWNER) ? Mode.PLAYER : Mode.STACK;
    }

    public enum Tier {
        BASIC,
        ADVANCED,
        MASTER,
        ARCANE;

        @SuppressWarnings("unused")
        public boolean isAcrossDimensions() {
            return this == MASTER || this == ARCANE;
        }

        public boolean canBindToPlayer() {
            return this == ADVANCED || this == MASTER || this == ARCANE;
        }

        @SuppressWarnings("unused")
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
                default -> throw new IllegalStateException("Unexpected value: " + this);
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
                default -> throw new IllegalStateException("Unexpected value: " + this);
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
                DataComponentTypes.OwnerData data = stack.get(DataComponentTypes.OWNER);
                if (getMode(stack) == Mode.PLAYER && data != null) {
                    if(!data.uuid().equals(player.getUUID())) {
                        player.sendSystemMessage(Lang.NOT_OWNER.translateColored(ChatFormatting.RED, Component.literal(data.name()).withStyle(ChatFormatting.DARK_AQUA)));
                        return InteractionResultHolder.fail(stack);
                    } else {
                        stack.remove(DataComponentTypes.OWNER);
                        player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_NO_LONGER_BOUND.translateColored(ChatFormatting.GREEN, data.name()));
                    }
                } else {
                    data = new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString());
                    stack.set(DataComponentTypes.OWNER, data);
                    player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_NOW_BOUND.translateColored(ChatFormatting.GREEN, data.name()));
                }
            } else {
                try {
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new PacketOpenAlchemicalBookGUI(hand, CapabilityAlchemicalBookLocations.from(stack).getLocations(), getMode(stack), canEdit(stack, (ServerPlayer) player)));
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
        Config.AlchemicalBookEditLevel editLevel = Config.server.editOthersAlchemicalBooks.get();

        if(editLevel == Config.AlchemicalBookEditLevel.ENABLED) return true;
        if(player.hasPermissions(Commands.LEVEL_GAMEMASTERS) && editLevel == Config.AlchemicalBookEditLevel.OP_ONLY) return true;
        return owner.equals(player);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponentTypes.OWNER);
    }
}
