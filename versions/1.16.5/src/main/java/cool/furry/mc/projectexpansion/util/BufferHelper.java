package cool.furry.mc.projectexpansion.util;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

// collection & optional helpers that PacketBuffer only gained in 1.17
public class BufferHelper {
    public static <T> void writeCollection(PacketBuffer buf, Collection<T> collection, BiConsumer<PacketBuffer, T> writer) {
        buf.writeVarInt(collection.size());
        for (T value : collection) writer.accept(buf, value);
    }

    public static <T> List<T> readList(PacketBuffer buf, Function<PacketBuffer, T> reader) {
        int size = buf.readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(reader.apply(buf));
        return list;
    }

    public static <T> void writeOptional(PacketBuffer buf, Optional<T> optional, BiConsumer<PacketBuffer, T> writer) {
        buf.writeBoolean(optional.isPresent());
        optional.ifPresent(value -> writer.accept(buf, value));
    }

    public static <T> Optional<T> readOptional(PacketBuffer buf, Function<PacketBuffer, T> reader) {
        return buf.readBoolean() ? Optional.of(reader.apply(buf)) : Optional.empty();
    }
}
