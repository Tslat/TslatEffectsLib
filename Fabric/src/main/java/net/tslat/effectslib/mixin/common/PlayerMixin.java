package net.tslat.effectslib.mixin.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@ModifyArg(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
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
}
