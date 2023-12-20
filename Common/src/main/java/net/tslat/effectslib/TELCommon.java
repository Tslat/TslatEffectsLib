package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.tslat.effectslib.networking.TELNetworking;
import net.tslat.effectslib.networking.packet.TELParticlePacket;

public interface TELCommon {
    static void init() {
        TELNetworking.registerPacket(TELParticlePacket.class, TELParticlePacket::new);
    }

    AABB getRandomEntityBoundingBox(Entity entity, RandomSource random);
}
