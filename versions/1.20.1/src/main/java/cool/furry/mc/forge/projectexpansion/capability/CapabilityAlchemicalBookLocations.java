package cool.furry.mc.forge.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.registries.Capabilities;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public record CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayer player, @Nullable ItemStack itemStack) implements IAlchemicalBookLocationsProvider {
    public static final int BASIC_DISTANCE_RATIO = 1000;
    public static final int ADVANCED_DISTANCE_RATIO = 500;
    public static final int MASTER_DISTANCE_RATIO = 100;
    public static final int ARCANE_DISTANCE_RATIO = 0;

    public static IAlchemicalBookLocationsProvider fromPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            throw new IllegalStateException("Player does not have expected capability");
        }
        return new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.PLAYER, serverPlayer, null);
    }

    public static IAlchemicalBookLocationsProvider fromItemStack(ItemStack stack) {
        if (!stack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS).isPresent()) {
            throw new IllegalStateException("ItemStack does not have expected capability");
        }
        return new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.STACK, null, stack);
    }

    public static IAlchemicalBookLocationsProvider from(ItemStack stack) throws BookError.OwnerOfflineError {
        if (!(stack.getItem() instanceof ItemAlchemicalBook book)) {
            throw new IllegalArgumentException("ItemStack is not an alchemical book");
        }

        if (book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            ServerPlayer owner = book.getPlayer(stack);
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

    public record TeleportLocation(String name, int x, int y, int z, ResourceKey<Level> dimension, int index) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeResourceKey(dimension);
            buf.writeInt(index);
        }

        public static TeleportLocation read(FriendlyByteBuf buf) {
            return new TeleportLocation(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readResourceKey(Registries.DIMENSION), buf.readInt());
        }

        public void teleportTo(ServerPlayer player, boolean acrossDimensions) throws BookError.DimensionNotFoundError, BookError.WrongDimensionError {
            ResourceKey<Level> dim = player.level().dimension();

            ServerLevel level = Util.getDimension(dimension);
            if (level == null) {
                throw new BookError.DimensionNotFoundError(dimension);
            }

            if (!dim.equals(dimension) && !acrossDimensions) {
                throw new BookError.WrongDimensionError();
            }

            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        }

        public double distanceFrom(BlockPos pos) {
            return Math.sqrt(Math.pow(pos.getX() - x, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() - z, 2));
        }

        public int getCost(ItemStack stack, Player player) {
            if (player.isCreative()) {
                return 0;
            }

            return getCost(stack, player.getOnPos());
        }

        public int getCost(ItemStack stack, BlockPos pos) {
            if (!(stack.getItem() instanceof ItemAlchemicalBook book)) {
                return (int) Math.ceil(distanceFrom(pos) * BASIC_DISTANCE_RATIO);
            }
            return (int) Math.ceil(distanceFrom(pos) * book.getTier().distanceRatio());
        }

        // records are immutable
        public TeleportLocation setIndex(int index) {
            if (index() == index) return this;
            return from(name, new BlockPos(x, y, z), dimension, index);
        }

        public boolean isBack() {
            return name.equals(BACK_KEY);
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putString(TagNames.NAME, name);
            tag.putInt(TagNames.X, x);
            tag.putInt(TagNames.Y, y);
            tag.putInt(TagNames.Z, z);
            tag.putInt(TagNames.INDEX, index);
            tag.putString(TagNames.DIMENSION, dimension.location().toString());
            return tag;
        }

        public static TeleportLocation deserialize(CompoundTag tag) {
            String name = tag.getString(TagNames.NAME);
            int x = tag.getInt(TagNames.X);
            int y = tag.getInt(TagNames.Y);
            int z = tag.getInt(TagNames.Z);
            int index = tag.getInt(TagNames.INDEX);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString(TagNames.DIMENSION)));
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

        public static TeleportLocation from(String name, BlockPos pos, ResourceKey<Level> dimension, int index) {
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
            private final Component name;

            public DimensionNotFoundError(Component name) {
                super(Type.DIMENSION_NOT_FOUND);
                this.name = name;
            }

            public DimensionNotFoundError(ResourceKey<Level> level) {
                this(Component.translatable(level.location().toLanguageKey()));
            }

            public DimensionNotFoundError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }

        public static class NoBackLocationError extends BookError {
            public NoBackLocationError() {
                super(Type.NO_BACK_LOCATION);
            }
        }

        public static class DuplicateNameError extends BookError {
            private final Component name;

            public DuplicateNameError(Component name) {
                super(Type.DUPLICATE_NAME);
                this.name = name;
            }

            public DuplicateNameError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }

        public static class NameNotFoundError extends BookError {
            private final Component name;

            public NameNotFoundError(Component name) {
                super(Type.NAME_NOT_FOUND);
                this.name = name;
            }

            public NameNotFoundError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }

        public static class OwnerOfflineError extends BookError {
            private final Component player;

            public OwnerOfflineError(Component name) {
                super(Type.OWNER_OFFLINE);
                this.player = name;
            }

            public OwnerOfflineError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, player);
            }
        }

        public static class NotEnoughEMCError extends BookError {
            private final String emc;

            public NotEnoughEMCError(String emc) {
                super(Type.NOT_ENOUGH_EMC);
                this.emc = emc;
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, emc);
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

        public MutableComponent getComponent() {
            return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED);
        }
    }

    private Storage getStorage() {
        LazyOptional<Storage> storage;
        if (player != null) {
            storage = player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS);
        } else if (itemStack != null) {
            storage = itemStack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS);
        } else {
            throw new NullPointerException("Both Player and ItemStack are null");
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
    public void addLocation(Player player, String name) throws BookError.DuplicateNameError {
        addLocation(name, GlobalPos.of(player.level().dimension(), player.blockPosition()));
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
    public void saveBackLocation(Player player, GlobalPos pos) {
        try {
            AlchemicalBookLocationData data = getData();
            if (data.hasLocation(BACK_KEY)) {
                data = data.removeLocation(BACK_KEY);
                markDirty(data);
            }

            addLocation(BACK_KEY, pos);
            sync((ServerPlayer) player);
        } catch (BookError.DuplicateNameError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveBackLocation(Player player) {
        saveBackLocation(player, GlobalPos.of(player.level().dimension(), player.blockPosition()));
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
    public void teleportBack(ServerPlayer player, boolean acrossDimensions) throws BookError.NoBackLocationError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        @Nullable TeleportLocation backLocation = getBackLocation();
        if (backLocation == null) {
            throw new BookError.NoBackLocationError();
        }

        ResourceKey<Level> dim = player.level().dimension();

        ServerLevel level = Util.getDimension(backLocation.dimension());
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
    public void teleportTo(String name, ServerPlayer player, boolean acrossDimensions) throws BookError.NameNotFoundError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        GlobalPos pos = GlobalPos.of(player.level().dimension(), player.blockPosition());
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
    public void sync(ServerPlayer player) {
        boolean canEdit = false;
        if (this.player != null) {
            canEdit = ItemAlchemicalBook.canEdit(player, this.player);
        } else if (itemStack != null) {
            canEdit = ItemAlchemicalBook.canEdit(itemStack, player);
        }
        PacketHandler.sendToPlayer(player, new PacketSyncAlchemicalBookLocations(getLocations(), canEdit));
    }

    @Override
    public ServerPlayer getPlayerOrException() {
        if (player == null) {
            throw new NullPointerException("Player is null");
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
            ServerPlayer target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
            if (target == null) continue;
            syncToPlayer(target);
        }
    }

    @Override
    public void syncToPlayer(ServerPlayer target) {
        if (player == null) return;
        ItemStack stack = target.getMainHandItem();
        if (stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            @Nullable ServerPlayer stackOwner = book.getPlayer(stack);
            if (player.equals(stackOwner)) {
                fromPlayer(player).sync(target);
            }
        }
    }

    @Override
    public void ensureEditable(ServerPlayer editor) throws BookError.EditNotAllowedError {
        if (player != null && !ItemAlchemicalBook.canEdit(editor, player)) {
            throw new BookError.EditNotAllowedError();
        }
    }

    private void markDirty(AlchemicalBookLocationData provider) {
        getStorage().data = provider;
    }

    // attached to players and alchemical books, the nbt format matches what older versions saved
    public static class Storage implements ICapabilitySerializable<CompoundTag> {
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
        public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            data.getLocations().forEach(location -> nbt.put(location.name(), location.serialize()));
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data = new AlchemicalBookLocationData(nbt.getAllKeys().stream().map(name -> TeleportLocation.deserialize(nbt.getCompound(name))).toList()).reindex();
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
