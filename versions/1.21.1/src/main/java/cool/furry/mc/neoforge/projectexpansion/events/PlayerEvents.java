package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.neoforge.projectexpansion.registries.Attributes;
import cool.furry.mc.neoforge.projectexpansion.registries.Capabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Main.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        IAlchemicalBookLocationsProvider provider = event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY);
        if (provider != null) {
            provider.sync((ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        IAlchemicalBookLocationsProvider provider = event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY);
        if (provider != null) {
            provider.sync((ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
        IAlchemicalBookLocationsProvider provider = event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY);
        if (provider != null) {
            provider.sync((ServerPlayer) event.getEntity());
        }
    }

    @EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void entityAttributeModification(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, Attributes.SUN_EXPOSURE_PROTECTION);
        }
    }
}
