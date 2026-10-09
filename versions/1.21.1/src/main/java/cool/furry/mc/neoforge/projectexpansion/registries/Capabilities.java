package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.neoforge.projectexpansion.util.IEmcStorageBigInteger;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

public class Capabilities {
    public static final EntityCapability<IAlchemicalBookLocationsProvider, Void> ALCHEMICAL_BOOK_LOCATIONS_ENTITY = EntityCapability.createVoid(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
    public static final ItemCapability<IAlchemicalBookLocationsProvider, Void> ALCHEMICAL_BOOK_LOCATIONS_ITEM = ItemCapability.createVoid(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
    public static final BlockCapability<IEmcStorageBigInteger, @Nullable Direction> BIG_EMC_STORAGE_CAPABILITY = BlockCapability.createSided(Main.rl("alchemical_book_locations"), IEmcStorageBigInteger.class);
}
