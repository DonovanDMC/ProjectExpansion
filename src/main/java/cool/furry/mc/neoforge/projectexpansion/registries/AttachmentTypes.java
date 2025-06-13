package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations.AlchemicalBookLocationData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> Registry = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Main.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AlchemicalBookLocationData>> ALCHEMICAL_BOOK_LOCATIONS = Registry.register("alchemical_book_locations",
            () -> AttachmentType.builder(AlchemicalBookLocationData::new)
                    .serialize(AlchemicalBookLocationData.CODEC)
                    .copyHandler(AlchemicalBookLocationData::copy)
                    .copyOnDeath()
                    .build()
    );
}
