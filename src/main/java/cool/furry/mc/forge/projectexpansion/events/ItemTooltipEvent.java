package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.Enchantments;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ItemTooltipEvent {
    // we need to be lower priority than ProjectE's listener so the EMC component is present when we get the event
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void itemTooltipEvent(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()|| event.getPlayer() == null || event.getPlayer().isDeadOrDying()) {
            return;
        }

        learnedTooltip: if(Config.enabledLearnedTooltip.get() && (!ProjectEConfig.client.shiftEmcToolTips.get() || Screen.hasShiftDown())) {
            boolean hasValue = ProjectEAPI.getEMCProxy().hasValue(stack);
            if (!hasValue) {
                break learnedTooltip;
            }

            IKnowledgeProvider provider = Util.getKnowledgeProvider(event.getPlayer());
            if (provider == null) {
                break learnedTooltip;
            }

            boolean hasKnowledge = provider.hasKnowledge(ItemInfo.fromStack(stack));
            long value = ProjectEAPI.getEMCProxy().getValue(stack);
            AtomicInteger index = new AtomicInteger(-1);
            AtomicInteger peTransmutableIndex = new AtomicInteger(-1);
            for (ITextComponent c : event.getToolTip()) {
                if (c.getString().equals(EMCHelper.getEmcTextComponent(value, 1).getString())) {
                    index.set(event.getToolTip().indexOf(c));
                    continue;
                }

                if (c.getString().equals(I18n.get(PELang.EMC_HAS_KNOWLEDGE.getTranslationKey()))) {
                    peTransmutableIndex.set(event.getToolTip().indexOf(c));
                }
            }

            // attempt to add a minimal notice
            if (index.get() != -1) {
                event.getToolTip().set(index.get(), event.getToolTip().get(index.get()).copy().append(new StringTextComponent(" (").setStyle(ColorStyle.WHITE)).append(hasKnowledge ?
                        new StringTextComponent("✓").setStyle(ColorStyle.GREEN) : new StringTextComponent("✗").setStyle(ColorStyle.RED)
                ).append(new StringTextComponent(")").setStyle(ColorStyle.WHITE)));
            } else {
                // if we can't find an existing EMC element, add a new more detailed element
                event.getToolTip().add(hasKnowledge ?
                        new TranslationTextComponent("text.projectexpansion.learned").setStyle(ColorStyle.GREEN) : new TranslationTextComponent("text.projectexpansion.notLearned").setStyle(ColorStyle.RED)
                );
            }

            if (peTransmutableIndex.get() != -1) {
                event.getToolTip().remove(peTransmutableIndex.get());
            }
        }

        boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
        if(hasEnch) {
            boolean enabled = stack.getOrCreateTag().getBoolean(NBTNames.ALCHEMICAL_COLLECTION_ENABLED);
            event.getToolTip().add(new TranslationTextComponent("text.projectexpansion.alchemical_collection", new TranslationTextComponent(enabled ? "text.projectexpansion.enabled" : "text.projectexpansion.disabled").setStyle(enabled ? ColorStyle.GREEN : ColorStyle.RED)));
        }
    }
}
