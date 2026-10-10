package cool.furry.mc.projectexpansion.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.util.BasicDataComponentTypes;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.Util;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class DataComponentTypes {
    public static final DeferredRegister.DataComponents Registry = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Main.MOD_ID);

    public record OwnerData(UUID uuid, String name) {
        public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(UUIDUtil.CODEC.fieldOf(TagNames.OWNER).forGetter(OwnerData::uuid), Codec.STRING.fieldOf(TagNames.OWNER_NAME).forGetter(OwnerData::name)).apply(instance, OwnerData::new)
        );
        public static final StreamCodec<ByteBuf, OwnerData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, OwnerData::uuid, ByteBufCodecs.STRING_UTF8, OwnerData::name, OwnerData::new);

        public boolean isNone() {
            return uuid == Util.DUMMY_UUID;
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OwnerData>> OWNER = Registry.registerComponentType("uuid", (builder) -> builder.persistent(OwnerData.CODEC).networkSynchronized(OwnerData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapabilityAlchemicalBookLocations.AlchemicalBookLocationData>> ALCHEMICAL_BOOK_LOCATIONS = Registry.registerComponentType("alchemical_book_locations", (builder) -> builder.persistent(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.CODEC).networkSynchronized(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> LAST_USED = Registry.registerComponentType("last_used", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> KNOWLEDGE_GAINED = Registry.registerComponentType("knowledge_gained", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    // presence means the stack can't be put into anything that converts it to EMC
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> EMC_LOCKED = Registry.registerComponentType("emc_locked", (builder) -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));



    // redstone mode and distribution, stored on pipe upgrades
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PipeSettings>> PIPE_SETTINGS = Registry.registerComponentType("pipe_settings", (builder) -> builder.persistent(PipeSettings.CODEC).networkSynchronized(PipeSettings.STREAM_CODEC));
}
