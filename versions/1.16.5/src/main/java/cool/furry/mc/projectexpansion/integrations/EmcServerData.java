package cool.furry.mc.projectexpansion.integrations;

import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

// the overlays' server side, kept apart from Common since that touches client classes
public class EmcServerData {
    private static final String STORED_KEY = "projectexpansion:emc_stored";
    private static final String MAXIMUM_KEY = "projectexpansion:emc_maximum";

    // many blocks (like projecte's condensers) never send their stored emc to the client
    public static void append(CompoundNBT data, World level, BlockPos pos, TileEntity blockEntity, @Nullable Direction side) {
        // a side can have nothing on it, like an emc storage's off sides, the block as a whole is used then
        if (!appendSide(data, blockEntity, side) && side != null) appendSide(data, blockEntity, null);
    }

    private static boolean appendSide(CompoundNBT data, TileEntity blockEntity, @Nullable Direction side) {
        @Nullable IEmcStorageBigInteger bigEmcStorage = blockEntity.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).orElse(null);
        if (bigEmcStorage != null) {
            data.putString(STORED_KEY, bigEmcStorage.getStoredEmcBigInteger().toString());
            data.putString(MAXIMUM_KEY, bigEmcStorage.getMaximumEmcBigInteger().toString());
            return true;
        }
        @Nullable IEmcStorage emcStorage = blockEntity.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, side).orElse(null);
        if (emcStorage != null) {
            data.putString(STORED_KEY, Long.toString(emcStorage.getStoredEmc()));
            data.putString(MAXIMUM_KEY, Long.toString(emcStorage.getMaximumEmc()));
            return true;
        }
        return false;
    }

    public static boolean has(@Nullable CompoundNBT data) {
        return data != null && data.contains(STORED_KEY) && data.contains(MAXIMUM_KEY);
    }

    public static BigInteger stored(CompoundNBT data) {
        return new BigInteger(data.getString(STORED_KEY));
    }

    public static BigInteger maximum(CompoundNBT data) {
        return new BigInteger(data.getString(MAXIMUM_KEY));
    }
}
