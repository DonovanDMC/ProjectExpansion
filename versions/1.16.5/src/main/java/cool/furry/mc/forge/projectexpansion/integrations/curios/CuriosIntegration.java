package cool.furry.mc.forge.projectexpansion.integrations.curios;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

public class CuriosIntegration {
    public static final String MOD_ID = "curios";
    public static final String TABLET_SLOT_ID = "transmutation_tablet";
    public static final ResourceLocation TABLET_SLOT_ICON = Main.rl("curios/transmutation_tablet");

    public static boolean modLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static Optional<IItemHandlerModifiable> getCuriosInventory(PlayerEntity player) {
        if (modLoaded()) return Integrator.getCuriosInventory(player);
        return Optional.empty();
    }

    // slots aren't data driven in this version of curios
    public static void sendIMC() {
        if (modLoaded()) Integrator.sendIMC();
    }

    private static final class Integrator {
        private static void sendIMC() {
            InterModComms.sendTo(MOD_ID, SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder(TABLET_SLOT_ID).icon(TABLET_SLOT_ICON).build());
        }

        private static Optional<IItemHandlerModifiable> getCuriosInventory(PlayerEntity player) {
            Optional<ICuriosItemHandler> curiosItemHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
            if (!curiosItemHandler.isPresent()) return Optional.empty();
            Optional<ICurioStacksHandler> itemHandler = curiosItemHandler.get().getStacksHandler(TABLET_SLOT_ID);
            return itemHandler.map(ICurioStacksHandler::getStacks);
        }
    }
}
