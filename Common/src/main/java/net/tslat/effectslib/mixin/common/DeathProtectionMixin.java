package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeathProtection.class)
public class DeathProtectionMixin {
    @WrapWithCondition(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/consume_effects/ConsumeEffect;apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    public boolean tel$checkDeathProtection(ConsumeEffect consumer, Level level, ItemStack stack, LivingEntity entity) {
        boolean cancelled = false;

        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.checkDeathProtectionConsumer(instance, entity, stack, consumer, cancelled))
                cancelled = true;
        }

        return !cancelled;
    }
}
