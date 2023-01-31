package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> Registry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);

    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_STORE = Registry.register("knowledge_sharing_book.store", () -> new SoundEvent(new ResourceLocation(Main.MOD_ID, "knowledge_sharing_book.store")));
    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_USE = Registry.register("knowledge_sharing_book.use", () -> new SoundEvent(new ResourceLocation(Main.MOD_ID, "knowledge_sharing_book.use")));
    public static final RegistryObject<SoundEvent> KNOWLEDGE_SHARING_BOOK_USE_NONE = Registry.register("knowledge_sharing_book.use_none", () -> new SoundEvent(new ResourceLocation(Main.MOD_ID, "knowledge_sharing_book.use_none")));
    public static final RegistryObject<SoundEvent> ALCHEMICAL_COLLECTION_COLLECT = Registry.register("alchemical_collection.collect", () -> new SoundEvent(new ResourceLocation(Main.MOD_ID, "alchemical_collection.collect")));
}
