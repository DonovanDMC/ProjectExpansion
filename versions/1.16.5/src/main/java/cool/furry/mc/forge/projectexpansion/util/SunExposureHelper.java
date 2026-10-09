package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.registries.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.GameType;

@SuppressWarnings("unused")
public class SunExposureHelper {
    public static final ITag.INamedTag<Item> PROTECTIVE_ITEMS = ItemTags.createOptional(Main.rl("sun_exposure_protection"));

    private static boolean automaticProtection(ServerPlayerEntity player) {
        return player.gameMode.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR);
    }

    public static boolean wearingProtectiveBoots(ServerPlayerEntity player) {
        return automaticProtection(player) || player.inventory.getArmor(EquipmentSlotType.FEET.getIndex()).getItem().is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveLeggings(ServerPlayerEntity player) {
        return automaticProtection(player) || player.inventory.getArmor(EquipmentSlotType.LEGS.getIndex()).getItem().is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveChestplate(ServerPlayerEntity player) {
        return automaticProtection(player) || player.inventory.getArmor(EquipmentSlotType.CHEST.getIndex()).getItem().is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveHelmet(ServerPlayerEntity player) {
        return automaticProtection(player) || player.inventory.getArmor(EquipmentSlotType.HEAD.getIndex()).getItem().is(PROTECTIVE_ITEMS);
    }

    // percentage, 0-100
    public static int getProtectionAmount(ServerPlayerEntity player) {
        return (int) player.getAttributeValue(Attributes.SUN_EXPOSURE_PROTECTION.get()) * 100;
    }

    // level, 0-4
    public static int getProtectionLevel(ServerPlayerEntity player) {
        return (int) player.getAttributeValue(Attributes.SUN_EXPOSURE_PROTECTION.get()) * 4;
    }
}
