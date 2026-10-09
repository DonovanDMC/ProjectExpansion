package cool.furry.mc.forge.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.GlobalPos;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IAlchemicalBookLocationsProvider {
    void addLocation(PlayerEntity player, String name) throws CapabilityAlchemicalBookLocations.BookError.DuplicateNameError;
    void addLocation(String name, GlobalPos pos) throws CapabilityAlchemicalBookLocations.BookError.DuplicateNameError;
    void ensureEditable(ServerPlayerEntity editor) throws CapabilityAlchemicalBookLocations.BookError.EditNotAllowedError;
    @Nullable CapabilityAlchemicalBookLocations.TeleportLocation getBackLocation();
    CapabilityAlchemicalBookLocations.TeleportLocation getBackLocationOrThrow() throws CapabilityAlchemicalBookLocations.BookError.NoBackLocationError;
    @Nullable ItemStack itemStack();
    ItemStack getItemStackOrException();
    @Nullable CapabilityAlchemicalBookLocations.TeleportLocation getLocation(String name);
    CapabilityAlchemicalBookLocations.TeleportLocation getLocationOrThrow(String name) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError;
    ImmutableList<CapabilityAlchemicalBookLocations.TeleportLocation> getLocations();
    ItemAlchemicalBook.Mode mode();
    @Nullable ServerPlayerEntity player();
    ServerPlayerEntity getPlayerOrException();
    void reindex();
    void removeLocation(String name) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError;
    void resetLocations();
    void saveBackLocation(PlayerEntity player);
    void saveBackLocation(PlayerEntity player, GlobalPos pos);
    void sync(ServerPlayerEntity player);
    void syncToOtherPlayers();
    void syncToPlayer(ServerPlayerEntity target);
    void teleportBack(ServerPlayerEntity player, boolean acrossDimensions) throws CapabilityAlchemicalBookLocations.BookError.NoBackLocationError, CapabilityAlchemicalBookLocations.BookError.WrongDimensionError, CapabilityAlchemicalBookLocations.BookError.DimensionNotFoundError;
    void teleportTo(String name, ServerPlayerEntity player, boolean acrossDimensions) throws CapabilityAlchemicalBookLocations.BookError.NameNotFoundError, CapabilityAlchemicalBookLocations.BookError.WrongDimensionError, CapabilityAlchemicalBookLocations.BookError.DimensionNotFoundError;
}