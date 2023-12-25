package net.tslat.effectslib.mixin.common;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
	private float modifyDamage(DamageSource damageSource, float damage) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = tslatEffectsLib$handlePlayerDamage(damageSource, damage);

		return damage;
	}

	@Unique
	private float tslatEffectsLib$handlePlayerDamage(DamageSource damageSource, float damage) {
		final LivingEntity victim = (LivingEntity)(Object)this;
		final List<Consumer<Float>> attackerCallbacks = new ObjectArrayList<>();
		final List<Consumer<Float>> victimCallbacks = new ObjectArrayList<>();
		final boolean bypassesEnchants = damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);

		if (damageSource.getEntity() instanceof LivingEntity attacker) {
			for (MobEffectInstance instance : attacker.getActiveEffects()) {
				if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect) {
					damage = extendedMobEffect.modifyOutgoingAttackDamage(attacker, victim, instance, damageSource, damage);

					attackerCallbacks.add(dmg -> extendedMobEffect.afterOutgoingAttack(attacker, victim, instance, damageSource, dmg));
				}
			}

			if (!bypassesEnchants) {
				Map<Enchantment, EnchantmentUtil.EntityEnchantmentData> attackerEnchants = EnchantmentUtil.collectAllEnchantments(attacker, true);

				for (Map.Entry<Enchantment, EnchantmentUtil.EntityEnchantmentData> entry : attackerEnchants.entrySet()) {
					final EnchantmentUtil.EntityEnchantmentData data = entry.getValue();
					final ExtendedEnchantment enchant = (ExtendedEnchantment)data.getEnchantment();
					final int totalLevel = data.getTotalEnchantmentLevel();
					final int enchantedStacks = data.getEnchantedStacks().size();

					for (int i = 0; i < enchantedStacks; i++) {
						final IntObjectPair<ItemStack> stack = data.getEnchantedStacks().get(i);
						damage = enchant.modifyOutgoingAttackDamage(attacker, victim, damageSource, damage, stack.second(), stack.firstInt(), totalLevel);
						final boolean isLastStack = i == enchantedStacks - 1;

						attackerCallbacks.add(dmg -> enchant.afterOutgoingAttack(attacker, victim, damageSource, dmg, stack.second(), stack.firstInt(), totalLevel, isLastStack));
					}
				}
			}
		}

		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect) {
				damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);

				victimCallbacks.add(dmg -> extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, dmg));
			}
		}

		if (!bypassesEnchants) {
			Map<Enchantment, EnchantmentUtil.EntityEnchantmentData> victimEnchants = EnchantmentUtil.collectAllEnchantments(victim, true);

			for (Map.Entry<Enchantment, EnchantmentUtil.EntityEnchantmentData> entry : victimEnchants.entrySet()) {
				final EnchantmentUtil.EntityEnchantmentData data = entry.getValue();
				final ExtendedEnchantment enchant = (ExtendedEnchantment)data.getEnchantment();
				final int totalLevel = data.getTotalEnchantmentLevel();
				final int enchantedStacks = data.getEnchantedStacks().size();

				for (int i = 0; i < enchantedStacks; i++) {
					final IntObjectPair<ItemStack> stack = data.getEnchantedStacks().get(i);
					damage = enchant.modifyIncomingAttackDamage(victim, damageSource, damage, stack.second(), stack.firstInt(), totalLevel);
					final boolean isLastStack = i == enchantedStacks - 1;

					victimCallbacks.add(dmg -> enchant.afterIncomingAttack(victim, damageSource, dmg, stack.second(), stack.firstInt(), totalLevel, isLastStack));
				}
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
		if (tslatEffectsLib$checkEffectAttackCancellation((LivingEntity)(Object)this, damageSource, damage))
			callback.setReturnValue(false);
	}

	@Unique
	private boolean tslatEffectsLib$checkEffectAttackCancellation(LivingEntity victim, DamageSource damageSource, float damage) {
		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect() instanceof ExtendedMobEffect extendedMobEffect)
				if (!extendedMobEffect.beforeIncomingAttack(victim, instance, damageSource, damage))
					return true;
		}

		return false;
	}
}
