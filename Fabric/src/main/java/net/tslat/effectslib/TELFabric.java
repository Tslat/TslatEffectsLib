package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class TELFabric implements TELCommon {
    @Override
    public AABB getRandomEntityBoundingBox(Entity entity, RandomSource random) {
        return entity.getBoundingBox();
    }
}
