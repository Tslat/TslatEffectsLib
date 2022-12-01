package net.tslat.effectslib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(
			method = "render",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V"
					),
					to = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/client/renderer/GameRenderer;renderConfusionOverlay(F)V"
					)
			),
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Ljava/lang/Double;floatValue()F",
					shift = At.Shift.AFTER
			)
	)
	public void renderEffectOverlays(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo callback) {
		doExtendedEffectRenders(this.minecraft.player, partialTicks);
	}

	private void doExtendedEffectRenders(LocalPlayer player, float partialTicks) {
		PoseStack poseStack = new PoseStack();

		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect && extendedMobEffect.getOverlayRenderer() != null)
				extendedMobEffect.getOverlayRenderer().render(poseStack, partialTicks, instance);
		}
	}
}
