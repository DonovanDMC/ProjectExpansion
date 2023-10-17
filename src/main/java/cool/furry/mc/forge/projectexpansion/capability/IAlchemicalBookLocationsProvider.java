package cool.furry.mc.forge.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface IAlchemicalBookLocationsProvider extends INBTSerializable<CompoundTag> {
    void addLocation(Player player, String name) throws CapabilityAlchemicalBookLocations.BookError.DuplicateNameError;
    void addLocation(String name, GlobalPos pos) throws CapabilityAlchemicalBookLocations.BookError.DuplicateNameError;
    void ensureEditable(ServerPlayer editor) throws CapabilityAlchemicalBookLocations.BookError.EditNotAllowedError;
    @Nullable CapabilityAlchemicalBookLocations.TeleportLocation getBackLocation();
    CapabilityAlchemicalBookLocations.TeleportLocation getBackLocationOrThrow() throws CapabilityAlchemicalBookLocations.BookError.NoBackLocationError;
    @Nullable ItemStack getItemStack();
    ItemStack getItemStackOrException();
    @Nullable CapabilityAlchemicalBookLocations.TeleportLocation getLocation(String name);
    CapabilityAlchemicalBookLocations.TeleportLocation getLocationOrThrow(String name) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError;
    ImmutableList<CapabilityAlchemicalBookLocations.TeleportLocation> getLocations();
    ItemAlchemicalBook.Mode getMode();
    @Nullable ServerPlayer getPlayer();
    ServerPlayer getPlayerOrException();
    void removeLocation(String name) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError;
    void resetLocations();
    void saveBackLocation(Player player);
    void saveBackLocation(Player player, GlobalPos pos);
    void sync(ServerPlayer player);
    void syncToOtherPlayers();
    void syncToPlayer(ServerPlayer target);
    void teleportBack(ServerPlayer player, boolean acrossDimensions) throws CapabilityAlchemicalBookLocations.BookError.NoBackLocationError, CapabilityAlchemicalBookLocations.BookError.WrongDimensionError, CapabilityAlchemicalBookLocations.BookError.DimensionNotFoundError;
    void teleportTo(String name, ServerPlayer player, boolean acrossDimensions) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError, CapabilityAlchemicalBookLocations.BookError.WrongDimensionError, CapabilityAlchemicalBookLocations.BookError.DimensionNotFoundError;
}