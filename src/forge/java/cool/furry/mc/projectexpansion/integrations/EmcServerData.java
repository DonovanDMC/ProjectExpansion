package cool.furry.mc.projectexpansion.integrations;

import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

// the overlays' server side, kept apart from Common since that touches client classes
public class EmcServerData {
    private static final String STORED_KEY = "projectexpansion:emc_stored";
    private static final String MAXIMUM_KEY = "projectexpansion:emc_maximum";

    // many blocks (like projecte's condensers) never send their stored emc to the client
    public static void append(CompoundTag data, Level level, BlockPos pos, BlockEntity blockEntity, @Nullable Direction side) {
        // a side can have nothing on it, like an emc storage's off sides, the block as a whole is used then
        if (!appendSide(data, blockEntity, side) && side != null) appendSide(data, blockEntity, null);
    }

    private static boolean appendSide(CompoundTag data, BlockEntity blockEntity, @Nullable Direction side) {
        @Nullable IEmcStorageBigInteger bigEmcStorage = blockEntity.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).orElse(null);
        if (bigEmcStorage != null) {
            data.putString(STORED_KEY, bigEmcStorage.getStoredEmcBigInteger().toString());
            data.putString(MAXIMUM_KEY, bigEmcStorage.getMaximumEmcBigInteger().toString());
            return true;
        }
        @Nullable IEmcStorage emcStorage = blockEntity.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, side).orElse(null);
        if (emcStorage != null) {
            data.putString(STORED_KEY, Long.toString(emcStorage.getStoredEmc()));
            data.putString(MAXIMUM_KEY, Long.toString(emcStorage.getMaximumEmc()));
            return true;
        }
        return false;
    }

    public static boolean has(@Nullable CompoundTag data) {
        return data != null && data.contains(STORED_KEY) && data.contains(MAXIMUM_KEY);
    }

    public static BigInteger stored(CompoundTag data) {
        return new BigInteger(data.getString(STORED_KEY));
    }

    public static BigInteger maximum(CompoundTag data) {
        return new BigInteger(data.getString(MAXIMUM_KEY));
    }
}
