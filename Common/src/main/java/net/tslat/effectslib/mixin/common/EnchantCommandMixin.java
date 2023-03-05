package net.tslat.effectslib.mixin.common;

import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {
	@Redirect(method = "enchant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean canEnchantExtended(Enchantment instance, ItemStack stack) {
		if (instance instanceof ExtendedEnchantment extendedEnchantment)
			return extendedEnchantment.canEnchant(stack, ExtendedEnchantment.COMMAND);

		return instance.canEnchant(stack);
	}
}
