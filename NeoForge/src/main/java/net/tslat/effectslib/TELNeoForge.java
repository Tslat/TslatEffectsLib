package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.Map;

public class TELNeoForge implements TELCommon {
    @Override
    public AABB getRandomEntityBoundingBox(Entity entity, RandomSource random) {
        if (!entity.isMultipartEntity())
            return entity.getBoundingBox();

        PartEntity<?>[] parts = entity.getParts();

        return parts[random.nextInt(parts.length)].getBoundingBox();
    }

    @Override
    public Map<Enchantment, Integer> getEnchantmentsFromStack(ItemStack stack) {
        return TELItemStackData.getDataFor(stack).getCachedEnchantments(stack);
    }
}