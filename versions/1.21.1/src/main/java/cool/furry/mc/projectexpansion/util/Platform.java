package cool.furry.mc.projectexpansion.util;

import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

// what the shared code needs that differs between versions, every version has its own copy
public class Platform {
    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static MutableComponent empty() {
        return Component.empty();
    }

    public static Level level(Entity entity) {
        return entity.level();
    }

    public static IEMCProxy emcProxy() {
        return IEMCProxy.INSTANCE;
    }

    // feedback that's always shown to whoever ran the command
    public static void sendMessage(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
    }

    public static void sendSuccess(CommandSourceStack source, Supplier<Component> message, boolean notify) {
        source.sendSuccess(message, notify);
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        return stack.copyWithCount(count);
    }

    // stacks hold components instead of nbt from 1.20.5
    public static boolean isSameItemSameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
