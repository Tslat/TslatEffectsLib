package net.tslat.effectslib.mixin.common;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {
	@Unique
	private static ItemStack tslatEffectsLib$capturedStack;

	@Inject(method = "removeNonCurses", at = @At("HEAD"))
	public void captureTelStack(ItemStack stack, int damage, int count, CallbackInfoReturnable<ItemStack> cir) {
		tslatEffectsLib$capturedStack = stack;
	}

	@Inject(method = {"lambda$removeNonCurses$0", "method_16694"}, at = @At(value = "HEAD", remap = false), cancellable = true)
	private static void canEnchantExtended(Map.Entry<Enchantment, Integer> entry, CallbackInfoReturnable<Boolean> cir) {
		if (entry.getKey() instanceof ExtendedEnchantment extendedEnchantment) {
			Boolean shouldRemove = extendedEnchantment.shouldGrindstoneRemove(tslatEffectsLib$capturedStack);

			if (shouldRemove != null)
				cir.setReturnValue(!shouldRemove);
		}
	}
}
