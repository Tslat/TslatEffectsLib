package net.tslat.effectslib.mixin.common;

import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
	@Inject(method = "getEnchantmentList", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void filterExtendedEnchantments(ItemStack stack, int enchantSlot, int level, CallbackInfoReturnable<List<EnchantmentInstance>> callback, List<EnchantmentInstance> enchants) {
		enchants.removeIf(instance -> instance.enchantment instanceof ExtendedEnchantment extendedEnchant && !extendedEnchant.canEnchant(stack, ExtendedEnchantment.ENCHANTING_TABLE));
	}
}
