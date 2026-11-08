package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.*;
import cool.furry.mc.projectexpansion.gui.container.ContainerEMCRefinery;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> Registry = DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_1 = registerBlockEntity("collector_tier_1", BlockEntityCollector.class, ContainerCollector.Tier1::new);
    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_2 = registerBlockEntity("collector_tier_2", BlockEntityCollector.class, ContainerCollector.Tier2::new);
    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_3 = registerBlockEntity("collector_tier_3", BlockEntityCollector.class, ContainerCollector.Tier3::new);
    public static final RegistryObject<MenuType<ContainerCondenserMK3Input>> CONDENSER_MK3_INPUT = Registry.register("condenser_mk3_input", () -> IForgeMenuType.create((id, inv, buf) -> new ContainerCondenserMK3Input(id, inv, getBlockEntityFromBuf(buf, BlockEntityCondenserMK3.class), buf.readEnum(Direction.class))));
    public static final RegistryObject<MenuType<ContainerCondenserMK3Output>> CONDENSER_MK3_OUTPUT = registerBlockEntity("condenser_mk3_output", BlockEntityCondenserMK3.class, ContainerCondenserMK3Output::new);
    public static final RegistryObject<MenuType<ContainerArcaneTransmutationTablet>> ARCANE_TRANSMUTATION_TABLET = register("arcane_transmutation_tablet", ContainerArcaneTransmutationTablet::fromNetwork);
    public static final RegistryObject<MenuType<ContainerEMCRefinery>> EMC_REFINERY = registerBlockEntity("emc_refinery", BlockEntityEMCRefinery.class, ContainerEMCRefinery::new);
    public static final RegistryObject<MenuType<ContainerEmcPipe>> EMC_PIPE = Registry.register("emc_pipe", () -> IForgeMenuType.create(ContainerEmcPipe::fromNetwork));
    public static final RegistryObject<MenuType<ContainerPowerFlower>> POWER_FLOWER = Registry.register("power_flower", () -> IForgeMenuType.create(ContainerPowerFlower::fromNetwork));

    public static <CONTAINER extends AbstractContainerMenu> RegistryObject<MenuType<CONTAINER>> register(String name, IContainerFactory<CONTAINER> factory) {
        return Registry.register(name, () -> new MenuType<>(factory));
    }

    public static <CONTAINER extends ContainerBase, BE extends BlockEntity> RegistryObject<MenuType<CONTAINER>> registerBlockEntity(String name, Class<BE> blockEntityClass, ContainerTypeDeferredRegister.IBlockEntityContainerFactory<CONTAINER, BE> factory) {
        return Registry.register(name, () -> IForgeMenuType.create((id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass))));
    }

    @OnlyIn(Dist.CLIENT)
    private static <BE extends BlockEntity> BE getBlockEntityFromBuf(FriendlyByteBuf buf, Class<BE> type) {
        BlockPos pos = buf.readBlockPos();
        BE blockEntity = WorldHelper.getBlockEntity(type, Minecraft.getInstance().level, pos);
        if (blockEntity == null) {
            throw new IllegalStateException("Client could not locate block entity at " + pos + " for block entity container. "
                    + "This is likely caused by a mod breaking client side block entity lookup");
        }
        return blockEntity;
    }
}
