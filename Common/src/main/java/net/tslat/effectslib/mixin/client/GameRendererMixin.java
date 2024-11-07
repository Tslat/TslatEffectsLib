package net.tslat.effectslib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
    private Minecraft minecraft;

	@Shadow public abstract Matrix4f getProjectionMatrix(float p_363849_);

	@Shadow protected abstract float getFov(Camera p_109142_, float p_109143_, boolean p_109144_);

	@Shadow @Final private Camera mainCamera;

	@WrapOperation(
			method = "renderLevel",
			at = @At(
					value = "INVOKE",
					target = "Ljava/lang/Double;floatValue()F"
			)
	)
	private float tel$renderEffectOverlays(Double instance, Operation<Float> original, DeltaTracker deltaTracker) {
		float scale = original.call(instance);

		tel$doExtendedEffectRenders(this.minecraft.player, deltaTracker, scale);

		return scale;
	}

	@Unique
	private void tel$doExtendedEffectRenders(LocalPlayer player, DeltaTracker deltaTracker, float screenEffectScale) {
		PoseStack poseStack = new PoseStack();
		Supplier<Matrix4f> projectionMatrix = () -> {
			Matrix4f matrix4f = getProjectionMatrix(getFov(this.mainCamera, deltaTracker.getGameTimeDeltaPartialTick(true), true));

			matrix4f.mul(poseStack.last().pose());

			return matrix4f;
		};

		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect && extendedMobEffect.getOverlayRenderer() != null)
				extendedMobEffect.getOverlayRenderer().render(poseStack, projectionMatrix, deltaTracker, screenEffectScale, instance);
		}
	}
}
