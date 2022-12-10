package net.tslat.effectslib.mixin.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow private boolean effectsDirty;

	@Shadow @Final private Map<MobEffect, MobEffectInstance> activeEffects;

	@Shadow protected abstract void onEffectAdded(MobEffectInstance pInstance, @Nullable Entity pEntity);

	@Shadow protected abstract void onEffectRemoved(MobEffectInstance pEffectInstance);

	@Shadow public abstract Collection<MobEffectInstance> getActiveEffects();

	@ModifyArg(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
			),
			index = 1
	)
	public float modifyDamage(DamageSource damageSource, float damage) {
		if (!damageSource.isBypassMagic()) {
			LivingEntity victim = (LivingEntity)(Object)this;

			for (MobEffectInstance instance : victim.getActiveEffects()) {
				if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
					damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);
			}

			if (damageSource.getEntity() instanceof LivingEntity attacker) {
				for (MobEffectInstance instance : attacker.getActiveEffects()) {
					if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
						damage = extendedMobEffect.modifyOutgoingAttackDamage(attacker, victim, instance, damageSource, damage);
				}
			}
		}

		return damage;
	}

	@Inject(
			method = "hurt",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	public void checkCancellation(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
		if (checkEffectAttackCancellation((LivingEntity)(Object)this, damageSource, damage))
			callback.setReturnValue(false);
	}

	private boolean checkEffectAttackCancellation(LivingEntity victim, DamageSource damageSource, float damage) {
		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
				if (!extendedMobEffect.beforeIncomingAttack(victim, instance, damageSource, damage))
					return true;
		}

		return false;
	}

	@Inject(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
					shift = At.Shift.BEFORE
			),
			remap = false
	)
	public void afterAttack(DamageSource damageSource, float damage, CallbackInfo callback) {
		handlePostAttack((LivingEntity)(Object)this, damageSource, damage);
	}

	private void handlePostAttack(LivingEntity victim, DamageSource damageSource, float damage) {
		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
				extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, damage);
		}

		if (damageSource.getEntity() instanceof LivingEntity attacker) {
			for (MobEffectInstance instance : attacker.getActiveEffects()) {
				if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
					extendedMobEffect.afterOutgoingAttack(attacker, victim, instance, damageSource, damage);
			}
		}
	}

	@Redirect(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"
			)
	)
	public void onAdded(LivingEntity entity, MobEffectInstance effectInstance, Entity source) {
		if (effectInstance.getEffect() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onApplication(effectInstance, source, entity, effectInstance.getAmplifier());

		onEffectAdded(effectInstance, source);
	}

	@Redirect(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
			)
	)
	public boolean onUpdate(MobEffectInstance existingEffect, MobEffectInstance newEffect) {
		if (existingEffect.getEffect() instanceof ExtendedMobEffect extendedEffect)
			newEffect = extendedEffect.onReapplication(existingEffect, newEffect, (LivingEntity)(Object)this);

		return existingEffect.update(newEffect);
	}

	@ModifyArg(
			method = "removeEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V"
			)
	)
	public MobEffectInstance onRemoval(MobEffectInstance effectInstance) {
		if (effectInstance.getEffect() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onRemoval(effectInstance, (LivingEntity)(Object)this);

		return effectInstance;
	}

	@Inject(
			method = "canBeAffected",
			at = @At(
					value = "TAIL"
			),
			cancellable = true
	)
	public void canApplyEffect(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> callback) {
		if (effectInstance.getEffect() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApply((LivingEntity)(Object)this, effectInstance))
			callback.setReturnValue(false);

		if (!getActiveEffects().isEmpty()) {
			for (MobEffectInstance otherInstance : getActiveEffects()) {
				if (otherInstance.getEffect() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApplyOther((LivingEntity)(Object)this, otherInstance))
					callback.setReturnValue(false);
			}
		}
	}

	@Inject(
			method = "addEatEffect",
			at = @At(
					value = "HEAD"
			)
	)
	public void onFoodConsumption(ItemStack food, Level level, LivingEntity entity, CallbackInfo callback) {
		if (food.isEdible())
			checkEffectCuring(food, entity);
	}

	private void checkEffectCuring(ItemStack food, LivingEntity entity) {
		if (entity.getActiveEffectsMap().isEmpty())
			return;

		for (Iterator<Map.Entry<MobEffect, MobEffectInstance>> iterator = this.activeEffects.entrySet().iterator(); iterator.hasNext(); ) {
			MobEffectInstance effectInstance = iterator.next().getValue();

			if (effectInstance.getEffect() instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldCureEffect(effectInstance, food, entity)) {
				onEffectRemoved(effectInstance);
				iterator.remove();

				this.effectsDirty = true;
			}
		}
	}

	@Inject(
			method = "tickEffects",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/network/syncher/SynchedEntityData;get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;"
			),
			cancellable = true
	)
	protected void onEffectsTick(CallbackInfo callback) {
		if (((LivingEntity)(Object)this).level.isClientSide() && !doCustomEffectParticles((LivingEntity)(Object)this))
			callback.cancel();
	}

	private boolean doCustomEffectParticles(LivingEntity entity) {
		boolean continueVanilla = false;

		for (MobEffectInstance effect : this.activeEffects.values()) {
			if (!(effect.getEffect() instanceof ExtendedMobEffect extendedEffect) || !extendedEffect.doClientSideEffectTick(effect, entity))
				continueVanilla = true;
		}

		return continueVanilla;
	}
}
