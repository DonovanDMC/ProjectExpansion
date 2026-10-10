package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.registries.DamageSources;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCompactSun extends Block implements IMatterBlock {
    public BlockCompactSun(AbstractBlock.Properties properties) {
        super(properties);
    }

    public static AbstractBlock.Properties getProperties() {
        return AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_YELLOW).strength(2_000_000, 6_000_000).requiresCorrectToolForDrops().lightLevel((state) -> 15);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.COMPACT_SUN_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.COMPACT_SUN_TOOLTIP2.translateColored(TextFormatting.GRAY, Util.getSunBonus()));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public void stepOn(World level, BlockPos pos, Entity entity) {
        DamageSources damage = DamageSources.fromLevel(level);
        if (entity instanceof ServerPlayerEntity && !SunExposureHelper.wearingProtectiveBoots(((ServerPlayerEntity) entity))) {
            ServerPlayerEntity player = ((ServerPlayerEntity) entity);
            float maxPlayerHealth = player.getMaxHealth();
            // deal 60% of the player's max health (6 hearts for 20) in damage every 20 ticks
            player.hurt(damage.walkOnSun(), maxPlayerHealth * 0.6f);
        }
        super.stepOn(level, pos, entity);
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(Matter.FINAL);
    }

    @SuppressWarnings("unused")
    public static boolean adjacent(@Nullable IBlockReader level, BlockPos pos) {
        return adjacent(level, pos, null);
    }

    public static boolean adjacent(@Nullable IBlockReader level, BlockPos pos, @Nullable Direction filterDirection) {
        if (level == null) {
            return false;
        }

        for (Direction dir : Direction.values()) {
            if(filterDirection != null && dir != filterDirection) {
                continue;
            }

            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof BlockCompactSun) {
                return true;
            }
        }

        return false;
    }
}
