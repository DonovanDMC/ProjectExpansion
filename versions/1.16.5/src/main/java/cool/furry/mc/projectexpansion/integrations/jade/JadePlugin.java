package cool.furry.mc.projectexpansion.integrations.jade;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

// jade uses hwyla's api here, so this works with either of them
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        registrar.registerComponentProvider(JadeDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
        registrar.registerBlockDataProvider(JadeDataProvider.INSTANCE, TileEntity.class);
    }
}
