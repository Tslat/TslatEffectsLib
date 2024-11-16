package net.tslat.effectslib.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Stack;

@Mixin(LivingEntity.class)
public class LivingEntityNeoForgeMixin {
    @Shadow @Nullable protected Stack<DamageContainer> damageContainers;

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void tel$captureModifiedDamage(ServerLevel level, DamageSource damageSource, float damage, CallbackInfo ci) {
        if (damage != this.damageContainers.peek().getNewDamage())
            this.damageContainers.peek().setNewDamage(damage);
    }
}
