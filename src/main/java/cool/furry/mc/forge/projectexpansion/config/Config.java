package cool.furry.mc.forge.projectexpansion.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder Builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec Spec;
    public static final ForgeConfigSpec.ConfigValue<Integer> tickDelay = Builder.translation("gui.projectexpansion.config.tick_delay.desc").define("tickDelay", 20);
    public static final ForgeConfigSpec.ConfigValue<Boolean> formatEMC = Builder.translation("gui.projectexpansion.config.format_emc.desc").define("formatEMC", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> fullNumberNames = Builder.translation("gui.projectexpansion.config.full_number_names.desc").define("fullNumberNames", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> emcDisplay = Builder.translation("gui.projectexpansion.config.emc_display.desc").define("emcDisplay", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> notifyCommandChanges = Builder.translation("gui.projectexpansion.config.notify_command_changes.desc").define("notifyCommandChanges", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> limitEmcLinkVendor = Builder.translation("gui.projectexpansion.config.limit_emc_link_vendor.desc").define("limitEmcLinkVendor", true);
    // due to the large range of this (1 - Integer.MAX_VALUE, with all 2 billion values being valid), this option is excluded from the gui
    public static final ForgeConfigSpec.ConfigValue<Integer> transmutationInterfaceItemCount = Builder.translation("gui.projectexpansion.config.transmutation_interface_item_count.desc").define("transmutationInterfaceItemCount", 4096);
    public static final ForgeConfigSpec.ConfigValue<Boolean> useOldValues = Builder.translation("gui.projectexpansion.config.use_old_values.desc").define("useOldValues", false);
    public static final ForgeConfigSpec.ConfigValue<Long> collectorMultiplier = Builder.translation("gui.projectexpansion.config.collector_multiplier.desc").define("collectorMultiplier", 1L);
    public static final ForgeConfigSpec.ConfigValue<Integer> emcLinkItemLimitMultiplier = Builder.translation("gui.projectexpansion.config.emc_link_item_limit_multiplier.desc").define("emcLinkItemLimitMultiplier", 1);
    public static final ForgeConfigSpec.ConfigValue<Long> emcLinkEMCLimitMultiplier = Builder.translation("gui.projectexpansion.config.emc_link_emc_limit_multiplier.desc").define("emcLinkEMCLimitMultiplier", 1L);
    public static final ForgeConfigSpec.ConfigValue<Long> powerflowerMultiplier = Builder.translation("gui.projectexpansion.config.powerflower_multiplier.desc").define("powerflowerMultiplier", 1L);
    public static final ForgeConfigSpec.ConfigValue<Long> relayBonusMultiplier = Builder.translation("gui.projectexpansion.config.relay_bonus_multiplier.desc").define("relayBonusMultiplier", 1L);
    public static final ForgeConfigSpec.ConfigValue<Long> relayTransferMultiplier = Builder.translation("gui.projectexpansion.config.relay_transfer_multiplier.desc").define("relayTransferMultiplier", 1L);

    static {
        Spec = Builder.build();
    }
}
