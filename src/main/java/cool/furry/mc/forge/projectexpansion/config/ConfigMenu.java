package cool.furry.mc.forge.projectexpansion.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
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
    private final Screen parentScreen;
    private OptionsRowList optionsRowList;

    public ConfigMenu(Screen parentScreen) {
        super(new TranslationTextComponent("gui.projectexpansion.config.title", Main.MOD_ID));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        optionsRowList = new OptionsRowList(Objects.requireNonNull(minecraft), width, height, OPTIONS_LIST_TOP_HEIGHT, height - OPTIONS_LIST_BOTTOM_OFFSET, OPTIONS_LIST_ITEM_HEIGHT);
        children.add(optionsRowList);

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.tick_delay",
            1.0, 200.0,
            1.0F,
            __ -> (double) Config.tickDelay.get(),
            (__, newValue) -> Config.tickDelay.set(newValue.intValue()),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.tick_delay").append(new StringTextComponent(String.format(": %s", option.get(gs))))
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.format_emc",
            __ -> Config.formatEMC.get(),
            (__, newValue) -> Config.formatEMC.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.full_number_names",
            __ -> Config.formatEMC.get() && Config.fullNumberNames.get(),
            (__, newValue) -> Config.fullNumberNames.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.emc_display",
            __ -> Config.formatEMC.get() && Config.emcDisplay.get(),
            (__, newValue) -> Config.emcDisplay.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.notify_command_changes",
            __ -> Config.notifyCommandChanges.get(),
            (__, newValue) -> Config.notifyCommandChanges.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.notify_knowledge_book_changes",
            __ -> Config.notifyKnowledgeBookGains.get(),
            (__, newValue) -> Config.notifyKnowledgeBookGains.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.limit_emc_link_vendor",
            __ -> Config.limitEmcLinkVendor.get(),
            (__, newValue) -> Config.limitEmcLinkVendor.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.use_old_values",
            __ -> Config.useOldValues.get(),
            (__, newValue) -> Config.useOldValues.set(newValue)
        ));

        optionsRowList.addBig(new BooleanOption(
            "gui.projectexpansion.config.enable_fluid_efficiency",
            __ -> Config.enableFluidEfficiency.get(),
            (__, newValue) -> Config.enableFluidEfficiency.set(newValue)
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.collector_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.collectorMultiplier.get(),
            (__, newValue) -> Config.collectorMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.collector_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_item_limit_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.emcLinkItemLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkItemLimitMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.emc_link_item_limit_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_fluid_limit_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.emcLinkFluidLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkFluidLimitMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.emc_link_fluid_limit_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.emc_link_emc_limit_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.emcLinkEMCLimitMultiplier.get(),
            (__, newValue) -> Config.emcLinkEMCLimitMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.emc_link_emc_limit_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.powerflower_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.powerflowerMultiplier.get(),
            (__, newValue) -> Config.powerflowerMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.powerflower_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.relay_bonus_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.relayBonusMultiplier.get(),
            (__, newValue) -> Config.relayBonusMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.relay_bonus_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        optionsRowList.addBig(new SliderPercentageOption(
            "gui.projectexpansion.config.relay_transfer_multiplier",
            0.1F, 20.0F, 0.1F,
            __ -> Config.relayTransferMultiplier.get(),
            (__, newValue) -> Config.relayTransferMultiplier.set(newValue),
            (gs, option) -> new TranslationTextComponent("gui.projectexpansion.config.relay_transfer_multiplier").append(new StringTextComponent(String.format(": %s", (int) option.get(gs))))
        ));

        addButton(new Button((width - BUTTON_WIDTH) / 2, height - DONE_BUTTON_TOP_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslationTextComponent("gui.done"), (button) -> minecraft.pushGuiLayer(parentScreen)));
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        optionsRowList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, font, title.getString(), width / 2, TITLE_HEIGHT, 0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}