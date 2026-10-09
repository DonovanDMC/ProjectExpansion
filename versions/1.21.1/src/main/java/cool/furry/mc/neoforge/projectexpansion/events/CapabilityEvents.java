package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.entity.*;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.neoforge.projectexpansion.registries.Capabilities;
import cool.furry.mc.neoforge.projectexpansion.registries.Items;
import cool.furry.mc.neoforge.projectexpansion.util.IHasCapability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY,
                EntityType.PLAYER,
                (player, dir) -> new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.PLAYER, (ServerPlayer) player, null)
        );
        event.registerItem(
                Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ITEM,
                (stack, dir) -> new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.STACK, null, stack),
                Items.BASIC_ALCHEMICAL_BOOK.get(),
                Items.ADVANCED_ALCHEMICAL_BOOK.get(),
                Items.MASTER_ALCHEMICAL_BOOK.get(),
                Items.ARCANE_ALCHEMICAL_BOOK.get()
        );

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IHasCapability icap) {
                icap.registerCapabilities(event);
            }
        }

        BlockEntityAdvancedAlchemicalChest.registerCapabilities(event);
        BlockEntityCollector.registerCapabilities(event);
        BlockEntityCondenserMK3.registerCapabilities(event);
        BlockEntityEMCLink.registerCapabilities(event);
        BlockEntityTransmutationInterface.registerCapabilities(event);
        BlockEntityRelay.registerCapabilities(event);
    }
}
