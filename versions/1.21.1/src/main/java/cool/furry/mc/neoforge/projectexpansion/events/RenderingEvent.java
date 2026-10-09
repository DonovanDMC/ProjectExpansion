package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.neoforge.projectexpansion.util.IChestLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerChest(event, BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get());
        registerChest(event, BlockEntityTypes.CONDENSER_MK3.get());
    }

    private static <BE extends BlockEntity & IChestLike> void registerChest(EntityRenderersEvent.RegisterRenderers event, BlockEntityType<BE> type) {
        event.registerBlockEntityRenderer(type, context -> new ChestRenderer<>(context, type));
    }
}
