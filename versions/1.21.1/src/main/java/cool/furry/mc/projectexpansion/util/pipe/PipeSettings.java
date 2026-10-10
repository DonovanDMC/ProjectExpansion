package cool.furry.mc.projectexpansion.util.pipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

// lives on the upgrade in a side, like Pipez, so moving the upgrade moves the settings
public record PipeSettings(RedstoneMode redstoneMode, Distribution distribution) {
    public static final PipeSettings DEFAULT = new PipeSettings(RedstoneMode.IGNORED, Distribution.ROUND_ROBIN);

    public static final Codec<PipeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StringRepresentable.fromEnum(RedstoneMode::values).optionalFieldOf("redstone_mode", DEFAULT.redstoneMode).forGetter(PipeSettings::redstoneMode),
            StringRepresentable.fromEnum(Distribution::values).optionalFieldOf("distribution", DEFAULT.distribution).forGetter(PipeSettings::distribution)
    ).apply(instance, PipeSettings::new));
    public static final StreamCodec<ByteBuf, PipeSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> RedstoneMode.VALUES[i], RedstoneMode::ordinal), PipeSettings::redstoneMode,
            ByteBufCodecs.idMapper(i -> Distribution.VALUES[i], Distribution::ordinal), PipeSettings::distribution,
            PipeSettings::new
    );

    public PipeSettings withRedstoneMode(RedstoneMode mode) {
        return new PipeSettings(mode, distribution);
    }

    public PipeSettings withDistribution(Distribution value) {
        return new PipeSettings(redstoneMode, value);
    }

    // what actually applies, the upgrade has to unlock a setting for it to count
    public PipeSettings effective(@Nullable PipeUpgrade upgrade) {
        return new PipeSettings(
                PipeUpgrade.canChangeRedstoneMode(upgrade) ? redstoneMode : DEFAULT.redstoneMode,
                PipeUpgrade.canChangeDistribution(upgrade) ? distribution : DEFAULT.distribution
        );
    }
}
