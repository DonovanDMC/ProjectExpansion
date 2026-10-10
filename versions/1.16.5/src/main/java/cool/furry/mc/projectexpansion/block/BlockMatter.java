package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.util.IHasMatter;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockMatter extends Block implements IHasMatter, IMatterBlock {
    private final Matter matter;

    public BlockMatter(AbstractBlock.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static AbstractBlock.Properties getProperties(Matter matter) {
        return AbstractBlock.Properties.of(Material.METAL, state -> matter.mapColor == null ? Material.METAL.getColor() : matter.mapColor.get()).strength(2_000_000, 6_000_000).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }

}
