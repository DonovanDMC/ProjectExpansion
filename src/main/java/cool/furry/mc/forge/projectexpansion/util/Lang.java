package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;

@SuppressWarnings("unused")
public enum Lang implements ILangEntry {
    // Text
    WIP("text", "wip"),
    PROVIDER_ERROR("text", "provider_error"),
    SEE_WIKI("text", "see_wiki"),
    COST("text", "cost"),
    NOT_OWNER("text", "not_owner"),
    NBT_FILTER_DISABLED("text", "nbt_filter", "disabled"),
    NBT_FILTER_ENABLED("text", "nbt_filter", "enabled"),
    FAILED_TO_GET_KNOWLEDGE_PROVIDER("text", "failed_to_get_knowledge_provider"),
    LEARNED("text", "learned"),
    NOT_LEARNED("text", "not_learned"),
    ENABLED("text", "enabled"),
    DISABLED("text", "disabled"),
    ALCHEMICAL_COLLECTION("text", "alchemical_collection"),
    POS_OUTOFBOUNDS("text", "pos", "outofbounds"),

    // Enchantments
    ENCHANTMENT_ALCHEMICAL_COLLECTION("enchantment", "alchemical_collection"),
    ENCHANTMENT_ALCHEMIAL_COLLECTION_DESC("enchantment", "alchemical_collection", "desc"),

    // Misc
    ADVANCED_ALCHEMICAL_CHEST_TITLE("gui", "advanced_alchemical_chest", "title"),
    ALCHEMICAL_BOOK("gui", "alchemical_book"),
    ALCHEMICAL_BOOK_CLOSE("gui", "alchemical_book", "close"),
    ALCHEMICAL_BOOK_DELETE("gui", "alchemical_book", "delete"),
    ALCHEMICAL_BOOK_CREATE("gui", "alchemical_book", "create"),
    ALCHEMICAL_BOOK_BACK("gui", "alchemical_book", "back"),
    ALCHEMICAL_BOOK_COST("gui", "alchemical_book", "cost"),
    ALCHEMICAL_BOOK_DISTANCE("gui", "alchemical_book", "distance"),
    ALCHEMICAL_BOOK_DIMENSION("gui", "alchemical_book", "dimension"),
    ALCHEMICAL_BOOK_NO_BACK_LOCATION("gui", "alchemical_book", "no_back_location"),
    ;

    private String key;
    Lang(String type, String... path) {
        this(net.minecraft.util.Util.makeDescriptionId(type, Main.rl(String.join(".", path))));
    }

    Lang(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }

    public IFormattableTextComponent extend(String extension, Object... args) {
        String originalKey = this.key;
        this.key += "." + extension;
        IFormattableTextComponent component = translate(args);
        this.key = originalKey;
        return component;
    }

    public IFormattableTextComponent extendColored(String extension, TextFormatting color, Object... args) {
        String originalKey = this.key;
        this.key += "." + extension;
        IFormattableTextComponent component = translateColored(color, args);
        this.key = originalKey;
        return component;
    }

    public enum Blocks implements ILangEntry {
        // Advanced Alchemical Chest
        ADVANCED_ALCHEMICAL_CHEST_COLOR_SET("advanced_alchemical_chest", "color_set"),
        ADVANCED_ALCHEMICAL_CHEST_INVLID_ITEM("advanced_alchemical_chest", "invalid_item"),
        ADVANCED_ALCHEMICAL_CHEST_COLOR("advanced_alchemical_chest", "color"),
        ADVANCED_ALCHEMICAL_CHEST_TOOLTIP("advanced_alchemical_chest", "tooltip"),

        // Collector
        COLLECTOR_TOOLTIP("collector", "tooltip"),
        COLLECTOR_EMC("collector", "emc"),

        // EMC Link
        EMC_LINK_TOOLTIP("emc_link", "tooltip"),
        EMC_LINK_LIMIT_ITEMS("emc_link", "limit_items"),
        EMC_LINK_LIMIT_FLUIDS("emc_link", "limit_fluids"),
        EMC_LINK_FLUID_EXPORT_EFFICIENCY("emc_link", "fluid_export_efficiency"),
        EMC_LINK_LIMIT_EMC("emc_link", "limit_emc"),
        EMC_LINK_NOT_SET("emc_link", "not_set"),
        EMC_LINK_ALREADY_SET("emc_link", "already_set"),
        EMC_LINK_EMPTY_HAND("emc_link", "empty_hand"),
        EMC_LINK_CLEARED("emc_link", "cleared"),
        EMC_LINK_NOT_ENOUGH_EMC("emc_link", "not_enough_emc"),
        EMC_LINK_SET("emc_link", "set"),
        EMC_LINK_NO_EMC_VALUE("emc_link", "no_emc_value"),
        EMC_LINK_NO_EXPORT_REMAINING("emc_link", "no_export_remaining"),

        // Power Flower
        POWER_FLOWER_TOOLTIP("power_flower", "tooltip"),
        POWER_FLOWER_EMC("power_flower", "emc"),

        // Relay
        RELAY_TOOLTIP("relay", "tooltip"),
        RELAY_BONUS("relay", "bonus"),
        RELAY_TRANSFER("relay", "transfer"),

        // Misc
        TRANSMUTATION_INTERFACE_TOOLTIP("transmutation_interface", "tooltip"),

        BASIC_COLLECTOR("basic_collector"),
        DARK_COLLECTOR("dark_collector"),
        RED_COLLECTOR("red_collector"),
        MAGENTA_COLLECTOR("magenta_collector"),
        PINK_COLLECTOR("pink_collector"),
        PURPLE_COLLECTOR("purple_collector"),
        VIOLET_COLLECTOR("violet_collector"),
        BLUE_COLLECTOR("blue_collector"),
        CYAN_COLLECTOR("cyan_collector"),
        GREEN_COLLECTOR("green_collector"),
        LIME_COLLECTOR("lime_collector"),
        YELLOW_COLLECTOR("yellow_collector"),
        ORANGE_COLLECTOR("orange_collector"),
        WHITE_COLLECTOR("white_collector"),
        FADING_COLLECTOR("fading_collector"),
        FINAL_COLLECTOR("final_collector"),
        ;

        private String key;
        Blocks(String... path) {
            this.key = net.minecraft.util.Util.makeDescriptionId("block", Main.rl(String.join(".", path)));
        }

        @Override
        public String getTranslationKey() {
            return key;
        }

        public IFormattableTextComponent extend(String extension, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translate(args);
            this.key = originalKey;
            return component;
        }

        public IFormattableTextComponent extendColored(String extension, TextFormatting color, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translateColored(color, args);
            this.key = originalKey;
            return component;
        }
    }

    public enum Commands implements ILangEntry {
        // EMC
        EMC_INVALID("emc", "invalid"),
        EMC_ADD_SUCCESS("emc", "add", "success"),
        EMC_ADD_SUCCESS_SELF("emc", "add", "success_self"),
        EMC_ADD_NOTIFICATION("emc", "add", "notification"),
        EMC_REMOVE_SUCCESS("emc", "remove", "success"),
        EMC_REMOVE_SUCCESS_SELF("emc", "remove", "success_self"),
        EMC_REMOVE_NOTIFICATION("emc", "remove", "notification"),
        EMC_REMOVE_NEGATIVE("emc", "remove", "negative"),
        EMC_SET_SUCCESS("emc", "set", "success"),
        EMC_SET_SUCCESS_SELF("emc", "set", "success_self"),
        EMC_SET_NOTIFICATION("emc", "set", "notification"),
        EMC_GET_SUCCESS("emc", "get", "success"),
        EMC_GET_SUCCESS_SELF("emc", "get", "success_self"),
        EMC_TEST_SUCCESS("emc", "test", "success"),
        EMC_TEST_SUCCESS_SELF("emc", "test", "success_self"),
        EMC_TEST_FAIL("emc", "test", "fail"),
        EMC_TEST_FAIL_SELF("emc", "test", "fail_self"),

        // Knowledge
        KNOWLEDGE_INVALID("knowledge", "invalid"),
        KNOWLEDGE_CLEAR_SUCCESS("knowledge", "clear", "success"),
        KNOWLEDGE_CLEAR_SUCCESS_SELF("knowledge", "clear", "success_self"),
        KNOWLEDGE_CLEAR_FAIL("knowledge", "clear", "fail"),
        KNOWLEDGE_CLEAR_FAIL_SELF("knowledge", "clear", "fail_self"),
        KNOWLEDGE_CLEAR_NOTIFICATION("knowledge", "clear", "notification"),
        KNOWLEDGE_LEARN_SUCCESS("knowledge", "learn", "success"),
        KNOWLEDGE_LEARN_SUCCESS_SELF("knowledge", "learn", "success_self"),
        KNOWLEDGE_LEARN_FAIL("knowledge", "learn", "fail"),
        KNOWLEDGE_LEARN_FAIL_SELF("knowledge", "learn", "fail_self"),
        KNOWLEDGE_LEARN_NOTIFICATION("knowledge", "learn", "notification"),
        KNOWLEDGE_UNLEARN_SUCCESS("knowledge", "unlearn", "success"),
        KNOWLEDGE_UNLEARN_SUCCESS_SELF("knowledge", "unlearn", "success_self"),
        KNOWLEDGE_UNLEARN_FAIL("knowledge", "unlearn", "fail"),
        KNOWLEDGE_UNLEARN_FAIL_SELF("knowledge", "unlearn", "fail_self"),
        KNOWLEDGE_UNLEARN_NOTIFICATION("knowledge", "unlearn", "notification"),
        KNOWLEDGE_TEST_SUCCESS("knowledge", "test", "success"),
        KNOWLEDGE_TEST_SUCCESS_SELF("knowledge", "test", "success_self"),
        KNOWLEDGE_TEST_FAIL("knowledge", "test", "fail"),
        KNOWLEDGE_TEST_FAIL_SELF("knowledge", "test", "fail_self"),

        // Book
        BOOK_INVALID_HAND_ITEM("book", "invalid_hand_item"),
        BOOK_FAILED_TO_GET_CAPABILITY("book", "failed_to_get_capability"),
        BOOK_EMPTY("book", "empty"),
        BOOK_CLICK_TO_COPY("book", "click_to_copy"),
        BOOK_BOUND_TO_PLAYER("book", "bound_to_player"),
        BOOK_LIST_LOCATION("book", "list", "location"),
        BOOK_CLEAR_ITEMSTACK_SUCCESS("book", "clear", "itemstack_success"),
        BOOK_CLEAR_PLAYER_SUCCESS("book", "clear", "player_success"),
        BOOK_CLEAR_PLAYER_SUCCESS_SELF("book", "clear", "player_success_self"),
        BOOK_CLEAR_PLAYER_NOTIFICATION("book", "clear", "player_notification"),
        BOOK_REMOVE_INVALID_LOCATION("book", "remove", "invalid_location"),
        BOOK_REMOVE_INTERNAL_LOCATION("book", "remove", "internal_location"),
        BOOK_REMOVE_ITEMSTACK_SUCCESS("book", "remove", "itemstack_success"),
        BOOK_REMOVE_PLAYER_SUCCESS("book", "remove", "player_success"),
        BOOK_REMOVE_PLAYER_SUCCESS_SELF("book", "remove", "player_success_self"),
        BOOK_REMOVE_PLAYER_NOTIFICATION("book", "remove", "player_notification"),
        BOOK_REMOVE_BACKUP("book", "remove", "backup"),
        BOOK_REMOVE_BACKUP_INFO("book", "remove", "backup_info"),
        BOOK_ADD_DUPLICATE_NAME("book", "add", "duplicate_name"),
        BOOK_ADD_INVALID_NAME("book", "add", "invalid_name"),
        BOOK_ADD_ITEMSTACK_SUCCESS("book", "add", "itemstack_success"),
        BOOK_ADD_PLAYER_SUCCESS("book", "add", "player_success"),
        BOOK_ADD_PLAYER_SUCCESS_SELF("book", "add", "player_success_self"),
        BOOK_ADD_PLAYER_NOTIFICATION("book", "add", "player_notification"),

        // Misc
        CONSOLE("console"),
        PLAYER_ONLY("player_only"),
        ;

        private String key;
        Commands(String... path) {
            this.key = net.minecraft.util.Util.makeDescriptionId("command", Main.rl(String.join(".", path)));
        }

        @Override
        public String getTranslationKey() {
            return key;
        }

        public IFormattableTextComponent extend(String extension, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translate(args);
            this.key = originalKey;
            return component;
        }

        public IFormattableTextComponent extendColored(String extension, TextFormatting color, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translateColored(color, args);
            this.key = originalKey;
            return component;
        }
    }

    public enum Items implements ILangEntry {
        // Misc
        COMRESSED_COLLECTOR_TOOLTIP("compressed_collector", "tooltip"),
        ALCHEMICAL_BOOK_TOOLTIP("alchemical_book", "tooltip"),
        ALCHEMICAL_BOOK_TOOLTIP_BASIC("alchemical_book", "tooltip_basic"),
        ALCHEMICAL_BOOK_TOOLTIP_ADVANCED("alchemical_book", "tooltip_advanced"),
        ALCHEMICAL_BOOK_TOOLTIP_MASTER("alchemical_book", "tooltip_master"),
        ALCHEMICAL_BOOK_TOOLTIP_ARCANE("alchemical_book", "tooltip_arcane"),
        ALCHEMICAL_BOOK_TOOLTIP_ACROSS_DIMENSIONS("alchemical_book", "tooltip_across_dimensions"),
        ALCHEMICAL_BOOK_TOOLTIP_BIND("alchemical_book", "tooltip_bind"),
        ALCHEMICAL_BOOK_BOUND_TO("alchemical_book", "bound_to"),
        ALCHEMICAL_BOOK_OWNER_NOT_ONLINE("alchemical_book", "owner_not_online"),

        // Matter Upgrader
        MATTER_UPGRADER_TOOLTIP("matter_upgrader", "tooltip"),
        MATTER_UPGRADER_TOOLTIP2("matter_upgrader", "tooltip2"),
        MATTER_UPGRADER_TOOLTIP_CREATIVE("matter_upgrader", "tooltip_creative"),
        MATTER_UPGRADER_NOT_OWNER("matter_upgrader", "not_owner"),
        MATTER_UPGRADER_MAX_UPGRADE("matter_upgrader", "max_upgrade"),
        MATTER_UPGRADER_NOT_LEARNED("matter_upgrader", "not_learned"),
        MATTER_UPGRADER_NOT_ENOUGH_EMC("matter_upgrader", "not_enough_emc"),
        MATTER_UPGRADER_DONE("matter_upgrader", "done"),
        MATTER_UPGRADER_DONE_CREATIVE("matter_upgrader", "done_creative"),

        // Misc
        FINAL_STAR_SHARD_TOOLTIP("final_star_shard", "tooltip"),
        FINAL_STAR_TOOLTIP("final_star", "tooltip"),
        INFINITE_FUEL_TOOLTIP("infinite_fuel", "tooltip"),
        INFINITE_FUEL_NOT_ENOUGH_EMC("infinite_fuel", "not_enough_emc"),
        INFINITE_STEAK_TOOLTIP("infinite_steak", "tooltip"),
        INFINITE_STEAK_NOT_ENOUGH_EMC("infinite_steak", "not_enough_emc"),
        KNOWLEDGE_SHARING_BOOK_SELECTED("knowledge_sharing_book", "selected"),
        KNOWLEDGE_SHARING_BOOK_STORED("knowledge_sharing_book", "stored"),
        KNOWLEDGE_SHARING_BOOK_SELF("knowledge_sharing_book", "self"),
        KNOWLEDGE_SHARING_BOOK_LEARNED("knowledge_sharing_book", "learned"),
        KNOWLEDGE_SHARING_BOOK_LEARNED_OVER_100("knowledge_sharing_book", "learned_over_100"),
        KNOWLEDGE_SHARING_BOOK_LEARNED_TOTAL("knowledge_sharing_book", "learned_total"),
        KNOWLEDGE_SHARING_BOOK_NO_NEW_KNOWLEDGE("knowledge_sharing_book", "no_new_knowledge"),
        KNOWLEDGE_SHARING_BOOK_NO_OWNER("knowledge_sharing_book", "no_owner"),

        // Alchemical Book
        ALCHEMICAL_BOOK_ERROR("alchemical_book", "error"),
        ALCHEMICAL_BOOK_CREATE_FAILED("alchemical_book", "create_failed"),
        ALCHEMICAL_BOOK_DELETE_FAILED("alchemical_book", "delete_failed"),
        ALCHEMICAL_BOOK_TELEPORT_FAILED("alchemical_book", "teleport_failed"),
        ALCHEMICAL_BOOK_NO_LONGER_BOUND("alchemical_book", "no_longer_bound"),
        ALCHEMICAL_BOOK_NOW_BOUND("alchemical_book", "now_bound"),
        ALCHEMICAL_BOOK_CORRUPTED("alchemical_book", "corrupted"),
        ALCHEMICAL_BOOK_NOT_ENOUGH_EMC("alchemical_book", "not_enough_emc"),
        ;

        private String key;

        Items(String... path) {
            this.key = net.minecraft.util.Util.makeDescriptionId("item", Main.rl(String.join(".", path)));
        }

        @Override
        public String getTranslationKey() {
            return key;
        }

        public IFormattableTextComponent extend(String extension, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translate(args);
            this.key = originalKey;
            return component;
        }

        public IFormattableTextComponent extendColored(String extension, TextFormatting color, Object... args) {
            String originalKey = this.key;
            this.key += "." + extension;
            IFormattableTextComponent component = translateColored(color, args);
            this.key = originalKey;
            return component;
        }
    }
}
