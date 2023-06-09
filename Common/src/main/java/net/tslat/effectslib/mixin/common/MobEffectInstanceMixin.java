package net.tslat.effectslib.mixin.common;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.ExtendedMobEffectHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handle the various {@link MobEffectInstance callbacks}
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements ExtendedMobEffectHolder {
	@Unique
	Object data;

	@Shadow
	private int duration;
	@Shadow
	public abstract MobEffect getEffect();
	@Shadow
	private int amplifier;
	@Shadow
	public abstract void applyEffect(LivingEntity pEntity);

	@Redirect(
			method = "applyEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V"
			)
	)
	private void tickEffect(MobEffect effect, LivingEntity entity, int amplifier) {
		if (effect instanceof ExtendedMobEffect extendedEffect) {
			extendedEffect.tick(entity, (MobEffectInstance)(Object)this, amplifier);
		}
		else {
			effect.applyEffectTick(entity, amplifier);
		}
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;isDurationEffectTick(II)Z"
			)
	)
	private boolean checkVanillaEffectTick(MobEffect effect, int duration, int amplifier) {
		return !(effect instanceof ExtendedMobEffect) && effect.isDurationEffectTick(duration, amplifier);
	}

	@Inject(
			method = "tick",
			at = @At(value = "HEAD")
	)
	private void checkEffectTick(LivingEntity entity, Runnable runnable, CallbackInfoReturnable<Boolean> callback) {
		if (this.duration > 0 && this.getEffect() instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldTickEffect((MobEffectInstance)(Object)this, entity, this.duration, this.amplifier))
			applyEffect(entity);
	}

	@Override
	public Object getExtendedMobEffectData() {
		return this.data;
	}

	@Override
	public void setExtendedMobEffectData(Object data) {
		this.data = data;
	}
}
