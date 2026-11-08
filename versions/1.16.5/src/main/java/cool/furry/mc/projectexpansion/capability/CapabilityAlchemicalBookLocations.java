package cool.furry.mc.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class CapabilityAlchemicalBookLocations implements IAlchemicalBookLocationsProvider {
    private final ItemAlchemicalBook.Mode mode;
    @Nullable private final ServerPlayerEntity player;
    @Nullable private final ItemStack itemStack;

    public CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayerEntity player, @Nullable ItemStack itemStack) {
        this.mode = mode;
        this.player = player;
        this.itemStack = itemStack;
    }

    public ItemAlchemicalBook.Mode mode() {
        return mode;
    }

    @Nullable public ServerPlayerEntity player() {
        return player;
    }

    @Nullable public ItemStack itemStack() {
        return itemStack;
    }

    @Override
    public String toString() {
        return "CapabilityAlchemicalBookLocations[mode=" + mode + ", player=" + player + ", itemStack=" + itemStack + "]";
    }

    public static final int BASIC_DISTANCE_RATIO = 1000;
    public static final int ADVANCED_DISTANCE_RATIO = 500;
    public static final int MASTER_DISTANCE_RATIO = 100;
    public static final int ARCANE_DISTANCE_RATIO = 0;

    public static IAlchemicalBookLocationsProvider fromPlayer(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity) || !player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            throw new IllegalStateException("PlayerEntity does not have expected capability");
        }
        ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
        return new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.PLAYER, serverPlayer, null);
    }

    public static IAlchemicalBookLocationsProvider fromItemStack(ItemStack stack) {
        if (!stack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            throw new IllegalStateException("ItemStack does not have expected capability");
        }
        return new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.STACK, null, stack);
    }

    public static IAlchemicalBookLocationsProvider from(ItemStack stack) throws BookError.OwnerOfflineError {
        if (!(stack.getItem() instanceof ItemAlchemicalBook)) {
            throw new IllegalArgumentException("ItemStack is not an alchemical book");
        }
        ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();

        if (book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            ServerPlayerEntity owner = book.getPlayer(stack);
            if (owner == null) {
                throw new BookError.OwnerOfflineError(Util.getOwner(stack).name());
            }
            return fromPlayer(owner);
        } else {
            return fromItemStack(stack);
        }
    }

    public static boolean isForbiddenName(String name) {
        return name.equalsIgnoreCase(BACK_KEY);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TeleportLocation that = (TeleportLocation) o;
            return Objects.equals(name, that.name) && Objects.equals(x, that.x) && Objects.equals(y, that.y) && Objects.equals(z, that.z) && Objects.equals(dimension, that.dimension) && Objects.equals(index, that.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, x, y, z, dimension, index);
        }

        @Override
        public String toString() {
            return "TeleportLocation[name=" + name + ", x=" + x + ", y=" + y + ", z=" + z + ", dimension=" + dimension + ", index=" + index + "]";
        }

        public void write(PacketBuffer buf) {
            buf.writeUtf(name);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeResourceLocation(dimension.location());
            buf.writeInt(index);
        }

        public static TeleportLocation read(PacketBuffer buf) {
            return new TeleportLocation(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), RegistryKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation()), buf.readInt());
        }

        public void teleportTo(ServerPlayerEntity player, boolean acrossDimensions) throws BookError.DimensionNotFoundError, BookError.WrongDimensionError {
            RegistryKey<World> dim = player.level.dimension();

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

        // as if both were in the overworld, so going between dimensions is measured like a portal would
        public double distanceFrom(World level, BlockPos pos) {
            if (level.dimension().equals(dimension)) return distanceFrom(pos);
            double from = Util.coordinateScale(level, level.dimension()), to = Util.coordinateScale(level, dimension);
            return Math.sqrt(Math.pow(pos.getX() * from - x * to, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() * from - z * to, 2));
        }

        public int getCost(ItemStack stack, PlayerEntity player) {
            if (player.isCreative()) {
                return 0;
            }

            return getCost(stack, player.level, player.blockPosition().below());
        }

        public int getCost(ItemStack stack, World level, BlockPos pos) {
            float ratio = stack.getItem() instanceof ItemAlchemicalBook ? ((ItemAlchemicalBook) stack.getItem()).getTier().distanceRatio() : BASIC_DISTANCE_RATIO;
            double cost = distanceFrom(level, pos) * ratio;
            // crossing dimensions costs a set amount on top, free books stay free
            if (ratio > 0 && !level.dimension().equals(dimension)) cost += Config.server.alchemicalBookDimensionCost.get();
            return (int) Math.min(Integer.MAX_VALUE, Math.ceil(cost));
        }

        // records are immutable
        public TeleportLocation setIndex(int index) {
            if (index() == index) return this;
            return from(name, new BlockPos(x, y, z), dimension, index);
        }

        public boolean isBack() {
            return name.equals(BACK_KEY);
        }

        public CompoundNBT serialize() {
            CompoundNBT tag = new CompoundNBT();
            tag.putString(TagNames.NAME, name);
            tag.putInt(TagNames.X, x);
            tag.putInt(TagNames.Y, y);
            tag.putInt(TagNames.Z, z);
            tag.putInt(TagNames.INDEX, index);
            tag.putString(TagNames.DIMENSION, dimension.location().toString());
            return tag;
        }

        public static TeleportLocation deserialize(CompoundNBT tag) {
            String name = tag.getString(TagNames.NAME);
            int x = tag.getInt(TagNames.X);
            int y = tag.getInt(TagNames.Y);
            int z = tag.getInt(TagNames.Z);
            int index = tag.getInt(TagNames.INDEX);
            RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString(TagNames.DIMENSION)));
            return new TeleportLocation(name, x, y, z, dimension, index);
        }

        public TeleportLocation copy() {
            return new TeleportLocation(name, x, y, z, dimension, index);
        }

        // make a copy if the input matches current, used to avoid double copying when paired with setIndex
        public TeleportLocation copyIfEquals(TeleportLocation location) {
            if (location == this) return copy();
            return this;
        }

        public static TeleportLocation from(String name, GlobalPos pos, int index) {
            return from(name, pos.pos(), pos.dimension(), index);
        }

        public static TeleportLocation from(String name, BlockPos pos, RegistryKey<World> dimension, int index) {
            return new TeleportLocation(name, pos.getX(), pos.getY(), pos.getZ(), dimension, index);
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
                this(new TranslationTextComponent(level.location().getNamespace() + "." + level.location().getPath()));
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

    private Storage getStorage() {
        LazyOptional<Storage> storage;
        if (player != null) {
            storage = player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS);
        } else if (itemStack != null) {
            storage = itemStack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS);
        } else {
            throw new NullPointerException("Both PlayerEntity and ItemStack are null");
        }
        return storage.orElseThrow(() -> new IllegalStateException("Missing alchemical book locations capability"));
    }

    private AlchemicalBookLocationData getData() {
        return getStorage().data;
    }

    @Override
    public ImmutableList<TeleportLocation> getLocations() {
        return getData().getLocations().stream().sorted(Comparator.comparingInt(TeleportLocation::index)).collect(ImmutableList.toImmutableList());
    }

    @Override
    public void addLocation(String name, GlobalPos pos) throws BookError.DuplicateNameError {
        AlchemicalBookLocationData data = getData();
        if (data.hasLocation(name) && !name.equals(BACK_KEY)) {
            throw new BookError.DuplicateNameError(name);
        }
        int lastIndex = -1;
        try {
            lastIndex = data.getLocations().get(data.getLocations().size() - 1).index();
        } catch (NoSuchElementException ignore) {
        }
        TeleportLocation location = TeleportLocation.from(name, pos, lastIndex + 1);
        data = data.addLocation(name, location);
        markDirty(data);
    }

    @Override
    public void addLocation(PlayerEntity player, String name) throws BookError.DuplicateNameError {
        addLocation(name, GlobalPos.of(player.level.dimension(), player.blockPosition()));
    }

    @Override
    public void reindex() {
        AlchemicalBookLocationData data = getData();
        data = data.reindex();
        markDirty(data);
    }

    @Override
    public void removeLocation(String name) throws BookError.NameNotFoundError {
        AlchemicalBookLocationData data = getData();
        if (!data.hasLocation(name)) {
            throw new BookError.NameNotFoundError(name);
        }

        data = data.removeLocation(name);
        markDirty(data);
    }

    @Override
    public void saveBackLocation(PlayerEntity player, GlobalPos pos) {
        try {
            AlchemicalBookLocationData data = getData();
            if (data.hasLocation(BACK_KEY)) {
                data = data.removeLocation(BACK_KEY);
                markDirty(data);
            }

            addLocation(BACK_KEY, pos);
            sync((ServerPlayerEntity) player);
        } catch (BookError.DuplicateNameError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveBackLocation(PlayerEntity player) {
        saveBackLocation(player, GlobalPos.of(player.level.dimension(), player.blockPosition()));
    }

    private void setLocations(List<TeleportLocation> locations) {
        AlchemicalBookLocationData data = getData().setLocations(locations);
        markDirty(data);
    }

    private static final String BACK_KEY = "@back";
    private static final int BACK_INDEX = 0;

    @Override
    public @Nullable TeleportLocation getBackLocation() {
        AlchemicalBookLocationData data = getData();
        if (!data.hasLocation(BACK_KEY)) {
            return null;
        }

        return data.getLocation(BACK_KEY);
    }

    @Override
    public TeleportLocation getBackLocationOrThrow() throws BookError.NoBackLocationError {
        TeleportLocation location = getBackLocation();
        if (location == null) {
            throw new BookError.NoBackLocationError();
        }

        return location;
    }

    @Override
    public void teleportBack(ServerPlayerEntity player, boolean acrossDimensions) throws BookError.NoBackLocationError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        @Nullable TeleportLocation backLocation = getBackLocation();
        if (backLocation == null) {
            throw new BookError.NoBackLocationError();
        }

        RegistryKey<World> dim = player.level.dimension();

        ServerWorld level = Util.getDimension(backLocation.dimension());
        if (level == null) {
            throw new BookError.DimensionNotFoundError(backLocation.dimension());
        }

        if (!dim.equals(backLocation.dimension()) && !acrossDimensions) {
            throw new BookError.WrongDimensionError();
        }

        try {
            removeLocation(BACK_KEY);
        } catch (BookError.NameNotFoundError e) {
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
        return getData().getLocations().stream().filter(location -> location.name().equals(name)).findFirst().orElse(null);
    }

    @Override
    public TeleportLocation getLocationOrThrow(String name) throws BookError.NameNotFoundError {
        @Nullable TeleportLocation location = getLocation(name);
        if (location == null) {
            throw new BookError.NameNotFoundError(name);
        }

        return location;
    }

    @Override
    public void resetLocations() {
        AlchemicalBookLocationData data = getData();
        data = data.clearLocations();
        markDirty(data);
    }

    @Override
    public void sync(ServerPlayerEntity player) {
        boolean canEdit = false;
        if (this.player != null) {
            canEdit = ItemAlchemicalBook.canEdit(player, this.player);
        } else if (itemStack != null) {
            canEdit = ItemAlchemicalBook.canEdit(itemStack, player);
        }
        PacketHandler.sendToPlayer(player, new PacketSyncAlchemicalBookLocations(getLocations(), canEdit));
    }

    @Override
    public ServerPlayerEntity getPlayerOrException() {
        if (player == null) {
            throw new NullPointerException("PlayerEntity is null");
        }
        return player;
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
        for (String playerName : Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer()).getPlayerNames()) {
            ServerPlayerEntity target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
            if (target == null) continue;
            syncToPlayer(target);
        }
    }

    @Override
    public void syncToPlayer(ServerPlayerEntity target) {
        if (player == null) return;
        ItemStack stack = target.getMainHandItem();
        if (stack.getItem() instanceof ItemAlchemicalBook && ((ItemAlchemicalBook) stack.getItem()).getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
            @Nullable ServerPlayerEntity stackOwner = book.getPlayer(stack);
            if (player.equals(stackOwner)) {
                fromPlayer(player).sync(target);
            }
        }
    }

    @Override
    public void ensureEditable(ServerPlayerEntity editor) throws BookError.EditNotAllowedError {
        if (player != null && !ItemAlchemicalBook.canEdit(editor, player)) {
            throw new BookError.EditNotAllowedError();
        }
    }

    private void markDirty(AlchemicalBookLocationData provider) {
        getStorage().data = provider;
    }

    // attached to players and alchemical books, the nbt format matches what older versions saved
    public static class Storage implements ICapabilitySerializable<CompoundNBT> {
        public static final ResourceLocation NAME = Main.rl("alchemical_book_locations");
        private final LazyOptional<Storage> holder = LazyOptional.of(() -> this);
        private AlchemicalBookLocationData data = new AlchemicalBookLocationData();

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return Capabilities.ALCHEMICAL_BOOK_LOCATIONS.orEmpty(cap, holder);
        }

        public void invalidate() {
            holder.invalidate();
        }

        public void copyFrom(Storage other) {
            data = other.data.copy();
        }

        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT nbt = new CompoundNBT();
            data.getLocations().forEach(location -> nbt.put(location.name(), location.serialize()));
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            data = new AlchemicalBookLocationData(nbt.getAllKeys().stream().map(name -> TeleportLocation.deserialize(nbt.getCompound(name))).collect(Collectors.toList())).reindex();
        }
    }

    public static class AlchemicalBookLocationData {
        private final ImmutableList<TeleportLocation> locations;

        public AlchemicalBookLocationData() {
            this(new ArrayList<>());
        }

        private AlchemicalBookLocationData(List<TeleportLocation> locations) {
            this.locations = ImmutableList.copyOf(locations);
        }

        public AlchemicalBookLocationData copy() {
            List<TeleportLocation> locationsCopy = new ArrayList<>();
            for (TeleportLocation location : getLocations()) {
                locationsCopy.add(location.copy());
            }
            return new AlchemicalBookLocationData(locationsCopy);
        }

        public ImmutableList<TeleportLocation> getLocations() {
            return ImmutableList.copyOf(locations.stream().sorted(Comparator.comparingInt(TeleportLocation::index)).collect(Collectors.toList()));
        }

        public boolean hasLocation(String name) {
            return getLocations().stream().anyMatch(location -> location.name().equals(name));
        }

        public @Nullable TeleportLocation getLocation(String name) {
            return getLocations().stream().filter(location -> location.name().equals(name)).findFirst().orElse(null);
        }

        public AlchemicalBookLocationData removeLocation(String name) {
            List<TeleportLocation> newLocations = new ArrayList<>();

            for (TeleportLocation location : getLocations()) {
                if (location.name().equals(name)) continue;
                newLocations.add(location.copy());
            }

            return setLocations(newLocations).reindex();
        }

        public AlchemicalBookLocationData addLocation(String name, TeleportLocation newLocation) {
            List<TeleportLocation> newLocations = new ArrayList<>();
            for (TeleportLocation location : getLocations()) {
                newLocations.add(location.copy());
            }
            newLocations.add(newLocation);
            return setLocations(newLocations).reindex();
        }

        public AlchemicalBookLocationData clearLocations() {
            return setLocations(new ArrayList<>());
        }

        public AlchemicalBookLocationData setLocations(List<TeleportLocation> locations) {
            return new AlchemicalBookLocationData(locations);
        }

        public AlchemicalBookLocationData reindex() {
            List<TeleportLocation> locations = getLocations();
            if (locations.isEmpty()) return this;
            ArrayList<TeleportLocation> newLocations = new ArrayList<>();

            int index = BACK_INDEX + 1; // the back index should never be used for a normal location
            for (TeleportLocation location : locations) {
                if (location.isBack()) {
                    newLocations.add(location.setIndex(BACK_INDEX).copyIfEquals(location));
                    continue;
                }
                newLocations.add(location.setIndex(index).copyIfEquals(location));
                index++;
            }

            return setLocations(newLocations);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AlchemicalBookLocationData that = (AlchemicalBookLocationData) o;
            return Objects.equals(locations, that.locations);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(locations);
        }
    }
}
