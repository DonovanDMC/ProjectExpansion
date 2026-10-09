package cool.furry.mc.forge.projectexpansion.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.forge.projectexpansion.util.IChestLike;
import moze_intel.projecte.PECore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nullable;

// lovingly "lifted" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/rendering/ChestRenderer.java
public class ChestRenderer <BE extends TileEntity & IChestLike> extends TileEntityRenderer<BE> {
	private final ModelRenderer lid;
	private final ModelRenderer bottom;
	private final ModelRenderer lock;

	private final TileEntityType<BE> type;

	// models are built in code before 1.17's model layers
	public ChestRenderer(TileEntityRendererDispatcher dispatcher, TileEntityType<BE> type) {
		super(dispatcher);
		this.type = type;
		this.bottom = new ModelRenderer(64, 64, 0, 19);
		this.bottom.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, 0.0F);
		this.lid = new ModelRenderer(64, 64, 0, 0);
		this.lid.addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, 0.0F);
		this.lid.y = 9.0F;
		this.lid.z = 1.0F;
		this.lock = new ModelRenderer(64, 64, 0, 0);
		this.lock.addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F, 0.0F);
		this.lock.y = 8.0F;
	}

	@Override
	public void render(BE blockEntity, float partialTick, MatrixStack matrix, IRenderTypeBuffer renderer, int light, int overlayLight) {
		if (blockEntity.getLevel() == null || blockEntity.isRemoved()) return;
		BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
		Block block = state.getBlock();
		ResourceLocation texture = getTexture(block);
		if (blockEntity.getType().equals(this.type) && blockEntity instanceof IChestLike && texture != null) {
		    IChestLike chest = ((IChestLike) blockEntity);
			matrix.pushPose();
			matrix.translate(0.5D, 0.5D, 0.5D);
			matrix.mulPose(Vector3f.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
			matrix.translate(-0.5D, -0.5D, -0.5D);
			float lidAngle = 1.0F - chest.getOpenNess(partialTick);
			lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;
			IVertexBuilder builder = renderer.getBuffer(RenderType.entityCutout(texture));
			lid.xRot = -(lidAngle * ((float) Math.PI / 2F));
			lock.xRot = lid.xRot;
			lid.render(matrix, builder, light, overlayLight);
			lock.render(matrix, builder, light, overlayLight);
			bottom.render(matrix, builder, light, overlayLight);
			matrix.popPose();
		}
	}

	private @Nullable ResourceLocation getTexture(Block block) {
		if (block instanceof BlockAdvancedAlchemicalChest) {
		    BlockAdvancedAlchemicalChest chest = ((BlockAdvancedAlchemicalChest) block);
			return Main.rl(String.format("textures/block/advanced_alchemical_chest/%s.png", chest.getColor().getName()));
		}
		if (block instanceof BlockCondenserMK3) {
			return PECore.rl("textures/block/condenser_mk1.png");
		}
		return null;
	}
}