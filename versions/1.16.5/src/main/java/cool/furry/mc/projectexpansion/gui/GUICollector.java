package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.gui.container.ContainerCollector;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import moze_intel.projecte.utils.Constants;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class GUICollector<T extends ContainerCollector> extends PEContainerScreen<T> {

    public GUICollector(T container, PlayerInventory invPlayer, ITextComponent title) {
        super(container, invPlayer, title);
    }

    protected abstract ResourceLocation getTexture();

    protected int getBonusXShift() {
        return 0;
    }

    protected int getTextureBonusXShift() {
        return 0;
    }

    @Override
    protected void renderLabels(MatrixStack poseStack, int x, int y) {
        renderLabels(new GuiGraphics(poseStack, this), x, y);
    }

    protected void renderLabels(GuiGraphics graphics, int x, int y) {
        //Don't render title or inventory as we don't have space
        graphics.drawString(font, EMCFormat.format(menu.emc.get()), 60 + getBonusXShift(), 32, 0x404040, false);
        long kleinCharge = menu.kleinEmc.get();
        if (kleinCharge > 0) {
            graphics.drawString(font, Constants.EMC_FORMATTER.format(kleinCharge), 60 + getBonusXShift(), 44, 0x404040, false);
        }
    }

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTicks, int x, int y) {
        renderBg(new GuiGraphics(poseStack, this), partialTicks, x, y);
    }

    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);

        graphics.blit(getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);

        //Light Level. Max is 12
        int progress = (int) (menu.sunLevel.get() * 12.0 / 16);
        graphics.blit(getTexture(), leftPos + 126 + getBonusXShift(), topPos + 49 - progress, 177 + getTextureBonusXShift(), 13 - progress, 12, progress);

        //EMC storage. Max is 48
        graphics.blit(getTexture(), leftPos + 64 + getBonusXShift(), topPos + 18, 0, 166, (int) (new BigDecimal(menu.emc.get()).divide(new BigDecimal(menu.collector.getMaximumEmcBigInteger()), 3, RoundingMode.HALF_DOWN).multiply(BigDecimal.valueOf(48)).doubleValue()), 10);

        //Klein Star Charge Progress. Max is 48
        progress = (int) (menu.getKleinChargeProgress() * 48);
        graphics.blit(getTexture(), leftPos + 64 + getBonusXShift(), topPos + 58, 0, 166, progress, 10);

        //Fuel Progress. Max is 24.
        progress = (int) (menu.getFuelProgress() * 24);
        graphics.blit(getTexture(), leftPos + 138 + getBonusXShift(), topPos + 55 - progress, 176 + getTextureBonusXShift(), 38 - progress, 10, progress + 1);
    }

    public static class Tier1 extends GUICollector<ContainerCollector> {

        public Tier1(ContainerCollector container, PlayerInventory invPlayer, ITextComponent title) {
            super(container, invPlayer, title);
        }

        @Override
        protected ResourceLocation getTexture() {
            return PECore.rl("textures/gui/collector1.png");
        }
    }

    public static class Tier2 extends GUICollector<ContainerCollector> {

        public Tier2(ContainerCollector container, PlayerInventory invPlayer, ITextComponent title) {
            super(container, invPlayer, title);
            this.imageWidth = 200;
            this.imageHeight = 165;
        }

        @Override
        protected ResourceLocation getTexture() {
            return PECore.rl("textures/gui/collector2.png");
        }

        @Override
        protected int getBonusXShift() {
            return 16;
        }

        @Override
        protected int getTextureBonusXShift() {
            return 25;
        }
    }

    public static class Tier3 extends GUICollector<ContainerCollector> {

        public Tier3(ContainerCollector container, PlayerInventory invPlayer, ITextComponent title) {
            super(container, invPlayer, title);
            this.imageWidth = 218;
            this.imageHeight = 165;
        }

        @Override
        protected ResourceLocation getTexture() {
            return PECore.rl("textures/gui/collector3.png");
        }

        @Override
        protected int getBonusXShift() {
            return 34;
        }

        @Override
        protected int getTextureBonusXShift() {
            return 43;
        }
    }
}