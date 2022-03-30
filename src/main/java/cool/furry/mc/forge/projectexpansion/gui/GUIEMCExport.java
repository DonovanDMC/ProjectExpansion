package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.ContainerEMCExport;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GUIEMCExport extends ContainerScreen<ContainerEMCExport> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/emc_export.png");

    public static final int Y_SIZE = 130;

    public GUIEMCExport(ContainerEMCExport container, PlayerInventory playerInventory, ITextComponent name) {
        super(container, playerInventory, name);
        ySize = Y_SIZE;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        // String title = new TranslationTextComponent("block.projectexpansion.emc_export").getString();
        // Minecraft.getInstance().fontRenderer.drawString(matrixStack, title, xSize / 2F - Minecraft.getInstance().fontRenderer.getStringWidth(title) / 2F, ySize - 218, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        getMinecraft().getTextureManager().bindTexture(TEXTURE);
        int xOffSet = (width - xSize) / 2;
        int yOffSet = (height - ySize) / 2;
        this.blit(xOffSet, yOffSet, 0, 0, xSize, ySize);
    }
}
