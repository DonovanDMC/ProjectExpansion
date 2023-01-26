package cool.furry.mc.forge.projectexpansion.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec.Builder Builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec Spec;
    public static final ForgeConfigSpec.ConfigValue<Integer> tickDelay = Builder.comment("The delay between mod operations (in ticks, default 20) - this will slightly effect the amount of emc generated via rounding - increase if you're noticing lag").defineInRange("tickDelay", 20, 1, 200);
    public static final ForgeConfigSpec.ConfigValue<Boolean> formatEMC = Builder.comment("If EMC should be formatted as M/B/T/etc").define("formatEMC", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> fullNumberNames = Builder.comment("If full number names (Million/Billion/Trillion) should be used instead of abbreviations").define("fullNumberNames", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> emcDisplay = Builder.comment("Displays your current emc and gained emc per second in the top left corner.").define("emcDisplay", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> notifyCommandChanges = Builder.comment("Notify users something is changed about them via /emc.").define("notifyCommandChanges", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> notifyKnowledgeBookGains = Builder.comment("Tell users the list of items they gained when using a knowledge book.").define("notifyKnowledgeBookGains", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> limitEmcLinkVendor = Builder.comment("If EMC Link Right-Click functionality should be Limited by Tier or Not.").define("limitEmcLinkVendor", true);
    public static final ForgeConfigSpec.ConfigValue<Boolean> enableFluidEfficiency = Builder.comment("If fluid efficiency should be enabled.").define("enableFluidEfficiency", true);
    public static final ForgeConfigSpec.ConfigValue<Integer> transmutationInterfaceItemCount = Builder.comment("The amount of items that the transmutation interface will report to have. Depending on your usage, you may want this to be a high value.").defineInRange("transmutationInterfaceItemCount", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Double> collectorMultiplier = Builder.comment("Multiplies the output of Collectors.").defineInRange("collectorMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkItemLimitMultiplier = Builder.comment("Multiplies the item limit of EMC Links.").defineInRange("emcLinkItemLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkFluidLimitMultiplier = Builder.comment("Multiplies the fluid limit of EMC Links.").defineInRange("emcLinkFluidLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> emcLinkEMCLimitMultiplier = Builder.comment("Multiplies the emc limit of EMC Links.").defineInRange("emcLinkEMCLimitMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> powerflowerMultiplier = Builder.comment("Multiplies the output of Power Flowers.").defineInRange("powerflowerMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> relayBonusMultiplier = Builder.comment("Multiplies the bonus of Relays.").defineInRange("relayBonusMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Double> relayTransferMultiplier = Builder.comment("Multiplies the transfer limit of Relays.").defineInRange("relayTransferMultiplier", 1.0D, 0.1D, 50D);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteFuelCost = Builder.comment("The cost of using the infinite fuel item.").defineInRange("infiniteFuelCost", 128, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteFuelBurnTime = Builder.comment("The ticks each usage of the infinite fuel item will give.").defineInRange("infiniteFuelBurnTime", 1600, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<Integer> infiniteSteakCost = Builder.comment("The cost of using the infinite steak item.").defineInRange("infiniteSteakCost", 64, 1, Integer.MAX_VALUE);
    static { Spec = Builder.build(); }
}