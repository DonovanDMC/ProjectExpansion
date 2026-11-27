package cool.furry.mc.projectexpansion.wiki.images;

import cool.furry.mc.projectexpansion.wiki.Advancement;
import cool.furry.mc.projectexpansion.wiki.WikiImage;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Advancements {
    private Advancements() {}

    /******************************
     * Advanced Alchemical Chests *
     ******************************/

    @WikiImage("Advanced-Alchemical-Chest/advancement.png")
    @Advancement("advanced_alchemical_chest")
    public static void advancedAlchemicalChestAdvancement() {}

    /********************
     * Alchemical Books *
     ********************/

    @WikiImage("Alchemical-Book/advancements.png")
    @Advancement({ "basic_alchemical_book", "advanced_alchemical_book", "master_alchemical_book", "arcane_alchemical_book" })
    public static void alchemicalBookAdvancements() {}

    /*******************************
     * Arcane Transmutation Tablet *
     *******************************/

    @WikiImage("Arcane-Transmutation-Tablet/advancements.png")
    @Advancement({ "arcane_transmutation_tablet", "arcane_transmutation_table" })
    public static void arcaneTransmutationTabletAdvancements() {}

    /**************
     * Collectors *
     **************/

    @WikiImage("Collectors/advancements.png")
    @Advancement({ "collector", "final_collector", "compressed_collector", "final_compressed_collector" })
    public static void collectorAdvancements() {}

    /***************
     * Compact Sun *
     ***************/

    @WikiImage("Compact-Sun/advancements.png")
    @Advancement({ "compact_sun", "blinded_by_the_light" })
    public static void compactSunAdvancements() {}

    /*****************
     * Condenser MK3 *
     *****************/

    @WikiImage("Condenser-MK3/advancement.png")
    @Advancement("condenser_mk3")
    public static void condenserMK3Advancement() {}

    /*************
     * EMC Links *
     *************/

    @WikiImage("EMC-Link/advancements.png")
    @Advancement({ "emc_link", "final_emc_link" })
    public static void emcLinkAdvancements() {}

    /*************
     * EMC Pipes *
     *************/

    @WikiImage("EMC-Pipes/advancements.png")
    @Advancement({ "emc_pipe", "pipe_upgrade", "infinity_pipe_upgrade", "wrench" })
    public static void emcPipeAdvancements() {}

    /****************
     * EMC Refinery *
     ****************/

    @WikiImage("EMC-Refinery/advancement.png")
    @Advancement("emc_refinery")
    public static void emcRefineryAdvancement() {}

    /***************
     * EMC Storage *
     ***************/

    @WikiImage("EMC-Storage/advancement.png")
    @Advancement("emc_storage")
    public static void emcStorageAdvancement() {}

    /************
     * Ianthium *
     ************/

    @WikiImage("Ianthium/advancements.png")
    @Advancement({ "ianthium", "ianthium_ingot" })
    public static void ianthiumAdvancements() {}

    /**************************
     * Knowledge Sharing Book *
     **************************/

    @WikiImage("Knowledge-Sharing-Book/advancement.png")
    @Advancement("knowledge_sharing_book")
    public static void knowledgeSharingBookAdvancement() {}

    /*****************
     * Matter & Fuel *
     *****************/

    // each matter beside its block
    @WikiImage("Matter-and-Fuel/matter-advancements.png")
    @Advancement(columns = 2, value = {
            "magenta_matter", "magenta_matter_block",
            "pink_matter", "pink_matter_block",
            "purple_matter", "purple_matter_block",
            "violet_matter", "violet_matter_block",
            "blue_matter", "blue_matter_block",
            "cyan_matter", "cyan_matter_block",
            "green_matter", "green_matter_block",
            "lime_matter", "lime_matter_block",
            "yellow_matter", "yellow_matter_block",
            "orange_matter", "orange_matter_block",
            "white_matter", "white_matter_block",
            "fading_matter", "fading_matter_block"
    })
    public static void matterAdvancements() {}

    // each fuel beside its block
    @WikiImage("Matter-and-Fuel/fuel-advancements.png")
    @Advancement(columns = 2, value = {
            "magenta_fuel", "magenta_fuel_block",
            "pink_fuel", "pink_fuel_block",
            "purple_fuel", "purple_fuel_block",
            "violet_fuel", "violet_fuel_block",
            "blue_fuel", "blue_fuel_block",
            "cyan_fuel", "cyan_fuel_block",
            "green_fuel", "green_fuel_block",
            "lime_fuel", "lime_fuel_block",
            "yellow_fuel", "yellow_fuel_block",
            "orange_fuel", "orange_fuel_block",
            "white_fuel", "white_fuel_block"
    })
    public static void fuelAdvancements() {}

    /*****************
     * Power Flowers *
     *****************/

    @WikiImage("Power-Flowers/advancements.png")
    @Advancement({ "power_flower", "final_power_flower" })
    public static void powerFlowerAdvancements() {}

    /**********
     * Relays *
     **********/

    @WikiImage("Relays/advancements.png")
    @Advancement({ "relay", "final_relay" })
    public static void relayAdvancements() {}

    /*********
     * Stars *
     *********/

    @WikiImage("Stars/advancements.png")
    @Advancement({ "magnum_star_omega", "colossal_star_omega", "final_star_shard", "final_star" })
    public static void starAdvancements() {}

    /***************************
     * Transmutation Interface *
     ***************************/

    @WikiImage("Transmutation-Interface/advancement.png")
    @Advancement("transmutation_interface")
    public static void transmutationInterfaceAdvancement() {}
}
