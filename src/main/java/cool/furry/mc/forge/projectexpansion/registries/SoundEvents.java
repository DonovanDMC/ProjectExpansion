package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> Registry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);

    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_STORE = Registry.register("knowledge_sharing_book.store", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.store")));
    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_USE = Registry.register("knowledge_sharing_book.use", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.use")));
    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_USE_NONE = Registry.register("knowledge_sharing_book.use_none", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.use_none")));
    public static final RegistryObject<SoundEvent> ALCHEMICAL_COLLECTION_COLLECT = Registry.register("alchemical_collection.collect", () -> SoundEvent.createVariableRangeEvent(Main.rl("alchemical_collection.collect")));
}