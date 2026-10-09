package cool.furry.mc.neoforge.projectexpansion.util;

import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

public enum SearchType implements TranslatableEnum, ILangEntry {
    NORMAL(Lang.Configuration.SEARCH_TYPE_NORMAL, false, false),
    NORMAL_AUTOFOCUS(Lang.Configuration.SEARCH_TYPE_NORMAL_AUTOFOCUS, true, false),
    SYNC(Lang.Configuration.SEARCH_TYPE_SYNC, false, true),
    SYNC_AUTOFOCUS(Lang.Configuration.SEARCH_TYPE_SYNC_AUTOFOCUS, true, true),
    ;

    public static final SearchType[] VALUES = values();

    public final ILangEntry translation;
    public final boolean autoFocus;
    public final boolean syncSearch;

    SearchType(ILangEntry translation, boolean autoFocus, boolean syncSearch) {
        this.translation = translation;
        this.autoFocus = autoFocus;
        this.syncSearch = syncSearch;
    }

    @Override
    public String getTranslationKey() {
        return translation.getTranslationKey();
    }

    @Override
    public Component getTranslatedName() {
        return translation.translate();
    }

    public boolean isAvailable() {
        return !syncSearch || SearchSync.hasAny();
    }

    public void sync(String text) {
        if (syncSearch) SearchSync.syncAll(text);
    }

    public SearchType next() {
        SearchType[] values = VALUES;
        int next = (ordinal() + 1) % values.length;
        while (!values[next].isAvailable()) {
            next = (next + 1) % values.length;
        }
        return values[next];
    }
}
