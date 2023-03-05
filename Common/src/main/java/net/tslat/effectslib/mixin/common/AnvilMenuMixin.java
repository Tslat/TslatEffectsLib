package net.tslat.effectslib.mixin.common;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean canEnchantExtended(Enchantment instance, ItemStack stack) {
		if (instance instanceof ExtendedEnchantment extendedEnchantment)
			return extendedEnchantment.canEnchant(stack, ExtendedEnchantment.ANVIL);

		return instance.canEnchant(stack);
	}
}
