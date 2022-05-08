package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemUpgrade extends Item {
    public enum UpgradeType {
        SPEED,
        STACK;

        public String getName() {
            switch(this) {
                case SPEED: return "speed";
                case STACK: return "stack";
                default: return "unknown";
            }
        }

        public int getMax() {
            switch(this) {
                case SPEED: return 20; // -10 ticks per
                case STACK: return 6; // powers of 2
                default: return 0;
            }
        }
    }
    private final UpgradeType type;
    public ItemUpgrade(UpgradeType type) {
        super(new Item.Properties().group(Main.group).rarity(Rarity.EPIC));
        this.type = type;
    }


    public UpgradeType getType() {
        return type;
    }


    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, world, list, flag);
        list.add(new TranslationTextComponent(String.format("item.projectexpansion.%s_upgrade.tooltip", type.getName())));
        list.add(new TranslationTextComponent(String.format("item.projectexpansion.%s_upgrade.tooltip_max", type.getName()), type.getMax()).mergeStyle(TextFormatting.GREEN));
        list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").mergeStyle(TextFormatting.AQUA));
    }
}
