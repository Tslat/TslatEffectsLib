package net.tslat.effectslib.mixin.common;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityForgeMixin {
	@Redirect(method = "curePotionEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;isCurativeItem(Lnet/minecraft/world/item/ItemStack;)Z"), remap = false)
	private boolean cancelForgeHook(MobEffectInstance instance, ItemStack itemStack) {
		if (instance.getEffect() instanceof ExtendedMobEffect)
			return false;

		return instance.isCurativeItem(itemStack);
	}
}
