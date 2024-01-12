package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.tslat.effectslib.networking.TELNetworking;
import net.tslat.effectslib.networking.packet.TELClearParticlesPacket;
import net.tslat.effectslib.networking.packet.TELParticlePacket;

import java.util.Map;

public interface TELCommon {
    static void init() {
        TELNetworking.registerPacket(TELParticlePacket.class, TELParticlePacket::new);
        TELNetworking.registerPacket(TELClearParticlesPacket.class, TELClearParticlesPacket::new);
    }

    AABB getRandomEntityBoundingBox(Entity entity, RandomSource random);
    Map<Enchantment, Integer> getEnchantmentsFromStack(ItemStack stack);
}
