package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

public class TELForge implements TELCommon {
    @Override
    public AABB getRandomEntityBoundingBox(Entity entity, RandomSource random) {
        if (!entity.isMultipartEntity())
            return entity.getBoundingBox();

        PartEntity<?>[] parts = entity.getParts();

        return parts[random.nextInt(parts.length)].getBoundingBox();
    }
}
