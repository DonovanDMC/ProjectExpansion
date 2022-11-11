package cool.furry.mc.forge.projectexpansion.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class ChestRenderer extends TileEntityRenderer<TileAdvancedAlchemicalChest> {

	private final ModelRenderer lid;
	private final ModelRenderer base;
	private final ModelRenderer latch;

	private final Predicate<Block> blockChecker;
	private final ResourceLocation texture;

	public ChestRenderer(TileEntityRendererDispatcher dispatcher, ResourceLocation texture, Predicate<Block> blockChecker) {
		super(dispatcher);
		this.texture = texture;
		this.blockChecker = blockChecker;
		this.base = new ModelRenderer(64, 64, 0, 19);
		this.base.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, 0.0F);
		this.lid = new ModelRenderer(64, 64, 0, 0);
		this.lid.addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, 0.0F);
		this.lid.y = 9.0F;
		this.lid.z = 1.0F;
		this.latch = new ModelRenderer(64, 64, 0, 0);
		this.latch.addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F, 0.0F);
		this.latch.y = 8.0F;
	}

	@Override
	public void render(@Nonnull TileAdvancedAlchemicalChest chestTile, float partialTick, @Nonnull MatrixStack matrix, @Nonnull IRenderTypeBuffer renderer, int light, int overlayLight) {
		matrix.pushPose();
		if (chestTile.getLevel() != null && !chestTile.isRemoved()) {
			BlockState state = chestTile.getLevel().getBlockState(chestTile.getBlockPos());
			if (blockChecker.test(state.getBlock())) {
				matrix.translate(0.5D, 0.5D, 0.5D);
				matrix.mulPose(Vector3f.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
				matrix.translate(-0.5D, -0.5D, -0.5D);
			}
		}
		float lidAngle = 1.0F - chestTile.getLidAngle(partialTick);
		lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;
		IVertexBuilder builder = renderer.getBuffer(RenderType.entityCutout(texture));
		lid.xRot = -(lidAngle * ((float) Math.PI / 2F));
		latch.xRot = lid.xRot;
		lid.render(matrix, builder, light, overlayLight);
		latch.render(matrix, builder, light, overlayLight);
		base.render(matrix, builder, light, overlayLight);
		matrix.popPose();
	}
}