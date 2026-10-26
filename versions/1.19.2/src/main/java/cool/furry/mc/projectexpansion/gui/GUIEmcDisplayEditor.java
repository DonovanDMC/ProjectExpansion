package cool.furry.mc.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.EMCDisplay.EmcDisplayPosition;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class GUIEmcDisplayEditor extends Screen {
    private final @Nullable Screen parent;
    private boolean left;
    private boolean top;
    private int offsetX;
    private int offsetY;
    private boolean dragging = false;
    private int grabX;
    private int grabY;

    public GUIEmcDisplayEditor(@Nullable Screen parent) {
        super(Lang.GUI.EMC_DISPLAY_EDITOR.translate());
        this.parent = parent;
        EmcDisplayPosition position = Config.client.emcDisplayPosition.get();
        left = position.isLeft();
        top = position.isTop();
        offsetX = Config.client.emcDisplayOffsetX.get();
        offsetY = Config.client.emcDisplayOffsetY.get();
    }

    @Override
    protected void init() {
        int y = height / 2 + 10;
        addRenderableWidget(new Button(width / 2 - 154, y, 100, 20, Lang.GUI.EMC_DISPLAY_EDITOR_RESET.translate(), (button) -> reset()));
        addRenderableWidget(new Button(width / 2 - 50, y, 100, 20, CommonComponents.GUI_DONE, (button) -> save()));
        addRenderableWidget(new Button(width / 2 + 54, y, 100, 20, CommonComponents.GUI_CANCEL, (button) -> onClose()));
    }

    private int textWidth() {
        return font.width(EMCDisplay.getText());
    }

    private int textHeight() {
        return font.lineHeight;
    }

    private int x() {
        return EmcDisplayLayout.toScreen(left, offsetX, textWidth(), width);
    }

    private int y() {
        return EmcDisplayLayout.toScreen(top, offsetY, textHeight(), height);
    }

    private void moveTo(int x, int y) {
        int w = textWidth(), h = textHeight();
        x = EmcDisplayLayout.clamp(x, w, width);
        y = EmcDisplayLayout.clamp(y, h, height);
        left = EmcDisplayLayout.isStart(x, w, width);
        top = EmcDisplayLayout.isStart(y, h, height);
        offsetX = EmcDisplayLayout.toOffset(left, x, w, width);
        offsetY = EmcDisplayLayout.toOffset(top, y, h, height);
    }

    private boolean isOverText(double mouseX, double mouseY) {
        int x = x(), y = y();
        return mouseX >= x - 2 && mouseX < x + textWidth() + 2 && mouseY >= y - 2 && mouseY < y + textHeight() + 1;
    }

    private void reset() {
        left = top = true;
        offsetX = offsetY = EmcDisplayLayout.DEFAULT_OFFSET;
    }

    private void save() {
        Config.client.emcDisplayPosition.set(EmcDisplayPosition.of(top, left));
        Config.client.emcDisplayOffsetX.set(offsetX);
        Config.client.emcDisplayOffsetY.set(offsetY);
        Config.client.Spec.save();
        onClose();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        GuiGraphics guiGraphics = new GuiGraphics(poseStack, this);
        // lighter than the normal background, the display should be seen over the game like it normally is
        if (minecraft.level == null) renderBackground(poseStack);
        else guiGraphics.fill(0, 0, width, height, 0x40000000);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 30, 0xFFFFFF);
        drawCenteredString(poseStack, font, Lang.GUI.EMC_DISPLAY_EDITOR_HELP.translate(), width / 2, height / 2 - 16, 0xA0A0A0);

        String text = EMCDisplay.getText();
        int x = x(), y = y(), w = font.width(text), h = textHeight();
        if (dragging) {
            if (x == EmcDisplayLayout.center(w, width)) guiGraphics.fill(width / 2, 0, width / 2 + 1, height, 0x60FFFFFF);
            if (y == EmcDisplayLayout.center(h, height)) guiGraphics.fill(0, height / 2, width, height / 2 + 1, 0x60FFFFFF);
        }
        boolean highlight = dragging || isOverText(mouseX, mouseY);
        guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 1, highlight ? 0x60FFFFFF : 0x30FFFFFF);
        guiGraphics.renderOutline(x - 2, y - 2, w + 4, h + 3, highlight ? 0xFFFFFFFF : 0x80FFFFFF);
        guiGraphics.drawString(font, text, x, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // the display can be dropped over the buttons, so it gets picked up before they're clicked
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isOverText(mouseX, mouseY)) {
            dragging = true;
            grabX = (int) mouseX - x();
            grabY = (int) mouseY - y();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        int x = (int) mouseX - grabX, y = (int) mouseY - grabY;
        if (!hasShiftDown()) {
            x = EmcDisplayLayout.snapToCenter(x, textWidth(), width);
            y = EmcDisplayLayout.snapToCenter(y, textHeight(), height);
        }
        moveTo(x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = hasShiftDown() ? 10 : 1;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> moveTo(x() - step, y());
            case GLFW.GLFW_KEY_RIGHT -> moveTo(x() + step, y());
            case GLFW.GLFW_KEY_UP -> moveTo(x(), y() - step);
            case GLFW.GLFW_KEY_DOWN -> moveTo(x(), y() + step);
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
