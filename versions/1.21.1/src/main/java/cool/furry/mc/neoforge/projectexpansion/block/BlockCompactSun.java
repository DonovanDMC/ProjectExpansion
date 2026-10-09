package cool.furry.mc.neoforge.projectexpansion.block;

import cool.furry.mc.neoforge.projectexpansion.registries.DamageSources;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Matter;
import cool.furry.mc.neoforge.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCompactSun extends Block implements IMatterBlock {
    public BlockCompactSun(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties getProperties() {
        return BlockBehaviour.Properties.of().strength(2_000_000, 6_000_000).requiresCorrectToolForDrops().lightLevel((state) -> 15);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.COMPACT_SUN_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.COMPACT_SUN_TOOLTIP2.translateColored(ChatFormatting.GRAY, Util.getSunBonus()));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return MapColor.COLOR_YELLOW;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState blockState, Entity entity) {
        DamageSources damage = DamageSources.fromLevel(level);
        if (entity instanceof ServerPlayer player && !SunExposureHelper.wearingProtectiveBoots(player)) {
            float maxPlayerHealth = player.getMaxHealth();
            // deal 60% of the player's max health (6 hearts for 20) in damage every 20 ticks
            player.hurt(damage.walkOnSun(), maxPlayerHealth * 0.6f);
        }
        super.stepOn(level, pos, blockState, entity);
    }

    @Override
    public IMatterType getMatterType() {
        return Util.getMatterForProjectE(Matter.FINAL);
    }

    @SuppressWarnings("unused")
    public static boolean adjacent(@Nullable BlockGetter level, BlockPos pos) {
        return adjacent(level, pos, null);
    }

    public static boolean adjacent(@Nullable BlockGetter level, BlockPos pos, @Nullable Direction filterDirection) {
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
