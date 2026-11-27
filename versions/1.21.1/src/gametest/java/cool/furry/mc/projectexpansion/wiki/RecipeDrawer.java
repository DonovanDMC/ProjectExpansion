package cool.furry.mc.projectexpansion.wiki;

import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static cool.furry.mc.projectexpansion.wiki.WikiRenderer.GAP;
import static cool.furry.mc.projectexpansion.wiki.WikiRenderer.PAD;
import static cool.furry.mc.projectexpansion.wiki.WikiRenderer.SLOT;

// crafting & refining recipes, drawn on panels like a recipe viewer
final class RecipeDrawer {
    private static final int OUTPUT = 26;
    private static final int CRAFTING_WIDTH = PAD + SLOT * 3 + GAP + WikiRenderer.ARROW_WIDTH + GAP + OUTPUT + PAD, CRAFTING_HEIGHT = PAD + SLOT * 3 + PAD;
    private static final int REFINING_WIDTH = PAD + SLOT + GAP + WikiRenderer.ARROW_WIDTH + GAP + OUTPUT + PAD, REFINING_HEIGHT = PAD + OUTPUT + 2 + 9 + 3;

    private RecipeDrawer() {}

    // more than one are stacked, like the different ways to make magenta matter
    static void draw(GuiGraphics graphics, List<Recipe<?>> recipes) {
        int y = 2;
        for (Recipe<?> recipe : recipes) {
            if (recipe instanceof RefiningRecipe refining) {
                refining(graphics, 2, y, refining);
                y += REFINING_HEIGHT + 4;
            } else if (recipe instanceof CraftingRecipe crafting) {
                crafting(graphics, 2, y, crafting);
                y += CRAFTING_HEIGHT + 4;
            } else {
                throw new IllegalStateException("Can't draw " + recipe.getType());
            }
        }
    }

    // what a tag ingredient shows, the first item in it
    private static ItemStack shown(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    private static void crafting(GuiGraphics graphics, int x, int y, CraftingRecipe recipe) {
        WikiRenderer.panel(graphics, x, y, CRAFTING_WIDTH, CRAFTING_HEIGHT);
        int gridX = x + PAD, gridY = y + PAD;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) WikiRenderer.slot(graphics, gridX + column * SLOT, gridY + row * SLOT, SLOT);
        }
        // shaped recipes keep their shape, shapeless ones fill the grid in order
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            WikiRenderer.item(graphics, shown(ingredients.get(i)), gridX + (i % width) * SLOT + 1, gridY + (i / width) * SLOT + 1);
        }
        int arrowX = gridX + SLOT * 3 + GAP;
        WikiRenderer.arrow(graphics, arrowX, y + (CRAFTING_HEIGHT - WikiRenderer.ARROW_HEIGHT) / 2);
        int outputX = arrowX + WikiRenderer.ARROW_WIDTH + GAP, outputY = y + (CRAFTING_HEIGHT - OUTPUT) / 2;
        WikiRenderer.slot(graphics, outputX, outputY, OUTPUT);
        WikiRenderer.item(graphics, recipe.getResultItem(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess()), outputX + 5, outputY + 5);
    }

    private static void refining(GuiGraphics graphics, int x, int y, RefiningRecipe recipe) {
        WikiRenderer.panel(graphics, x, y, REFINING_WIDTH, REFINING_HEIGHT);
        int inputY = y + PAD + (OUTPUT - SLOT) / 2;
        WikiRenderer.slot(graphics, x + PAD, inputY, SLOT);
        WikiRenderer.item(graphics, shown(recipe.ingredient()).copyWithCount(recipe.count()), x + PAD + 1, inputY + 1);
        int arrowX = x + PAD + SLOT + GAP;
        WikiRenderer.arrow(graphics, arrowX, y + PAD + (OUTPUT - WikiRenderer.ARROW_HEIGHT) / 2);
        int outputX = arrowX + WikiRenderer.ARROW_WIDTH + GAP;
        WikiRenderer.slot(graphics, outputX, y + PAD, OUTPUT);
        WikiRenderer.item(graphics, recipe.result(), outputX + 5, y + PAD + 5);
        // the same text the refining category shows
        String text = String.format(Locale.ROOT, "%d EMC, %.1fs", recipe.emc(), recipe.time() / 20F);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, x + (REFINING_WIDTH - font.width(text)) / 2, y + PAD + OUTPUT + 2, 0xFF404040, false);
    }
}
