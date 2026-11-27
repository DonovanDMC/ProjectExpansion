package cool.furry.mc.projectexpansion.wiki;

import cool.furry.mc.projectexpansion.Main;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.RecipeFillButtonWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

// emi's recipe screen over a menu, with the mouse on a recipe's + button so its transfer tooltip shows
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class WikiEmi {
    // the menu it's waiting on, then the recipe to show over it
    private static @Nullable Class<? extends Screen> waitFor;
    private static @Nullable ResourceLocation recipe;
    private static int ticksLeft;

    private WikiEmi() {}

    // safe to call from the server, it's shown once the menu's open on the client
    public static void hoverFill(Class<? extends Screen> over, ResourceLocation id) {
        Minecraft.getInstance().execute(() -> {
            waitFor = over;
            recipe = id;
            ticksLeft = 200;
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (waitFor == null || recipe == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (!waitFor.isInstance(mc.screen)) {
            if (ticksLeft-- <= 0) waitFor = null;
            return;
        }
        EmiRecipe emiRecipe = Objects.requireNonNull(EmiApi.getRecipeManager().getRecipe(recipe), "emi has no recipe " + recipe);
        waitFor = null;
        recipe = null;
        EmiApi.displayRecipe(emiRecipe);
        if (!(mc.screen instanceof RecipeScreen screen)) throw new IllegalStateException("Emi's recipe screen didn't open");
        for (WidgetGroup group : currentPage(screen)) {
            if (group.recipe != emiRecipe) continue;
            for (Widget widget : group.widgets) {
                if (!(widget instanceof RecipeFillButtonWidget)) continue;
                Bounds bounds = widget.getBounds();
                double scale = mc.getWindow().getGuiScale();
                WikiImages.setMouse(mc, (group.x + bounds.x() + bounds.width() / 2D) * scale, (group.y + bounds.y() + bounds.height() / 2D) * scale);
                return;
            }
        }
        throw new IllegalStateException("Emi's recipe screen has no + button for " + emiRecipe.getId());
    }

    @SuppressWarnings("unchecked")
    private static List<WidgetGroup> currentPage(RecipeScreen screen) {
        try {
            Field field = RecipeScreen.class.getDeclaredField("currentPage");
            field.setAccessible(true);
            return (List<WidgetGroup>) field.get(screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't get emi's recipes", e);
        }
    }
}
