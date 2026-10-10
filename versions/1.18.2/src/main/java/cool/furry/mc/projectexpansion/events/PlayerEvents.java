package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.registries.Attributes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class PlayerEvents {
    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer && player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            CapabilityAlchemicalBookLocations.fromPlayer(serverPlayer).sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        // forge capabilities aren't copied on their own, unlike attachments
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(old ->
                event.getPlayer().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(storage -> storage.copyFrom(old))
        );
        original.invalidateCaps();
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getPlayer());
    }

    @Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void entityAttributeModification(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, Attributes.SUN_EXPOSURE_PROTECTION.get());
        }
    }
}
