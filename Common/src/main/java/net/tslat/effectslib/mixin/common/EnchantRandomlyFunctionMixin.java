package net.tslat.effectslib.mixin.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {
	@Redirect(method = "lambda$run$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"), remap = false)
	private static boolean canEnchantExtended(Enchantment instance, ItemStack stack) {
		if (instance instanceof ExtendedEnchantment extendedEnchantment)
			return extendedEnchantment.canEnchant(stack, ExtendedEnchantment.LOOT);

		return instance.canEnchant(stack);
	}
}
