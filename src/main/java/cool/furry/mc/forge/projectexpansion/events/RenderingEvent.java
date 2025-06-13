package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.neoforge.projectexpansion.util.AdvancedAlchemicalChest;
import moze_intel.projecte.PECore;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for(DyeColor color: DyeColor.values()) {
            DeferredHolder<Block, BlockAdvancedAlchemicalChest> block = AdvancedAlchemicalChest.getRegistryBlock(color);
            event.registerBlockEntityRenderer(BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get(), context -> new ChestRenderer<>(context, Main.rl(String.format("textures/block/advanced_alchemical_chest/%s.png", color.getName())), block));
            event.registerBlockEntityRenderer(BlockEntityTypes.CONDENSER_MK3.get(), context -> new ChestRenderer<>(context, PECore.rl("textures/block/condenser_mk1.png"), block));
        }
    }
}
