package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.ContainerMatterReplicator;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ScreenMatterReplicator extends ContainerScreen<ContainerMatterReplicator> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/matter_replicator.png");
    public static final int Y_SIZE = 157;
    public static final int X_SIZE = 176;
    private final ContainerMatterReplicator container;

    public ScreenMatterReplicator(ContainerMatterReplicator container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        ySize = Y_SIZE;
        xSize = X_SIZE;
        this.container = container;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderHoveredTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).inventory.getItemStack().isEmpty()) return;
        super.renderHoveredTooltip(matrixStack, mouseX, mouseY);
        ArrayList<ITextComponent> hoveringText = new ArrayList<>();
        if (isPointInRegion(ContainerMatterReplicator.ARROW_X, ContainerMatterReplicator.ARROW_Y, ContainerMatterReplicator.ARROW_WIDTH, ContainerMatterReplicator.ARROW_HEIGHT, mouseX, mouseY)) {
            // normal translations weren't woring??
            hoveringText.add(new StringTextComponent(String.format("Progress: %s%%", (int) (container.percentageToUnlock() * 100))));
        }
        if (!hoveringText.isEmpty()) func_243308_b(matrixStack, hoveringText, mouseX, mouseY);
        else  super.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Objects.requireNonNull(this.minecraft).getTextureManager().bindTexture(TEXTURE);
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;
        this.blit(matrixStack, edgeSpacingX, edgeSpacingY, 0, 0, this.xSize, this.ySize);

        double unlockProgress = container.percentageToUnlock();
        if(this.container.isLocked()) this.blit(matrixStack, guiLeft + ContainerMatterReplicator.ARROW_X, guiTop + ContainerMatterReplicator.ARROW_Y, ContainerMatterReplicator.ARROW_FILLED_X, ContainerMatterReplicator.ARROW_FILLED_Y,
            ContainerMatterReplicator.ARROW_WIDTH, (int) (unlockProgress * ContainerMatterReplicator.ARROW_HEIGHT));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        // no titles
    }
}
