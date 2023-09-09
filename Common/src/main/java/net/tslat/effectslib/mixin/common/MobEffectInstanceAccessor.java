package net.tslat.effectslib.mixin.common;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
	@Invoker("hasRemainingDuration")
	boolean hasTicksRemaining();
}