package cool.furry.mc.forge.projectexpansion.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec.Builder Builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec Spec;
    public static final ForgeConfigSpec.ConfigValue<Integer> tickDelay = Builder.translation("gui.projectexpansion.config.tick_delay.desc").defineInRange("tickDelay", 20, 1, 200);
    public static final ForgeConfigSpec.ConfigValue<Boolean> formatEMC = Builder.translation("gui.projectexpansion.config.format_emc.desc").define("formatEMC", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> fullNumberNames = Builder.translation("gui.projectexpansion.config.full_number_names.desc").define("fullNumberNames", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> emcDisplay = Builder.translation("gui.projectexpansion.config.emc_display.desc").define("emcDisplay", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> notifyCommandChanges = Builder.translation("gui.projectexpansion.config.notify_command_changes.desc").define("notifyCommandChanges", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> notifyKnowledgeBookGains = Builder.translation("gui.projectexpansion.config.notify_knowledge_book_gains.desc").define("notifyKnowledgeBookGains", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> limitEmcLinkVendor = Builder.translation("gui.projectexpansion.config.limit_emc_link_vendor.desc").define("limitEmcLinkVendor", true);
    public static final ForgeConfigSpec.ConfigValue<Integer> transmutationInterfaceItemCount = Builder.translation("gui.projectexpansion.config.transmutation_interface_item_count.desc").defineInRange("transmutationInterfaceItemCount", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Double> collectorMultiplier = Builder.translation("gui.projectexpansion.config.collector_multiplier.desc").defineInRange("collectorMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkItemLimitMultiplier = Builder.translation("gui.projectexpansion.config.emc_link_item_limit_multiplier.desc").defineInRange("emcLinkItemLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkFluidLimitMultiplier = Builder.translation("gui.projectexpansion.config.emc_link_fluid_limit_multiplier.desc").defineInRange("emcLinkFluidLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkEMCLimitMultiplier = Builder.translation("gui.projectexpansion.config.emc_link_emc_limit_multiplier.desc").defineInRange("emcLinkEMCLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> powerflowerMultiplier = Builder.translation("gui.projectexpansion.config.powerflower_multiplier.desc").defineInRange("powerflowerMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> relayBonusMultiplier = Builder.translation("gui.projectexpansion.config.relay_bonus_multiplier.desc").defineInRange("relayBonusMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> relayTransferMultiplier = Builder.translation("gui.projectexpansion.config.relay_transfer_multiplier.desc").defineInRange("relayTransferMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteFuelCost = Builder.translation("gui.projectexpansion.config.infinite_fuel_cost.desc").defineInRange("infiniteFuelCost", 128, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteFuelBurnTime = Builder.translation("gui.projectexpansion.config.infinite_fuel_burn_time.desc").defineInRange("infiniteFuelBurnTime", 1600, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteSteakCost = Builder.translation("gui.projectexpansion.config.infinite_steak_cost.desc").defineInRange("infiniteSteakCost", 64, 1, Integer.MAX_VALUE);
    static { Spec = Builder.build(); }
}