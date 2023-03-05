package net.tslat.effectslib.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
	@Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("HEAD"), cancellable = true)
	private static void getExtendedEnchantmentLevel(Enchantment enchant, LivingEntity entity, CallbackInfoReturnable<Integer> callback) {
		if (enchant instanceof ExtendedEnchantment extendedEnchant)
			callback.setReturnValue(extendedEnchant.getTotalEnchantmentLevel(entity));
	}
}
