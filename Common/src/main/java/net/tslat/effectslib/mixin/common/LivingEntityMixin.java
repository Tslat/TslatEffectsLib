package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	public abstract Collection<MobEffectInstance> getActiveEffects();

	@Shadow protected abstract void onEffectsRemoved(Collection<MobEffectInstance> p_366501_);

	@Shadow @Final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

	@WrapOperation(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
	)
	private float tel$hookDamageReduction(LivingEntity entity, DamageSource damageSource, float damage, Operation<Float> original) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = tel$handleDamageReduction(entity, damageSource, damage);

		return original.call(entity, damageSource, damage);
	}

    @Unique
	private float tel$handleDamageReduction(LivingEntity victim, DamageSource damageSource, float damage) {
		final List<Consumer<Float>> attackerCallbacks = new ObjectArrayList<>();
		final List<Consumer<Float>> victimCallbacks = new ObjectArrayList<>();
		final boolean bypassesEnchants = damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);

		if (damageSource.getEntity() instanceof LivingEntity attacker) {
			for (MobEffectInstance instance : attacker.getActiveEffects()) {
				if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect) {
					damage = extendedMobEffect.modifyOutgoingAttackDamage(attacker, victim, instance, damageSource, damage);

					attackerCallbacks.add(dmg -> extendedMobEffect.afterOutgoingAttack(attacker, victim, instance, damageSource, dmg));
				}
			}
		}

		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect) {
				damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);

				victimCallbacks.add(dmg -> extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, dmg));
			}
		}

		if (damage > 0) {
			for (Consumer<Float> consumer : attackerCallbacks) {
				consumer.accept(damage);
			}

			for (Consumer<Float> consumer : victimCallbacks) {
				consumer.accept(damage);
			}
		}

		return damage;
	}

	@Inject(
			method = "hurtServer",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	private void tel$checkCancellation(ServerLevel level,  DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
		if (tel$checkEffectAttackCancellation((LivingEntity)(Object)this, damageSource, damage))
			callback.setReturnValue(false);
	}

	@Unique
	private boolean tel$checkEffectAttackCancellation(LivingEntity victim, DamageSource damageSource, float damage) {
		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect)
				if (!extendedMobEffect.beforeIncomingAttack(victim, instance, damageSource, damage))
					return true;
		}

		return false;
	}

	@Inject(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"
			)
	)
	private void tel$onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity source, CallbackInfoReturnable<Boolean> callback) {
		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onApplication(effectInstance, source, (LivingEntity)(Object)this, effectInstance.getAmplifier());
	}

	@WrapOperation(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
			)
	)
	private boolean tel$onEffectUpdated(MobEffectInstance existingEffect, MobEffectInstance newEffect, Operation<Boolean> original) {
		if (existingEffect.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			newEffect = extendedEffect.onReapplication(existingEffect, newEffect, (LivingEntity)(Object)this);

		return original.call(existingEffect, newEffect);
	}

	@Inject(
			method = "removeEffect",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void tel$onEffectRemoved(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> callback) {
		if (effect.value() instanceof ExtendedMobEffect extendedEffect) {
			final MobEffectInstance effectInstance = this.activeEffects.get(extendedEffect);

			if (effectInstance instanceof MobEffectInstanceAccessor instance && instance.hasTicksRemaining() && !extendedEffect.onRemove(effectInstance, (LivingEntity)(Object)this))
				callback.setReturnValue(false);
		}
	}

	@WrapOperation(
			method = "removeAllEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"
			)
	)
	private void tel$captureRemovingEffectsMap(LivingEntity entity, Collection<MobEffectInstance> effects, Operation<Void> original) {
		for (Iterator<MobEffectInstance> iterator = effects.iterator(); iterator.hasNext();) {
			MobEffectInstance effectInstance = iterator.next();

			if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.onRemove(effectInstance, entity)) {
				iterator.remove();
				this.activeEffects.put(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(extendedEffect), effectInstance);
			}
		}

		original.call(entity, effects);
	}

	@WrapOperation(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"
			)
	)
	private void tel$onEffectExpired(LivingEntity entity, Collection<MobEffectInstance> effects, Operation<Void> original) {
		original.call(entity, effects);

		for (MobEffectInstance effectInstance : effects) {
			if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
				extendedEffect.onExpiry(effectInstance, entity);
		}
	}

	@Inject(
			method = "canBeAffected",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	private void tel$checkEffectApplicability(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> callback) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApply(self, effectInstance))
			callback.setReturnValue(false);

		if (!getActiveEffects().isEmpty()) {
			for (MobEffectInstance otherInstance : getActiveEffects()) {
				if (otherInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApplyOther(self, otherInstance))
					callback.setReturnValue(false);
			}
		}
	}

	@WrapOperation(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/syncher/SynchedEntityData;get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;",
					ordinal = 0
			)
	)
	private <T extends List<ParticleOptions>> Object tel$doEffectParticles(SynchedEntityData entityData, EntityDataAccessor<T> dataAcessor, Operation<T> original) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (self.level().isClientSide() && !tel$checkCustomEffectParticles(self))
			return List.of();

		return original.call(entityData, dataAcessor);
	}

	@Unique
	private boolean tel$checkCustomEffectParticles(LivingEntity entity) {
		boolean continueVanilla = false;

		for (MobEffectInstance effect : this.activeEffects.values()) {
			if (!(effect.getEffect().value() instanceof ExtendedMobEffect extendedEffect) || !extendedEffect.doClientSideEffectTick(effect, entity))
				continueVanilla = true;
		}

		return continueVanilla;
	}

	@WrapOperation(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;save()Lnet/minecraft/nbt/Tag;"))
	public Tag tel$wrapEffectSave(MobEffectInstance instance, Operation<Tag> original) {
		final Tag data = original.call(instance);

		if (data instanceof CompoundTag compoundTag && instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.write(compoundTag, instance);

		return data;
	}

	@WrapOperation(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;load(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/effect/MobEffectInstance;"))
	public MobEffectInstance tel$wrapEffectLoad(CompoundTag tag, Operation<MobEffectInstance> original) {
		final MobEffectInstance instance = original.call(tag);

		if (instance != null && instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.read(tag, instance);

		return instance;
	}
}
