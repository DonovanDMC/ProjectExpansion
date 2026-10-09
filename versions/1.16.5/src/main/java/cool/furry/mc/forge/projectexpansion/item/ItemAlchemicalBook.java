package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketOpenAlchemicalBookGUI;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.OwnerData;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, list, tooltipFlag);
        list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP.translateColored(TextFormatting.GRAY));
        switch(tier) {
            case BASIC: list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BASIC.translateColored(TextFormatting.RED, CapabilityAlchemicalBookLocations.BASIC_DISTANCE_RATIO)); break;
            case ADVANCED: {
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ADVANCED.translateColored(TextFormatting.RED, CapabilityAlchemicalBookLocations.ADVANCED_DISTANCE_RATIO));
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(TextFormatting.GREEN));
                } break;
            case MASTER: {
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_MASTER.translateColored(TextFormatting.RED, CapabilityAlchemicalBookLocations.MASTER_DISTANCE_RATIO));
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(TextFormatting.GREEN));
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ACROSS_DIMENSIONS.translateColored(TextFormatting.GREEN));
                } break;
            case ARCANE: {
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ARCANE.translateColored(TextFormatting.RED, CapabilityAlchemicalBookLocations.ARCANE_DISTANCE_RATIO));
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_BIND.translateColored(TextFormatting.GREEN));
                    list.add(Lang.Items.ALCHEMICAL_BOOK_TOOLTIP_ACROSS_DIMENSIONS.translateColored(TextFormatting.GREEN));
                } break;
        }
        if(getMode(stack) == Mode.PLAYER) {
            PlayerEntity player = getPlayer(stack);
            list.add(Lang.Items.ALCHEMICAL_BOOK_BOUND_TO.translateColored(TextFormatting.RED, player == null ? new StringTextComponent(Util.getOwner(stack).name()).withStyle(TextFormatting.DARK_AQUA) : player.getDisplayName().copy().withStyle(TextFormatting.DARK_AQUA)));
        }
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    public @Nullable ServerPlayerEntity getPlayer(ItemStack stack) {
        if (stack.getItem() instanceof ItemAlchemicalBook && ((ItemAlchemicalBook) stack.getItem()).getMode(stack) == Mode.STACK) return null;
        OwnerData owner = Util.getOwner(stack);
        if (owner.isNone()) return null;
        return Util.getPlayer(owner.uuid());
    }

    public enum Mode {
        PLAYER,
        STACK;

    }
    public Mode getMode(ItemStack stack) {
        return OwnerData.has(stack) ? Mode.PLAYER : Mode.STACK;
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
                case BASIC: return CapabilityAlchemicalBookLocations.BASIC_DISTANCE_RATIO;
                case ADVANCED: return CapabilityAlchemicalBookLocations.ADVANCED_DISTANCE_RATIO;
                case MASTER: return CapabilityAlchemicalBookLocations.MASTER_DISTANCE_RATIO;
                case ARCANE: return CapabilityAlchemicalBookLocations.ARCANE_DISTANCE_RATIO;
                default: throw new IllegalStateException("Unexpected value: " + this);
            }
        }

        public Rarity getRarity() {
            switch(this) {
                case BASIC: return Rarity.COMMON;
                case ADVANCED: return Rarity.UNCOMMON;
                case MASTER: return Rarity.RARE;
                case ARCANE: return Rarity.EPIC;
                default: throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(!level.isClientSide) {
            if(player.isCrouching() && getTier().canBindToPlayer()) {
                OwnerData data = OwnerData.get(stack);
                if (getMode(stack) == Mode.PLAYER && data != null) {
                    if(!data.uuid().equals(player.getUUID())) {
                        player.displayClientMessage(Lang.NOT_OWNER.translateColored(TextFormatting.RED, new StringTextComponent(data.name()).withStyle(TextFormatting.DARK_AQUA)), false);
                        return ActionResult.fail(stack);
                    } else {
                        OwnerData.remove(stack);
                        player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_NO_LONGER_BOUND.translateColored(TextFormatting.GREEN, data.name()), false);
                    }
                } else {
                    data = new OwnerData(player.getUUID(), player.getName().getString());
                    data.set(stack);
                    player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_NOW_BOUND.translateColored(TextFormatting.GREEN, data.name()), false);
                }
            } else {
                try {
                    PacketHandler.sendToPlayer((ServerPlayerEntity) player, new PacketOpenAlchemicalBookGUI(hand, CapabilityAlchemicalBookLocations.from(stack).getLocations(), getMode(stack), canEdit(stack, (ServerPlayerEntity) player)));
                } catch (CapabilityAlchemicalBookLocations.BookError.OwnerOfflineError ignore) {
                    player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_OWNER_NOT_ONLINE.translateColored(TextFormatting.RED), false);
                    return ActionResult.fail(stack);
                }
            }
        }

        return ActionResult.success(stack);
    }

    public static boolean canEdit(ItemStack stack, ServerPlayerEntity player) {
        if(!(stack.getItem() instanceof ItemAlchemicalBook)) return false;
        ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
        @Nullable ServerPlayerEntity owner = book.getPlayer(stack);
        if(owner == null) return true;
        return canEdit(player, owner);
    }

    public static boolean canEdit(ServerPlayerEntity player, ServerPlayerEntity owner) {
        Config.AlchemicalBookEditLevel editLevel = Config.server.editOthersAlchemicalBooks.get();

        if(editLevel == Config.AlchemicalBookEditLevel.ENABLED) return true;
        if(player.hasPermissions(2) && editLevel == Config.AlchemicalBookEditLevel.OP_ONLY) return true;
        return owner.equals(player);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return OwnerData.has(stack);
    }
}
