package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.ContainerEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.gui.container.*;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
public class MenuTypes {
    public static final DeferredRegister<ContainerType<?>> Registry = DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static final RegistryObject<ContainerType<ContainerCollector>> COLLECTOR_TIER_1 = registerBlockEntity("collector_tier_1", BlockEntityCollector.class, ContainerCollector.Tier1::new);
    public static final RegistryObject<ContainerType<ContainerCollector>> COLLECTOR_TIER_2 = registerBlockEntity("collector_tier_2", BlockEntityCollector.class, ContainerCollector.Tier2::new);
    public static final RegistryObject<ContainerType<ContainerCollector>> COLLECTOR_TIER_3 = registerBlockEntity("collector_tier_3", BlockEntityCollector.class, ContainerCollector.Tier3::new);
    public static final RegistryObject<ContainerType<ContainerCondenserMK3Input>> CONDENSER_MK3_INPUT = registerBlockEntity("condenser_mk3_input", BlockEntityCondenserMK3.class, ContainerCondenserMK3Input::new);
    public static final RegistryObject<ContainerType<ContainerCondenserMK3Output>> CONDENSER_MK3_OUTPUT = registerBlockEntity("condenser_mk3_output", BlockEntityCondenserMK3.class, ContainerCondenserMK3Output::new);
    public static final RegistryObject<ContainerType<ContainerArcaneTransmutationTablet>> ARCANE_TRANSMUTATION_TABLET = register("arcane_transmutation_tablet", ContainerArcaneTransmutationTablet::fromNetwork);
    public static final RegistryObject<ContainerType<ContainerEMCRefinery>> EMC_REFINERY = registerBlockEntity("emc_refinery", BlockEntityEMCRefinery.class, ContainerEMCRefinery::new);
    public static final RegistryObject<ContainerType<ContainerEmcPipe>> EMC_PIPE = register("emc_pipe", ContainerEmcPipe::fromNetwork);
    public static final RegistryObject<ContainerType<ContainerPowerFlower>> POWER_FLOWER = register("power_flower", ContainerPowerFlower::fromNetwork);

    public static <CONTAINER extends Container> RegistryObject<ContainerType<CONTAINER>> register(String name, IContainerFactory<CONTAINER> factory) {
        return Registry.register(name, () -> new ContainerType<>(factory));
    }

    public static <CONTAINER extends ContainerBase, BE extends TileEntity> RegistryObject<ContainerType<CONTAINER>> registerBlockEntity(String name, Class<BE> blockEntityClass, ContainerTypeDeferredRegister.ITileContainerFactory<CONTAINER, BE> factory) {
        return Registry.register(name, () -> IForgeContainerType.create((id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass))));
    }

    @OnlyIn(Dist.CLIENT)
    private static <BE extends TileEntity> BE getBlockEntityFromBuf(PacketBuffer buf, Class<BE> type) {
        BlockPos pos = buf.readBlockPos();
        BE blockEntity = WorldHelper.getTileEntity(type, Minecraft.getInstance().level, pos);
        if (blockEntity == null) {
            throw new IllegalStateException("Client could not locate block entity at " + pos + " for block entity container. "
                    + "This is likely caused by a mod breaking client side block entity lookup");
        }
        return blockEntity;
    }
}
