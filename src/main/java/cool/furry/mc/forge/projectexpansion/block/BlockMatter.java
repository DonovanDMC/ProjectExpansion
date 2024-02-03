package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class BlockMatter extends Block implements IHasMatter {
    private final Matter matter;

    public BlockMatter(Matter matter) {
        super(Block.Properties.of(Material.METAL).strength(2_000_000, 6_000_000).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public MaterialColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MaterialColor defaultColor) {
        MaterialColor color = matter.materialColor == null ? null : matter.materialColor.get();
        return color != null ? color : super.getMapColor(state, level, pos, defaultColor);
    }
}
