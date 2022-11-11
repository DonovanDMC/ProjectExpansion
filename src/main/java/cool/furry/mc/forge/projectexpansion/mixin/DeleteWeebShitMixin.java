package cool.furry.mc.forge.projectexpansion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import moze_intel.projecte.rendering.LayerYue;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// this removes the "yue" from players that aren't sin or their gf when in development (because it's always shown in development, thanks for that sin)
@SuppressWarnings("unused")
@Mixin(LayerYue.class)
public class DeleteWeebShitMixin {
    @Inject(at = @At("HEAD"), method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V", remap = false, cancellable = true)
    public void render(MatrixStack matrix, IRenderTypeBuffer renderer, int light, AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if(!FMLEnvironment.production && !player.getUUID().equals(SIN_UUID) && !player.getUUID().equals(CLAR_UUID)) ci.cancel();
    }

    @Shadow(remap = false) @Final private static UUID SIN_UUID;
    @Shadow(remap = false) @Final private static UUID CLAR_UUID;
}
