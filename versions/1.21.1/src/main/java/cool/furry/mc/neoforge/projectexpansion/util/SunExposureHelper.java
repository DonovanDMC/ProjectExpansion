package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.registries.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;

@SuppressWarnings("unused")
public class SunExposureHelper {
    public static final TagKey<Item> PROTECTIVE_ITEMS = ItemTags.create(Main.rl("sun_exposure_protection"));

    private static boolean automaticProtection(ServerPlayer player) {
        return player.gameMode.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR);
    }

    public static boolean wearingProtectiveBoots(ServerPlayer player) {
        return automaticProtection(player) || player.getInventory().getArmor(EquipmentSlot.FEET.getIndex()).is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveLeggings(ServerPlayer player) {
        return automaticProtection(player) || player.getInventory().getArmor(EquipmentSlot.LEGS.getIndex()).is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveChestplate(ServerPlayer player) {
        return automaticProtection(player) || player.getInventory().getArmor(EquipmentSlot.CHEST.getIndex()).is(PROTECTIVE_ITEMS);
    }

    public static boolean wearingProtectiveHelmet(ServerPlayer player) {
        return automaticProtection(player) || player.getInventory().getArmor(EquipmentSlot.HEAD.getIndex()).is(PROTECTIVE_ITEMS);
    }

    // percentage, 0-100
    public static int getProtectionAmount(ServerPlayer player) {
        return (int) player.getAttributeValue(Attributes.SUN_EXPOSURE_PROTECTION) * 100;
    }

    // level, 0-4
    public static int getProtectionLevel(ServerPlayer player) {
        return (int) player.getAttributeValue(Attributes.SUN_EXPOSURE_PROTECTION) * 4;
    }
}
