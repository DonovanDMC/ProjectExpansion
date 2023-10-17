package cool.furry.mc.forge.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.registries.Capabilities;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;

public class CapabilityAlchemicalBookLocations implements IAlchemicalBookLocationsProvider {
    public static void init() {
        CapabilityManager.INSTANCE.register(IAlchemicalBookLocationsProvider.class, new Capability.IStorage<IAlchemicalBookLocationsProvider>() {
            @Override
            public INBT writeNBT(Capability<IAlchemicalBookLocationsProvider> capability, IAlchemicalBookLocationsProvider instance, Direction side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<IAlchemicalBookLocationsProvider> capability, IAlchemicalBookLocationsProvider instance, Direction side, INBT nbt) {
                if(nbt instanceof CompoundNBT) {
                    instance.deserializeNBT((CompoundNBT) nbt);
                }
            }
        }, () -> getDefault(ItemAlchemicalBook.Mode.PLAYER, null, null));
    }
    public static final int BASIC_DISTANCE_RATIO = 1000;
    public static final int ADVANCED_DISTANCE_RATIO = 500;
    public static final int MASTER_DISTANCE_RATIO = 100;
    public static final int ARCANE_DISTANCE_RATIO = 0;
    private final ItemAlchemicalBook.Mode mode;
    private final @Nullable ServerPlayerEntity player;
    private final @Nullable ItemStack itemStack;
    private CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayerEntity player, @Nullable ItemStack itemStack) {
        this.mode = mode;
        this.player = player;
        this.itemStack = itemStack;
    }
    public static IAlchemicalBookLocationsProvider fromPlayer(PlayerEntity player) {
        return player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).orElseThrow(() -> new IllegalStateException("Player does not have expected capability"));
    }
    public static IAlchemicalBookLocationsProvider fromItemStack(ItemStack stack) {
        return stack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).orElseThrow(() -> new IllegalStateException("ItemStack does not have expected capability"));
    }

    public static IAlchemicalBookLocationsProvider from(ItemStack stack) throws BookError.OwnerOfflineError {
        if(!(stack.getItem() instanceof ItemAlchemicalBook)) {
            throw new IllegalArgumentException("ItemStack is not an alchemical book");
        }

        ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();

        if(book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            ServerPlayerEntity owner = book.getPlayer(stack);
            if(owner == null) {
                throw new BookError.OwnerOfflineError(stack.getOrCreateTag().getString(NBTNames.OWNER_NAME));
            }
            return fromPlayer(owner);
        } else {
            return fromItemStack(stack);
        }
    }

    public static boolean isForbiddenName(String name) {
        return name.equalsIgnoreCase(BACK_KEY);
    }

    public static IAlchemicalBookLocationsProvider getDefault(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayerEntity player, @Nullable ItemStack itemStack) {
        return new CapabilityAlchemicalBookLocations(mode, player, itemStack);
    }
    public static class Provider implements ICapabilityResolver<IAlchemicalBookLocationsProvider>, ICapabilitySerializable<CompoundNBT> {
        private @Nullable LazyOptional<IAlchemicalBookLocationsProvider> cached;
        public static final ResourceLocation NAME = Main.rl("alchemical_book_locations");
        private final IAlchemicalBookLocationsProvider defaultImpl;
        private final @Nullable ServerPlayerEntity player;
        private final @Nullable ItemStack itemStack;
        public Provider(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayerEntity player, @Nullable ItemStack itemStack) {
            this.defaultImpl = getDefault(mode, player, itemStack);
            this.player = player;
            this.itemStack = itemStack;
        }

        @Override
        public @Nonnull Capability<IAlchemicalBookLocationsProvider> getMatchingCapability() {
            return Capabilities.ALCHEMICAL_BOOK_LOCATIONS;
        }

        @Override
        public @Nonnull <T> LazyOptional<T> getCapabilityUnchecked(@Nonnull Capability<T> capability, @Nullable Direction direction) {
            if(cached == null || !cached.isPresent()) {
                cached = LazyOptional.of(() -> defaultImpl).cast();
            }
            return cached.cast();
        }

        @Override
        public void invalidate(@Nonnull Capability<?> capability, @Nullable Direction side) {
            invalidateAll();
        }

        @Override
        public void invalidateAll() {
            if (cached != null && cached.isPresent()) {
                cached.invalidate();
                cached = null;
            }
        }

        @Override
        public CompoundNBT serializeNBT() {
            return defaultImpl.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            defaultImpl.deserializeNBT(nbt);
        }
    }

    public static final class TeleportLocation {
        private final String name;
        private final int x;
        private final int y;
        private final int z;
        private final RegistryKey<World> dimension;
        private final int index;

        public TeleportLocation(String name, int x, int y, int z, RegistryKey<World> dimension, int index) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.index = index;
        }

        public void teleportTo(ServerPlayerEntity player, boolean acrossDimensions) throws BookError.DimensionNotFoundError, BookError.WrongDimensionError {
            RegistryKey<World> dim = player.getLevel().dimension();

            ServerWorld level = Util.getDimension(dimension);
            if (level == null) {
                throw new BookError.DimensionNotFoundError(dimension);
            }

            if (!dim.equals(dimension) && !acrossDimensions) {
                throw new BookError.WrongDimensionError();
            }

            player.teleportTo(level, x, y, z, player.yRot, player.xRot);
        }

        public double distanceFrom(BlockPos pos) {
            return Math.sqrt(Math.pow(pos.getX() - x, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() - z, 2));
        }

        public int getCost(ItemStack stack, PlayerEntity player) {
            if (player.isCreative()) {
                return 0;
            }

            return getCost(stack, player.getOnPos());
        }

        public int getCost(ItemStack stack, BlockPos pos) {
            if (!(stack.getItem() instanceof ItemAlchemicalBook)) {
                return (int) Math.ceil(distanceFrom(pos) * BASIC_DISTANCE_RATIO);
            }
            return (int) Math.ceil(distanceFrom(pos) * ((ItemAlchemicalBook) stack.getItem()).getTier().distanceRatio());
        }

        // records are immutable
        public TeleportLocation withIndex(int index) {
            return from(name, new BlockPos(x, y, z), dimension, index);
        }

        public boolean isBack() {
            return name.equals(BACK_KEY);
        }

        public CompoundNBT serialize() {
            CompoundNBT tag = new CompoundNBT();
            tag.putString(NBTNames.NAME, name);
            tag.putInt(NBTNames.X, x);
            tag.putInt(NBTNames.Y, y);
            tag.putInt(NBTNames.Z, z);
            tag.putInt(NBTNames.INDEX, index);
            tag.putString(NBTNames.DIMENSION, dimension.location().toString());
            return tag;
        }

        public static TeleportLocation deserialize(CompoundNBT tag) {
            String name = tag.getString(NBTNames.NAME);
            int x = tag.getInt(NBTNames.X);
            int y = tag.getInt(NBTNames.Y);
            int z = tag.getInt(NBTNames.Z);
            int index = tag.getInt(NBTNames.INDEX);
            RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString(NBTNames.DIMENSION)));
            return new TeleportLocation(name, x, y, z, dimension, index);
        }

        public static TeleportLocation from(String name, GlobalPos pos, int index) {
            return from(name, pos.pos(), pos.dimension(), index);
        }

        public static TeleportLocation from(String name, BlockPos pos, RegistryKey<World> dimension, int index) {
            return new TeleportLocation(name, pos.getX(), pos.getY(), pos.getZ(), dimension, index);
        }

        public String name() {
            return name;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        public RegistryKey<World> dimension() {
            return dimension;
        }

        public int index() {
            return index;
        }
    }

    public static class BookError extends Exception {
        public static class WrongDimensionError extends BookError {
            public WrongDimensionError() {
                super(Type.WRONG_DIMENSION);
            }
        }
        public static class DimensionNotFoundError extends BookError {
            private final ITextComponent name;
            public DimensionNotFoundError(ITextComponent name) {
                super(Type.DIMENSION_NOT_FOUND);
                this.name = name;
            }
            public DimensionNotFoundError(RegistryKey<World> level) {
                this(new TranslationTextComponent(level.location().toString().replace(":", ".")));
            }
            public DimensionNotFoundError(String name) {
                this(new StringTextComponent(name));
            }

            @Override
            public IFormattableTextComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED, name);
            }
        }
        public static class NoBackLocationError extends BookError {
            public NoBackLocationError() {
                super(Type.NO_BACK_LOCATION);
            }
        }
        public static class DuplicateNameError extends BookError {
            private final ITextComponent name;
            public DuplicateNameError(ITextComponent name) {
                super(Type.DUPLICATE_NAME);
                this.name = name;
            }
            public DuplicateNameError(String name) {
                this(new StringTextComponent(name));
            }

            @Override
            public IFormattableTextComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED, name);
            }
        }
        public static class NameNotFoundError extends BookError {
            private final ITextComponent name;
            public NameNotFoundError(ITextComponent name) {
                super(Type.NAME_NOT_FOUND);
                this.name = name;
            }
            public NameNotFoundError(String name) {
                this(new StringTextComponent(name));
            }

            @Override
            public IFormattableTextComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED, name);
            }
        }
        public static class OwnerOfflineError extends BookError {
            private final ITextComponent player;
            public OwnerOfflineError(ITextComponent name) {
                super(Type.OWNER_OFFLINE);
                this.player = name;
            }
            public OwnerOfflineError(String name) {
                this(new StringTextComponent(name));
            }

            @Override
            public IFormattableTextComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED, player);
            }
        }
        public static class NotEnoughEMCError extends BookError {
            private final String emc;
            public NotEnoughEMCError(String emc) {
                super(Type.NOT_ENOUGH_EMC);
                this.emc = emc;
            }

            @Override
            public IFormattableTextComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED, emc);
            }
        }
        public static class EditNotAllowedError extends BookError {
            public EditNotAllowedError() {
                super(Type.EDIT_NOT_ALLOWED);
            }
        }
        public enum Type {
            // Teleportation
            WRONG_DIMENSION,
            DIMENSION_NOT_FOUND,
            NO_BACK_LOCATION,
            NOT_ENOUGH_EMC,
            // Destinations
            DUPLICATE_NAME,
            NAME_NOT_FOUND,
            // Book Interaction
            OWNER_OFFLINE,
            EDIT_NOT_ALLOWED,
        }
        private final Type type;
        public BookError(Type type) {
            super("Book error: " + type.name());
            this.type = type;
        }

        protected String getKey() {
            return type.name().toLowerCase();
        }
        public Type getType() {
            return type;
        }

        public IFormattableTextComponent getComponent() {
            return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), TextFormatting.RED);
        }
    }

    private final HashMap<String, TeleportLocation> locations = new HashMap<>();
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        locations.forEach((name, location) -> nbt.put(name, location.serialize()));

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        locations.clear();
        nbt.getAllKeys().forEach(name -> locations.put(name, TeleportLocation.deserialize(nbt.getCompound(name))));
    }

    @Override
    public ImmutableList<TeleportLocation> getLocations() {
        return locations.values().stream().sorted(Comparator.comparingInt(TeleportLocation::index)).collect(ImmutableList.toImmutableList());
    }

    @Override
    public void addLocation(String name, GlobalPos pos) throws BookError.DuplicateNameError {
        if(locations.containsKey(name) && !name.equals(BACK_KEY)) {
            throw new BookError.DuplicateNameError(name);
        }
        int index = locations.size();
        if(name.equals(BACK_KEY)) {
            index = -1;
        } else if(locations.containsKey(BACK_KEY)) {
            index -= 1;
        }
        TeleportLocation location = TeleportLocation.from(name, pos, index);
        locations.put(name, location);
    }

    @Override
    public void addLocation(PlayerEntity player, String name) throws BookError.DuplicateNameError {
        addLocation(name, GlobalPos.of(player.level.dimension(), player.blockPosition()));
    }

    @Override
    public void removeLocation(String name) throws BookError.NameNotFoundError {
        if(!locations.containsKey(name)) {
            throw new BookError.NameNotFoundError(name);
        }

        int index = locations.get(name).index();
        locations.remove(name);
        if(index < 0 || name.equals(BACK_KEY)) return;
        locations.forEach((loc, location) -> {
            if(location.index() < index) return;
            locations.replace(loc, location.withIndex(location.index() - 1));
        });
    }

    @Override
    public void saveBackLocation(PlayerEntity player, GlobalPos pos) {
        try {
            if(locations.containsKey(BACK_KEY)) {
                removeLocation(BACK_KEY);
            }

            addLocation(BACK_KEY, pos);
            sync((ServerPlayerEntity) player);
        } catch (BookError.NameNotFoundError | BookError.DuplicateNameError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveBackLocation(PlayerEntity player) {
        saveBackLocation(player, GlobalPos.of(player.level.dimension(), player.blockPosition()));
    }

    private static final String BACK_KEY = "@back";
    @Override
    public @Nullable TeleportLocation getBackLocation() {
        if(!locations.containsKey(BACK_KEY)) {
            return null;
        }

        return locations.get(BACK_KEY);
    }

    @Override
    public TeleportLocation getBackLocationOrThrow() throws BookError.NoBackLocationError {
        TeleportLocation location = getBackLocation();
        if(location == null) {
            throw new BookError.NoBackLocationError();
        }

        return location;
    }

    @Override
    public void teleportBack(ServerPlayerEntity player, boolean acrossDimensions) throws BookError.NoBackLocationError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        @Nullable TeleportLocation backLocation = getBackLocation();
        if(backLocation == null) {
            throw new BookError.NoBackLocationError();
        }

        RegistryKey<World> dim = player.getLevel().dimension();

        ServerWorld level = Util.getDimension(backLocation.dimension());
        if(level == null) {
            throw new BookError.DimensionNotFoundError(backLocation.dimension());
        }

        if(!dim.equals(backLocation.dimension()) && !acrossDimensions) {
            throw new BookError.WrongDimensionError();
        }

        try {
            removeLocation(BACK_KEY);
        } catch(BookError.NameNotFoundError e) {
            throw new RuntimeException(e);
        }
        sync(player);
        backLocation.teleportTo(player, acrossDimensions);
    }

    @Override
    public void teleportTo(String name, ServerPlayerEntity player, boolean acrossDimensions) throws BookError.NameNotFoundError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        GlobalPos pos = GlobalPos.of(player.level.dimension(), player.blockPosition());
        getLocationOrThrow(name).teleportTo(player, acrossDimensions);
        saveBackLocation(player, pos);
    }

    @Override
    public @Nullable TeleportLocation getLocation(String name) {
        return locations.get(name);
    }

    @Override
    public TeleportLocation getLocationOrThrow(String name) throws BookError.NameNotFoundError {
        @Nullable TeleportLocation location = getLocation(name);
        if(location == null) {
            throw new BookError.NameNotFoundError(name);
        }

        return location;
    }

    @Override
    public void resetLocations() {
        locations.clear();
    }

    @Override
    public void sync(ServerPlayerEntity player) {
        boolean canEdit = false;
        if(this.player != null) {
            canEdit = ItemAlchemicalBook.canEdit(player, this.player);
        } else if(itemStack != null) {
            canEdit = ItemAlchemicalBook.canEdit(itemStack, player);
        }
        PacketHandler.sendTo(new PacketSyncAlchemicalBookLocations(getLocations(), canEdit), player);
    }

    @Override
    public ItemAlchemicalBook.Mode getMode() {
        return mode;
    }

    @Override
    public @Nullable ServerPlayerEntity getPlayer() {
        return player;
    }

    @Override
    public ServerPlayerEntity getPlayerOrException() {
        if (player == null) {
            throw new NullPointerException("Player is null");
        }
        return player;
    }

    @Override
    public @Nullable ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public ItemStack getItemStackOrException() {
        if (itemStack == null) {
            throw new NullPointerException("ItemStack is null");
        }
        return itemStack;
    }

    @Override
    public void syncToOtherPlayers() {
        if (player == null) return;
        for (String playerName : ServerLifecycleHooks.getCurrentServer().getPlayerNames()) {
            ServerPlayerEntity target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
            if(target == null) continue;
            syncToPlayer(target);
        }
    }

    @Override
    public void syncToPlayer(ServerPlayerEntity target) {
        if (player == null) return;
        ItemStack stack = target.getMainHandItem();
        if (stack.getItem() instanceof ItemAlchemicalBook && ((ItemAlchemicalBook) stack.getItem()).getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            @Nullable ServerPlayerEntity stackOwner = ((ItemAlchemicalBook) stack.getItem()).getPlayer(stack);
            if(player.equals(stackOwner)) {
                fromPlayer(player).sync(target);
            }
        }
    }

    @Override
    public void ensureEditable(ServerPlayerEntity editor) throws BookError.EditNotAllowedError {
        if(player != null && !ItemAlchemicalBook.canEdit(editor, player)) {
            throw new BookError.EditNotAllowedError();
        }
    }
}
