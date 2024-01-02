package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;

import java.util.Map;

public interface TELCommon {
    AABB getRandomEntityBoundingBox(Entity entity, RandomSource random);
    Map<Enchantment, Integer> getEnchantmentsFromStack(ItemStack stack);
}