package net.tslat.effectslib.mixin.common;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.EventHooks;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityNeoForgeMixin {
	@Redirect(method = "removeEffectsCuredBy", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/neoforged/neoforge/common/EffectCure;)Z"), remap = false)
	private boolean cancelForgeHook(LivingEntity entity, MobEffectInstance instance, EffectCure cure) {
		if (instance.getEffect() instanceof ExtendedMobEffect && (cure == EffectCures.MILK || cure == EffectCures.HONEY))
			return true;

		return EventHooks.onEffectRemoved(entity, instance, cure);
	}
}
