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
        assert this.minecraft != null;
        this.optionsRowList = new OptionsRowList(this.minecraft, this.width, this.height, OPTIONS_LIST_TOP_HEIGHT, this.height - OPTIONS_LIST_BOTTOM_OFFSET, OPTIONS_LIST_ITEM_HEIGHT);
        this.children.add(this.optionsRowList);

        // I want to go back to 1.16
        this.optionsRowList.func_214333_a(new SliderPercentageOption(
                "gui.projectexpansion.config.tick_delay",
                1.0, 200.0,
                1.0F,
                __ -> (double) Config.tickDelay.get(),
                (__, newValue) -> Config.tickDelay.set(newValue.intValue()),
                // I don't know if this is how I should be doing this, but it works
                (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.tick_delay"), option.get(gs))
        ));

        this.optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.format_emc",
                __ -> Config.formatEMC.get(),
                (__, newValue) -> Config.formatEMC.set(newValue)
        ));

        this.optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.full_number_names",
                __ -> Config.formatEMC.get() && Config.fullNumberNames.get(),
                (__, newValue) -> Config.fullNumberNames.set(newValue)
        ));

        this.optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.emc_display",
                __ -> Config.formatEMC.get() && Config.emcDisplay.get(),
                (__, newValue) -> Config.emcDisplay.set(newValue)
        ));

        this.optionsRowList.func_214333_a(new SliderPercentageOption(
                "gui.projectexpansion.config.powerflower_multiplier",
                1, 20, 1,
                __ -> (double) Config.powerflowerMultiplier.get(),
                (__, newValue) -> Config.powerflowerMultiplier.set(newValue.intValue()),
                (gs, option) -> String.format("%s: %s", I18n.format("gui.projectexpansion.config.powerflower_multiplier"), option.get(gs))
        ));

        this.optionsRowList.func_214333_a(new BooleanOption(
                "gui.projectexpansion.config.notify_emc_changes",
                __ -> Config.notifyEMCChanges.get(),
                (__, newValue) -> Config.notifyEMCChanges.set(newValue)
        ));

        this.addButton(new Button((this.width - BUTTON_WIDTH) / 2, this.height - DONE_BUTTON_TOP_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("gui.done"), button -> this.minecraft.displayGuiScreen(parentScreen)
        ));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        this.optionsRowList.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);
        super.render(mouseX, mouseY, partialTicks);
    }
}