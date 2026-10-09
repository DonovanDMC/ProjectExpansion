package cool.furry.mc.forge.projectexpansion.integrations.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerBlockComponent(JadeDataProvider.INSTANCE, Block.class);
    }
}
