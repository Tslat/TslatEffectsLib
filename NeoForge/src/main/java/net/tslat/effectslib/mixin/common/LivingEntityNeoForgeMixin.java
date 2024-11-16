package net.tslat.effectslib.mixin.common;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.EventHooks;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNeoForgeMixin {
	@Shadow protected abstract void onEffectRemoved(MobEffectInstance effectInstance);

	@Shadow @Final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

	@Shadow private boolean effectsDirty;

	@Shadow @Nullable protected Stack<DamageContainer> damageContainers;

	@Inject(method = "actuallyHurt", at = @At("HEAD"))
	private void tel$captureModifiedDamage(DamageSource damageSource, float damageAmount, CallbackInfo ci) {
		if (damageAmount != this.damageContainers.peek().getNewDamage())
			this.damageContainers.peek().setNewDamage(damageAmount);
	}

	@Redirect(method = "removeEffectsCuredBy", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/neoforged/neoforge/common/EffectCure;)Z"), remap = false)
	private boolean cancelForgeHook(LivingEntity entity, MobEffectInstance instance, EffectCure cure) {
		if (instance.getEffect().value() instanceof ExtendedMobEffect && (cure == EffectCures.MILK || cure == EffectCures.HONEY || cure == EffectCures.PROTECTED_BY_TOTEM))
			return true;

		return EventHooks.onEffectRemoved(entity, instance, cure);
	}

	@Redirect(
			method = "checkTotemDeathProtection",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectsCuredBy(Lnet/neoforged/neoforge/common/EffectCure;)Z"
			)
	)
	private boolean tel$checkTotemUndyingClear(LivingEntity entity, EffectCure effectCure) {
		if (entity.level().isClientSide())
			return false;

		boolean hasRemoved = false;

		for (Iterator<MobEffectInstance> iterator = this.activeEffects.values().iterator(); iterator.hasNext();) {
			MobEffectInstance instance = iterator.next();
			MobEffect effect = instance.getEffect().value();

			if ((effect instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldBeRemovedByTotemOfDeath(instance, entity)) || (instance.getCures().contains(EffectCures.PROTECTED_BY_TOTEM) && !EventHooks.onEffectRemoved(entity, instance, EffectCures.PROTECTED_BY_TOTEM))) {
				onEffectRemoved(instance);
				iterator.remove();

				hasRemoved = true;
				this.effectsDirty = true;
			}
		}

		return hasRemoved;
	}
}
