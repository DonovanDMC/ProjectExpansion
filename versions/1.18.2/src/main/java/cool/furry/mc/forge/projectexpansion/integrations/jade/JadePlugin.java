package cool.furry.mc.forge.projectexpansion.integrations.jade;

import cool.furry.mc.forge.projectexpansion.Main;
import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.world.level.block.Block;

// jade still used the waila api before 1.19
@WailaPlugin(Main.MOD_ID)
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerComponentProvider(JadeDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
