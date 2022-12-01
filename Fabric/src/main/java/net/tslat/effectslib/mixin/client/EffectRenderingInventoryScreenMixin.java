package net.tslat.effectslib.mixin.client;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook {@link ExtendedMobEffect#getDisplayName(MobEffectInstance)}
 */
@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {
	@Inject(
			method = "getEffectName",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	public void getEffectName(MobEffectInstance effect, CallbackInfoReturnable<Component> callback) {
		if (effect.getEffect() instanceof ExtendedMobEffect extendedEffect)
			callback.setReturnValue(extendedEffect.getDisplayName(effect));
	}
}
