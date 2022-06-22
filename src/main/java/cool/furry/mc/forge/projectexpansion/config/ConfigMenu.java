package cool.furry.mc.forge.projectexpansion.config;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ConfigMenu extends Screen {
    private static final int TITLE_HEIGHT = 8;
    private static final int OPTIONS_LIST_TOP_HEIGHT = 24;
    private static final int OPTIONS_LIST_BOTTOM_OFFSET = 32;
    private static final int OPTIONS_LIST_ITEM_HEIGHT = 25;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DONE_BUTTON_TOP_OFFSET = 26;

    private OptionsRowList optionsRowList;
    private final Screen parentScreen;

    public ConfigMenu(Screen parentScreen) {
        super(new TranslationTextComponent("gui.projectexpansion.config.title", Main.MOD_ID));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        optionsRowList = new OptionsRowList(Objects.requireNonNull(minecraft), width, height, OPTIONS_LIST_TOP_HEIGHT, height - OPTIONS_LIST_BOTTOM_OFFSET, OPTIONS_LIST_ITEM_HEIGHT);
        children.add(optionsRowList);

        // I want to go back to 1.16
        optionsRowList.func_214333_a(new SliderPercentageOption(
                "gui.projectexpansion.config.tick_delay",
                1.0, 200.0,
                1.0F,
                __ -> (double) Config.tickDelay.get(),
                (__, newValue) -> Config.tickDelay.set(newValue.intValue()),
                // I don't know if this is how I should be doing this, but it works
                (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.tick_delay"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.format_emc",
                __ -> Config.formatEMC.get(),
                (__, newValue) -> Config.formatEMC.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.full_number_names",
                __ -> Config.formatEMC.get() && Config.fullNumberNames.get(),
                (__, newValue) -> Config.fullNumberNames.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.emc_display",
                __ -> Config.formatEMC.get() && Config.emcDisplay.get(),
                (__, newValue) -> Config.emcDisplay.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.notify_command_changes",
                __ -> Config.notifyCommandChanges.get(),
                (__, newValue) -> Config.notifyCommandChanges.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.notify_knowledge_book_changes",
                __ -> Config.notifyKnowledgeBookGains.get(),
                (__, newValue) -> Config.notifyKnowledgeBookGains.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
            "gui.projectexpansion.config.limit_emc_link_vendor",
            __ -> Config.limitEmcLinkVendor.get(),
            (__, newValue) -> Config.limitEmcLinkVendor.set(newValue)
        ));

        optionsRowList.func_214333_a(new BooleanOption(
            "gui.projectexpansion.config.use_old_values",
            __ -> Config.useOldValues.get(),
            (__, newValue) -> Config.useOldValues.set(newValue)
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.collector_multiplier",
            1, 20, 1,
            __ -> (double) Config.collectorMultiplier.get(),
            (__, newValue) -> Config.collectorMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.collector_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_item_limit_multiplier",
            1, 20, 1,
            __ -> (double) Config.emcLinkItemLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkItemLimitMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.emc_link_item_limit_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_fluid_limit_multiplier",
            1, 20, 1,
            __ -> (double) Config.emcLinkFluidLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkFluidLimitMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.emc_link_fluid_limit_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_emc_limit_multiplier",
            1, 20, 1,
            __ -> (double) Config.emcLinkEMCLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkEMCLimitMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.emc_link_emc_limit_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.powerflower_multiplier",
            1, 20, 1,
            __ -> (double) Config.powerflowerMultiplier.get(),
            (__, newValue) -> Config.powerflowerMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.powerflower_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.relay_bonus_multiplier",
            1, 20, 1,
            __ -> (double) Config.relayBonusMultiplier.get(),
            (__, newValue) -> Config.relayBonusMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.relay_bonus_multiplier"), (int) option.get(gs))
        ));

        optionsRowList.func_214333_a(new SliderPercentageOption(
            "gui.projectexpansion.config.relay_transfer_multiplier",
            1, 20, 1,
            __ -> (double) Config.relayTransferMultiplier.get(),
            (__, newValue) -> Config.relayTransferMultiplier.set(newValue.intValue()),
            (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.relay_transfer_multiplier"), (int) option.get(gs))
        ));

        addButton(new Button((width - BUTTON_WIDTH) / 2, height - DONE_BUTTON_TOP_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("gui.done"), (button) -> minecraft.displayGuiScreen(parentScreen)));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        optionsRowList.render(mouseX, mouseY, partialTicks);
        drawCenteredString(font, title.getString(), width / 2, TITLE_HEIGHT, 0xFFFFFF);
        super.render(mouseX, mouseY, partialTicks);
    }
}