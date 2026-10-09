package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

// Screen#setTooltipForNextRenderPass was added in 1.19.4, this draws the tooltip once the screen is done rendering in the same way
public class DeferredTooltip {
    @Nullable
    private Component tooltip;

    public void set(Component tooltip) {
        this.tooltip = tooltip;
    }

    public void render(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
        if (tooltip != null) {
            screen.renderTooltip(poseStack, tooltip, mouseX, mouseY);
            tooltip = null;
        }
    }
}
