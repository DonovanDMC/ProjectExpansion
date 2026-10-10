package cool.furry.mc.projectexpansion.integrations.jade;

import cool.furry.mc.projectexpansion.Main;
import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaCommonRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

// jade still used the waila api before 1.19
@WailaPlugin(Main.MOD_ID)
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(JadeDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerComponentProvider(JadeDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
