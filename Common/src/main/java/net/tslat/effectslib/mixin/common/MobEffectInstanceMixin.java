package net.tslat.effectslib.mixin.common;

import net.minecraft.nbt.CompoundTag;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

	@Shadow protected abstract boolean hasRemainingDuration();

	@Redirect(
			method = "tick",
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
					target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"
			)
	)
	private boolean checkVanillaEffectTick(MobEffect effect, int duration, int amplifier) {
		return !(effect instanceof ExtendedMobEffect) && effect.shouldApplyEffectTickThisTick(duration, amplifier);
	}

	@Inject(
			method = "tick",
			at = @At(value = "HEAD")
	)
	private void checkEffectTick(LivingEntity entity, Runnable runnable, CallbackInfoReturnable<Boolean> callback) {
		if (hasRemainingDuration() && this.getEffect() instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldTickEffect((MobEffectInstance)(Object)this, entity, this.duration, this.amplifier))
			extendedEffect.tick(entity, (MobEffectInstance)(Object)this, this.amplifier);
	}

	@Inject(
			method = "writeDetailsTo",
			at = @At(value = "TAIL")
	)
	private void write(CompoundTag pNbt, CallbackInfo callback) {
		if (this.getEffect() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.write(pNbt, (MobEffectInstance)(Object)this);
	}

	@Inject(
			method = "loadSpecifiedEffect",
			at = @At(value = "HEAD")
	)
	private static void load(MobEffect pEffect, CompoundTag pNbt, CallbackInfoReturnable<MobEffectInstance> callback) {
		if (pEffect instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.read(pNbt, callback.getReturnValue());
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
