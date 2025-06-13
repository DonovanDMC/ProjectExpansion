package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> Registry = DeferredRegister.create(Registries.SOUND_EVENT, Main.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> KNOWLEDGE_SHARING_BOOK_STORE = Registry.register("knowledge_sharing_book.store", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.store")));
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOWLEDGE_SHARING_BOOK_USE = Registry.register("knowledge_sharing_book.use", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.use")));
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOWLEDGE_SHARING_BOOK_USE_NONE = Registry.register("knowledge_sharing_book.use_none", () -> SoundEvent.createVariableRangeEvent(Main.rl("knowledge_sharing_book.use_none")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ALCHEMICAL_COLLECTION_COLLECT = Registry.register("alchemical_collection.collect", () -> SoundEvent.createVariableRangeEvent(Main.rl("alchemical_collection.collect")));
}