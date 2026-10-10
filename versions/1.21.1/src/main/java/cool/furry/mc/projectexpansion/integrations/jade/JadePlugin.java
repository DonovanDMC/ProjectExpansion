package cool.furry.mc.projectexpansion.integrations.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(JadeDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerBlockComponent(JadeDataProvider.INSTANCE, Block.class);
    }
}
