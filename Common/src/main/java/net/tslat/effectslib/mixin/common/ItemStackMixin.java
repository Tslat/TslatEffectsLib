package net.tslat.effectslib.mixin.common;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract boolean isEdible();

    @Inject(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V"))
    public void doExtendedEnchantmentsTick(Level level, Entity entity, int slot, boolean isSelected, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity)
            tslatEffectsLib$tickEnchantmentsForStack(livingEntity, (ItemStack)(Object)this);
    }

    @Unique
    private static void tslatEffectsLib$tickEnchantmentsForStack(LivingEntity entity, ItemStack stack) {
        for (EnchantmentInstance enchant : EnchantmentUtil.getStackEnchantmentsForUse(entity, stack, null, true)) {
            ((ExtendedEnchantment)enchant.enchantment).tick(entity, stack);
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    public void applyCustomCuring(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (isEdible() && !level.isClientSide && !entity.getActiveEffectsMap().isEmpty())
            tslatEffectsLib$checkCustomCuring(entity, (ItemStack)(Object)this);
    }

    @Unique
    private static void tslatEffectsLib$checkCustomCuring(LivingEntity entity, ItemStack stack) {
        final Set<MobEffect> removingEffects = new ObjectOpenHashSet<>();

        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect() instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldCureEffect(effect, stack, entity))
                removingEffects.add(extendedEffect);
        }

        for (MobEffect effect : removingEffects) {
            entity.removeEffect(effect);
        }
    }
}