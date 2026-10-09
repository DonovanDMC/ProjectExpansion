package cool.furry.mc.neoforge.projectexpansion.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.util.IChestLike;
import moze_intel.projecte.PECore;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

// lovingly "lifted" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/rendering/ChestRenderer.java
public class ChestRenderer <BE extends BlockEntity & IChestLike> implements BlockEntityRenderer<BE> {
	private final ModelPart lid;
	private final ModelPart bottom;
	private final ModelPart lock;

	private final BlockEntityType<BE> type;

	public ChestRenderer(BlockEntityRendererProvider.Context context, BlockEntityType<BE> type) {
		this.type = type;
		ModelPart modelpart = context.bakeLayer(ModelLayers.CHEST);
		this.bottom = modelpart.getChild("bottom");
		this.lid = modelpart.getChild("lid");
		this.lock = modelpart.getChild("lock");
	}

	@Override
	public void render(BlockEntity blockEntity, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight) {
		if (blockEntity.getLevel() == null || blockEntity.isRemoved()) return;
		BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
		Block block = state.getBlock();
		ResourceLocation texture = getTexture(block);
		if (blockEntity.getType().equals(this.type) && blockEntity instanceof IChestLike chest && texture != null) {
			matrix.pushPose();
			matrix.translate(0.5D, 0.5D, 0.5D);
			matrix.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
			matrix.translate(-0.5D, -0.5D, -0.5D);
			float lidAngle = 1.0F - chest.getOpenNess(partialTick);
			lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;
			VertexConsumer builder = renderer.getBuffer(RenderType.entityCutout(texture));
			lid.xRot = -(lidAngle * ((float) Math.PI / 2F));
			lock.xRot = lid.xRot;
			lid.render(matrix, builder, light, overlayLight);
			lock.render(matrix, builder, light, overlayLight);
			bottom.render(matrix, builder, light, overlayLight);
			matrix.popPose();
		}
	}

	private @Nullable ResourceLocation getTexture(Block block) {
		return switch (block) {
			case BlockAdvancedAlchemicalChest chest -> Main.rl(String.format("textures/block/advanced_alchemical_chest/%s.png", chest.getColor().getName()));
			case BlockCondenserMK3 ignored -> PECore.rl("textures/block/condenser_mk1.png");
			default -> null;
		};
	}
}