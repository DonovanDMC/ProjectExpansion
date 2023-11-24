package cool.furry.mc.forge.projectexpansion.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import cool.furry.mc.forge.projectexpansion.util.IChestLike;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.function.Predicate;
import java.util.function.Supplier;

// lovingly "lifted" from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/rendering/ChestRenderer.java
public class ChestRenderer <B extends Block, BE extends BlockEntity & IChestLike> implements BlockEntityRenderer<BE> {
	private final ModelPart lid;
	private final ModelPart bottom;
	private final ModelPart lock;

	private final Predicate<Block> blockChecker;
	private final ResourceLocation texture;

	public ChestRenderer(BlockEntityRendererProvider.Context context, ResourceLocation texture, Supplier<RegistryObject<B>> type) {
		this.texture = texture;
		this.blockChecker = block -> block == type.get().get();
		ModelPart modelpart = context.bakeLayer(ModelLayers.CHEST);
		this.bottom = modelpart.getChild("bottom");
		this.lid = modelpart.getChild("lid");
		this.lock = modelpart.getChild("lock");
	}

	@Override
	public void render(@NotNull BE chest, float partialTick, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer, int light, int overlayLight) {
		matrix.pushPose();
		if (chest.getLevel() != null && !chest.isRemoved()) {
			BlockState state = chest.getLevel().getBlockState(chest.getBlockPos());
			if (blockChecker.test(state.getBlock())) {
				matrix.translate(0.5D, 0.5D, 0.5D);
				matrix.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
				matrix.translate(-0.5D, -0.5D, -0.5D);
			}
		}
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