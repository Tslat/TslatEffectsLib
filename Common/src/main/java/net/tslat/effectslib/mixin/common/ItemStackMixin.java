package net.tslat.effectslib.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
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
}