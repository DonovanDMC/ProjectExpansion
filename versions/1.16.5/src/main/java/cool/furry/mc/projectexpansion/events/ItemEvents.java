package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.registries.Attributes;
import cool.furry.mc.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.projectexpansion.util.Util;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
        if (stack.getItem().is(SunExposureHelper.PROTECTIVE_ITEMS) && stack.getItem() instanceof ArmorItem && event.getSlotType() == ((ArmorItem) stack.getItem()).getSlot()) {
            ArmorItem armor = (ArmorItem) stack.getItem();
            ResourceLocation id = Util.SUN_EXPOSURE_PROTECTION.apply(armor.getSlot().getName());
            // modifiers need a stable uuid per slot before 1.21
            UUID uuid = UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
            event.addModifier(Attributes.SUN_EXPOSURE_PROTECTION.get(), new AttributeModifier(uuid, id.toString(), 0.25D, AttributeModifier.Operation.ADDITION));
        }
    }
}
