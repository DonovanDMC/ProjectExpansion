package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class BlockMatter extends Block implements IHasMatter {
    private final Matter matter;

    public BlockMatter(Matter matter) {
        super(Block.Properties.of().strength(2_000_000, 6_000_000).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Override
    public @NotNull Matter getMatter() {
        return matter;
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        MapColor color = matter.mapColor == null ? null : matter.mapColor.get();
        return color != null ? color : super.getMapColor(state, level, pos, defaultColor);
    }
}
