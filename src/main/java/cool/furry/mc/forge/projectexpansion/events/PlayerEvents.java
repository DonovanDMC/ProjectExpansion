package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.registries.Capabilities;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(old -> {
            CompoundTag tag = old.serializeNBT();
            event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(newCap -> {
                newCap.deserializeNBT(tag);
            });
        });
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void attachCapabilities(net.minecraftforge.event.AttachCapabilitiesEvent<?> event) {
        if(event.getObject() instanceof ItemStack stack && stack.getItem() instanceof ItemAlchemicalBook) {
            attachCapability(event, CapabilityAlchemicalBookLocations.Provider.NAME, new CapabilityAlchemicalBookLocations.Provider(ItemAlchemicalBook.Mode.STACK, null, stack));
        } else if(event.getObject() instanceof ServerPlayer player) {
            attachCapability(event, CapabilityAlchemicalBookLocations.Provider.NAME, new CapabilityAlchemicalBookLocations.Provider(ItemAlchemicalBook.Mode.PLAYER, player, null));
        }
    }

    private static void attachCapability(net.minecraftforge.event.AttachCapabilitiesEvent<?> event, ResourceLocation name, ICapabilityResolver<?> provider) {
        event.addCapability(name, provider);
        event.addListener(provider::invalidateAll);
    }
    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(cap -> cap.sync((ServerPlayer) event.getEntity()));
    }

    @SubscribeEvent
    public static void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(cap -> cap.sync((ServerPlayer) event.getEntity()));
    }
}
