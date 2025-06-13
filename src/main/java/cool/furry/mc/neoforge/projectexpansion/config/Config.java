package cool.furry.mc.neoforge.projectexpansion.config;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.EMCDisplay;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.Arrays;

public final class Config {
    public static Client client = new Client();
    public static Server server = new Server();
    public static final class Client {
        public final ModConfigSpec.Builder Builder = new ModConfigSpec.Builder();
        public final ModConfigSpec Spec;
        public final ModConfigSpec.ConfigValue<String> emcDisplayPosition = Builder.comment("The Position of the emc display. Allowed values: TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT").define("emcDisplayPosition", "TOPLEFT", (val) -> val != null && EMCDisplay.POSITIONS.contains(val));
        public final ModConfigSpec.ConfigValue<Boolean> formatEMC = Builder.comment("If EMC should be formatted as M/B/T/etc").define("formatEMC", true);
        public final ModConfigSpec.ConfigValue<Boolean> fullNumberNames = Builder.comment("If full number names (Million/Billion/Trillion) should be used instead of abbreviations").define("fullNumberNames", true);
        public final ModConfigSpec.ConfigValue<Boolean> emcDisplay = Builder.comment("Displays your current emc and gained emc per second in the top left corner.").define("emcDisplay", true);
        public final ModConfigSpec.ConfigValue<Boolean> enabledLearnedTooltip = Builder.comment("If a tooltip should be shown on items which can be learned, denoting if the item has been learned or not. Note: ProjectE's client.shiftEmcToolTips applies to this.").define("enabledLearnedTooltip", true);
        public final ModConfigSpec.ConfigValue<Boolean> alchemicalCollectionSound = Builder.comment("If a sound should be played when something is collected with Alchemical Collection.").define("alchemicalCollectionSound", true);
        private Client() { Spec = Builder.build(); }
    }
    public static final class Server {
        public final ModConfigSpec.Builder Builder = new ModConfigSpec.Builder();
        public final ModConfigSpec Spec;
        public final ModConfigSpec.ConfigValue<Integer> tickDelay = Builder.comment("The delay between mod operations (in ticks, default 20) - this will slightly effect the amount of emc generated via rounding - increase if you're noticing lag").defineInRange("tickDelay", 20, 1, 200);
        public final ModConfigSpec.ConfigValue<Boolean> notifyCommandChanges = Builder.comment("Notify users when something is changed about them via commands.").define("notifyCommandChanges", true);
        public final ModConfigSpec.ConfigValue<Boolean> notifyKnowledgeBookGains = Builder.comment("Tell users the list of items they gained when using a knowledge book.").define("notifyKnowledgeBookGains", true);
        public final ModConfigSpec.ConfigValue<Boolean> limitEmcLinkVendor = Builder.comment("If EMC Link Right-Click functionality should be Limited by Tier or Not.").define("limitEmcLinkVendor", true);
        public final ModConfigSpec.ConfigValue<Boolean> enableFluidEfficiency = Builder.comment("If fluid efficiency should be enabled.").define("enableFluidEfficiency", true);
        public final ModConfigSpec.ConfigValue<Integer> transmutationInterfaceItemCount = Builder.comment("The amount of items that the transmutation interface will report to have. Depending on your usage, you may want this to be a high value.").defineInRange("transmutationInterfaceItemCount", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        public final ModConfigSpec.ConfigValue<Double> collectorMultiplier = Builder.comment("Multiplies the output of Collectors.").defineInRange("collectorMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> emcLinkItemLimitMultiplier = Builder.comment("Multiplies the item limit of EMC Links.").defineInRange("emcLinkItemLimitMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> emcLinkFluidLimitMultiplier = Builder.comment("Multiplies the fluid limit of EMC Links.").defineInRange("emcLinkFluidLimitMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> emcLinkEMCLimitMultiplier = Builder.comment("Multiplies the emc limit of EMC Links.").defineInRange("emcLinkEMCLimitMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> powerflowerMultiplier = Builder.comment("Multiplies the output of Power Flowers.").defineInRange("powerflowerMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> relayBonusMultiplier = Builder.comment("Multiplies the bonus of Relays.").defineInRange("relayBonusMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Double> relayTransferMultiplier = Builder.comment("Multiplies the transfer limit of Relays.").defineInRange("relayTransferMultiplier", 1.0D, 0.1D, 50D);
        public final ModConfigSpec.ConfigValue<Integer> infiniteFuelCost = Builder.comment("The cost of using the infinite fuel item.").defineInRange("infiniteFuelCost", 128, 1, Integer.MAX_VALUE);
        public final ModConfigSpec.ConfigValue<Integer> infiniteFuelBurnTime = Builder.comment("The ticks each usage of the infinite fuel item will give.").defineInRange("infiniteFuelBurnTime", 1600, 1, Integer.MAX_VALUE);
        public final ModConfigSpec.ConfigValue<Integer> infiniteSteakCost = Builder.comment("The cost of using the infinite steak item.").defineInRange("infiniteSteakCost", 64, 1, Integer.MAX_VALUE);
        public final ModConfigSpec.ConfigValue<Boolean> persistEnchantedBooksOnly = Builder.comment("If ProjectE's processors.EnchantmentProcessor.persistent option should only include enchanted books.").define("persistEnchantedBooksOnly", false);
        private final ModConfigSpec.ConfigValue<String> editOthersAlchemicalBooks = Builder.comment("If players should be allowed to edit books bound to other players. A player is considered to be \"OP\" when they have an op level of 2 or greater. Allowed values: DISABLED, OP_ONLY, ENABLED").define("editOthersAlchemicalBooks", AlchemicalBookEditLevel.DISABLED.name(), (val) -> val != null && Arrays.stream(AlchemicalBookEditLevel.values()).toList().contains(val));
        public final ModConfigSpec.ConfigValue<Boolean> zeroEmcFluidsAreFree = Builder.comment("If fluids which end their calculations at zero emc should be returned as free.").define("zeroEmcFluidsAreFree", true);
        public final ModConfigSpec.ConfigValue<Boolean> enableCollectorOptimizations = Builder.comment("If optimizations (ticking only once per second) should be enabled for collectors. This will make them process at most one item each second.").define("enableCollectorOptimizations", false);
        public final ModConfigSpec.ConfigValue<Integer> compactSunBonus = Builder.comment("The bonus (multiplicative) the compact sun block should give. Set to 0 to disable.").define("compactSunBonus", 10);
        public final ModConfigSpec.ConfigValue<Boolean> sunMultiplierPriceCompensation = Builder.comment("Enable determining the sun bonus mutiplier via the difference in emc price between the final power flower and the compact sun block, rounded up to the next 10. In normal gameplay this is ~33x, so a 40x multiplier. If either block has no emc value or the multiplier is lower than compactSunBonus, that value will be used instead.").define("sunMultiplierPriceCompensation", true);
        private Server() { Spec = Builder.build(); }

        public AlchemicalBookEditLevel editOthersAlchemicalBooks() {
            try {
                return AlchemicalBookEditLevel.valueOf(editOthersAlchemicalBooks.get().toUpperCase());
            } catch (IllegalArgumentException ignore) {
                LogManager.getLogger(Config.class).printf(Level.WARN, "Invalid value for editOthersAlchemicalBooks: %. Defaulting to %s", editOthersAlchemicalBooks.get(), AlchemicalBookEditLevel.DISABLED.name());
                editOthersAlchemicalBooks.set(AlchemicalBookEditLevel.DISABLED.name());
                return AlchemicalBookEditLevel.DISABLED;
            }
        }
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, client.Spec, String.format("%s/client.toml", Main.MOD_ID));
        modContainer.registerConfig(ModConfig.Type.SERVER, server.Spec, String.format("%s/server.toml", Main.MOD_ID));
    }

    public enum AlchemicalBookEditLevel {
        DISABLED,
        OP_ONLY,
        ENABLED
    }
}