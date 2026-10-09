package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.registries.Attributes;
import cool.furry.mc.forge.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemEvents {
    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(SunExposureHelper.PROTECTIVE_ITEMS) && stack.getItem() instanceof ArmorItem armor && event.getSlotType() == armor.getEquipmentSlot()) {
            ResourceLocation id = Util.SUN_EXPOSURE_PROTECTION.apply(armor.getEquipmentSlot().getName());
            // modifiers need a stable uuid per slot before 1.21
            UUID uuid = UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
            event.addModifier(Attributes.SUN_EXPOSURE_PROTECTION.get(), new AttributeModifier(uuid, id.toString(), 0.25D, AttributeModifier.Operation.ADDITION));
        }
    }
}
