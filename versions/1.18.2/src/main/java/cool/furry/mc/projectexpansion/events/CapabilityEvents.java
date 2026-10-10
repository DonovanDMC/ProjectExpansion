package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class CapabilityEvents {
    @Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(CapabilityAlchemicalBookLocations.Storage.class);
            event.register(IEmcStorageBigInteger.class);
        }
    }

    @Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        @SubscribeEvent
        public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                attachBookLocations(event);
            }
        }

        @SubscribeEvent
        public static void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
            if (event.getObject().getItem() instanceof ItemAlchemicalBook) {
                attachBookLocations(event);
            }
        }

        private static void attachBookLocations(AttachCapabilitiesEvent<?> event) {
            CapabilityAlchemicalBookLocations.Storage storage = new CapabilityAlchemicalBookLocations.Storage();
            event.addCapability(CapabilityAlchemicalBookLocations.Storage.NAME, storage);
            event.addListener(storage::invalidate);
        }
    }
}
