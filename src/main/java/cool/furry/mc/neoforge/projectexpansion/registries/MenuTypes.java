package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCollector;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Input;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerCondenserMK3Output;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> Registry = DeferredRegister.create(Registries.MENU, Main.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerCollector>> COLLECTOR_TIER_1 = register("collector_tier_1", BlockEntityCollector.class, ContainerCollector.Tier1::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerCollector>> COLLECTOR_TIER_2 = register("collector_tier_2", BlockEntityCollector.class, ContainerCollector.Tier2::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerCollector>> COLLECTOR_TIER_3 = register("collector_tier_3", BlockEntityCollector.class, ContainerCollector.Tier3::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerCondenserMK3Input>> CONDENSER_MK3_INPUT = register("condenser_mk3_input", BlockEntityCondenserMK3.class, ContainerCondenserMK3Input::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerCondenserMK3Output>> CONDENSER_MK3_OUTPUT = register("condenser_mk3_output", BlockEntityCondenserMK3.class, ContainerCondenserMK3Output::new);

    public static <CONTAINER extends AbstractContainerMenu, BE extends BlockEntity> DeferredHolder<MenuType<?>, MenuType<CONTAINER>> register(String name,
                                                                                                                          Class<BE> blockEntityClass, ContainerTypeDeferredRegister.IBlockEntityContainerFactory<CONTAINER, BE> factory) {
        return Registry.register(name, () -> IMenuTypeExtension.create((id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass))));
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
