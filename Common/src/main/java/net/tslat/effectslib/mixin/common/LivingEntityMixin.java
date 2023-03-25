package net.tslat.effectslib.mixin.common;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
	private float modifyDamage(DamageSource damageSource, float damage) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = handleEntityDamage(damageSource, damage);

		return damage;
	}

	private float handleEntityDamage(DamageSource damageSource, float damage) {
		LivingEntity victim = (LivingEntity)(Object)this;
		List<Consumer<Float>> attackerCallbacks = new ObjectArrayList<>();
		List<Consumer<Float>> victimCallbacks = new ObjectArrayList<>();

		if (damageSource.getEntity() instanceof LivingEntity attacker) {
			for (MobEffectInstance instance : attacker.getActiveEffects()) {
				if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect) {
					damage = extendedMobEffect.modifyOutgoingAttackDamage(attacker, victim, instance, damageSource, damage);

					attackerCallbacks.add(dmg -> extendedMobEffect.afterOutgoingAttack(attacker, victim, instance, damageSource, dmg));
				}
			}

			if (!damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
				Pair<Integer, Map<ItemStack, List<EnchantmentInstance>>> attackerEnchants = EnchantmentUtil.collectAllEnchantments(attacker, true);
				int index = 0;

				for (Map.Entry<ItemStack, List<EnchantmentInstance>> entry : attackerEnchants.getSecond().entrySet()) {
					int size = entry.getValue().size();

					for (EnchantmentInstance instance : entry.getValue()) {
						ExtendedEnchantment enchant = ((ExtendedEnchantment)instance.enchantment);
						damage = enchant.modifyOutgoingAttackDamage(attacker, victim, damageSource, damage, entry.getKey(), instance.level, attackerEnchants.getFirst());
						final int thisIndex = index;

						attackerCallbacks.add(dmg -> enchant.afterOutgoingAttack(attacker, victim, damageSource, dmg, entry.getKey(), instance.level, attackerEnchants.getFirst(), thisIndex == size - 1));
					}

					index++;
				}
			}
		}

		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect) {
				damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);

				victimCallbacks.add(dmg -> extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, dmg));
			}
		}

		if (!damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
			Pair<Integer, Map<ItemStack, List<EnchantmentInstance>>> victimEnchants = EnchantmentUtil.collectAllEnchantments(victim, true);
			int index = 0;

			for (Map.Entry<ItemStack, List<EnchantmentInstance>> entry : victimEnchants.getSecond().entrySet()) {
				int size = entry.getValue().size();

				for (EnchantmentInstance instance : entry.getValue()) {
					ExtendedEnchantment enchant = ((ExtendedEnchantment)instance.enchantment);
					damage = enchant.modifyIncomingAttackDamage(victim, damageSource, damage, entry.getKey(), instance.level, victimEnchants.getFirst());
					final int thisIndex = index;

					victimCallbacks.add(dmg -> enchant.afterIncomingAttack(victim, damageSource, dmg, entry.getKey(), instance.level, victimEnchants.getFirst(), thisIndex == size - 1));
				}

				index++;
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
			method = "hurt",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	private void checkCancellation(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
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

	@Redirect(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"
			)
	)
	private void onAdded(LivingEntity entity, MobEffectInstance effectInstance, Entity source) {
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
	private boolean onUpdate(MobEffectInstance existingEffect, MobEffectInstance newEffect) {
		if (existingEffect.getEffect() instanceof ExtendedMobEffect extendedEffect)
			newEffect = extendedEffect.onReapplication(existingEffect, newEffect, (LivingEntity)(Object)this);

		return existingEffect.update(newEffect);
	}

	@Inject(
			method = "removeEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V",
					shift = At.Shift.AFTER,
					by = 1
			),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION
	)
	private void onRemoval(MobEffect effect, CallbackInfoReturnable<Boolean> cir, MobEffectInstance effectInstance) {
		if (effect instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onRemoval(effectInstance, (LivingEntity)(Object)this);
	}

	@Inject(
			method = "canBeAffected",
			at = @At(
					value = "TAIL"
			),
			cancellable = true
	)
	private void canApplyEffect(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> callback) {
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
	private void onFoodConsumption(ItemStack food, Level level, LivingEntity entity, CallbackInfo callback) {
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
	private void onEffectsTick(CallbackInfo callback) {
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

	@Inject(
			method = "collectEquipmentChanges",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void onEquipmentChange(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> callback, Map<EquipmentSlot, ItemStack> changeMap, EquipmentSlot[] slots, int slotsSize, int slotIndex, EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
		handleEquipChange((LivingEntity)(Object)this, oldStack, newStack, slot);
	}

	private void handleEquipChange(LivingEntity entity, ItemStack from, ItemStack to, EquipmentSlot slot) {
		for (EnchantmentInstance instance : EnchantmentUtil.getStackEnchantmentsForUse(entity, from, slot, true)) {
			((ExtendedEnchantment)instance.enchantment).onUnequip(entity, slot, from, to, instance.level);
		}

		for (EnchantmentInstance instance : EnchantmentUtil.getStackEnchantmentsForUse(entity, to, slot, true)) {
			((ExtendedEnchantment)instance.enchantment).onEquip(entity, slot, from, to, instance.level);
		}
	}
}
