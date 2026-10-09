package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.forge.projectexpansion.util.IChestLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
