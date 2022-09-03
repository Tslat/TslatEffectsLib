package net.tslat.effectslib.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Ljava/lang/Double;floatValue()F",
					shift = At.Shift.AFTER
			),
			locals = LocalCapture.PRINT
	)
	public void renderEffectOverlays(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo callback) {
		for (MobEffectInstance effect : this.minecraft.player.getActiveEffects()) {
			if (effect.getEffect() instanceof ExtendedMobEffect extendedEffect) {
				if (extendedEffect.getOverlayRenderer() != null)
					extendedEffect.getOverlayRenderer().render(null, partialTicks, effect);
			}
		}
	}
}
