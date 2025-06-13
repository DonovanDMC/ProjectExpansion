package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.registries.Attributes;
import cool.furry.mc.neoforge.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ItemEvents {
    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(SunExposureHelper.PROTECTIVE_ITEMS) && stack.getItem() instanceof ArmorItem armor) {
            EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
            event.addModifier(Attributes.SUN_EXPOSURE_PROTECTION, new AttributeModifier(Util.SUN_EXPOSURE_PROTECTION.apply(slot.getSerializedName()), 0.25D, AttributeModifier.Operation.ADD_VALUE), slot);
        }
    }
}
