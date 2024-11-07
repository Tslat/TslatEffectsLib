package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.ExtendedMobEffectHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Handle the various {@link MobEffectInstance callbacks}
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements ExtendedMobEffectHolder {
	@Unique
	Object tel$extendedData;

	@WrapOperation(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"
			)
	)
	private boolean tel$onEffectTick(MobEffect effect, ServerLevel level, LivingEntity entity, int amplifier, Operation<Boolean> original) {
		if (effect instanceof ExtendedMobEffect extendedEffect)
			return extendedEffect.tick(entity, (MobEffectInstance)(Object)this, amplifier);

		return original.call(effect, level, entity, amplifier);
	}

	@WrapOperation(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"
			)
	)
	private boolean tel$checkEffectTick(MobEffect effect, int duration, int amplifier, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity entity) {
		if (!(effect instanceof ExtendedMobEffect extendedEffect))
			return original.call(effect, duration, amplifier);

		return extendedEffect.shouldTickEffect((MobEffectInstance)(Object)this, entity, duration, amplifier);
	}

	@Override
	public Object getExtendedMobEffectData() {
		return this.tel$extendedData;
	}

	@Override
	public void setExtendedMobEffectData(Object data) {
		this.tel$extendedData = data;
	}
}
