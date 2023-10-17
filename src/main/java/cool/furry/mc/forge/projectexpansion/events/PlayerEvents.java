package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.registries.Capabilities;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(old -> {
            CompoundNBT tag = old.serializeNBT();
            event.getPlayer().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(newCap -> {
                newCap.deserializeNBT(tag);
            });
        });
    }

    @SubscribeEvent
    public static void attachCapabilities(net.minecraftforge.event.AttachCapabilitiesEvent<?> event) {
        if(event.getObject() instanceof ItemStack && ((ItemStack) event.getObject()).getItem() instanceof ItemAlchemicalBook) {
            attachCapability(event, CapabilityAlchemicalBookLocations.Provider.NAME, new CapabilityAlchemicalBookLocations.Provider(ItemAlchemicalBook.Mode.STACK, null, ((ItemStack) event.getObject())));
        } else if(event.getObject() instanceof ServerPlayerEntity) {
            attachCapability(event, CapabilityAlchemicalBookLocations.Provider.NAME, new CapabilityAlchemicalBookLocations.Provider(ItemAlchemicalBook.Mode.PLAYER, ((ServerPlayerEntity) event.getObject()), null));
        }
    }

    private static void attachCapability(net.minecraftforge.event.AttachCapabilitiesEvent<?> event, ResourceLocation name, ICapabilityResolver<?> provider) {
        event.addCapability(name, provider);
        event.addListener(provider::invalidateAll);
    }

    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getPlayer().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(cap -> cap.sync((ServerPlayerEntity) event.getPlayer()));
    }

    @SubscribeEvent
    public static void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
        event.getPlayer().getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).ifPresent(cap -> cap.sync((ServerPlayerEntity) event.getPlayer()));
    }
}
