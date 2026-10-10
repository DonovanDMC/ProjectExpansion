package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.registries.Attributes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class PlayerEvents {
    private static void sync(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity && player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
            CapabilityAlchemicalBookLocations.fromPlayer(serverPlayer).sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        // forge capabilities aren't copied on their own, unlike attachments
        PlayerEntity original = event.getOriginal();
        original.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(old ->
                event.getPlayer().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(storage -> storage.copyFrom(old))
        );
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
