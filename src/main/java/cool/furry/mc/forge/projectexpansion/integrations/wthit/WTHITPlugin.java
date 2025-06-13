package cool.furry.mc.neoforge.projectexpansion.integrations.wthit;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import net.minecraft.world.level.block.Block;

@SuppressWarnings({"unused", "deprecation"})
public class WTHITPlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(WTHITDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
