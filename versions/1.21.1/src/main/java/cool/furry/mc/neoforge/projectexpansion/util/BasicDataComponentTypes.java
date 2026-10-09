package cool.furry.mc.neoforge.projectexpansion.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@SuppressWarnings("unused")
public class BasicDataComponentTypes {
    public record StringValue(String value) {
        public static final Codec<StringValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.STRING.fieldOf(TagNames.VALUE).forGetter(StringValue::value)).apply(instance, StringValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, StringValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, StringValue:: value, StringValue::new);
    }

    public record BooleanValue(boolean value) {
        public static final Codec<BooleanValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.BOOL.fieldOf(TagNames.VALUE).forGetter(BooleanValue::value)).apply(instance, BooleanValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, BooleanValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, BooleanValue::value, BooleanValue::new);
    }

    public record ByteValue(byte value) {
        public static final Codec<ByteValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.BYTE.fieldOf(TagNames.VALUE).forGetter(ByteValue::value)).apply(instance, ByteValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ByteValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BYTE, ByteValue::value, ByteValue::new);
    }

    public record ShortValue(short value) {
        public static final Codec<ShortValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.SHORT.fieldOf(TagNames.VALUE).forGetter(ShortValue::value)).apply(instance, ShortValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ShortValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.SHORT, ShortValue::value, ShortValue::new);
    }

    public record IntValue(int value) {
        public static final Codec<IntValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.INT.fieldOf(TagNames.VALUE).forGetter(IntValue::value)).apply(instance, IntValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, IntValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, IntValue::value, IntValue::new);
    }

    public record FloatValue(float value) {
        public static final Codec<FloatValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.FLOAT.fieldOf(TagNames.VALUE).forGetter(FloatValue::value)).apply(instance, FloatValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, FloatValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, FloatValue::value, FloatValue::new);
    }

    public record DoubleValue(double value) {
        public static final Codec<DoubleValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.DOUBLE.fieldOf(TagNames.VALUE).forGetter(DoubleValue::value)).apply(instance, DoubleValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, DoubleValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.DOUBLE, DoubleValue::value, DoubleValue::new);
    }

    public record LongValue(long value) {
        public static final Codec<LongValue> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.LONG.fieldOf(TagNames.VALUE).forGetter(LongValue::value)).apply(instance, LongValue::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, LongValue> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, LongValue::value, LongValue::new);
    }
}
