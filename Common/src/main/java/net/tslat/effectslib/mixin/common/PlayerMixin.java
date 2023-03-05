package net.tslat.effectslib.mixin.common;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
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
		if (!damageSource.isBypassMagic()) {
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

				if (!damageSource.isBypassEnchantments()) {
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

			if (!damageSource.isBypassEnchantments()) {
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
}
