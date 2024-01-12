/*
package net.tslat.effectslib.api.particle.transitionworker;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;

import java.util.function.Function;

*/
/**
 * Particle transition worker that adjusts the particle's position to circle a given position or entity over the particle's lifespan or an optionally configurable length of time.
 * <p>If the particle lasts longer than the transition time, the circling continues from the start</p>
 * <p>Optionally stop moving if the particle collides with something</p>
 *//*

public class CirclingPositionParticleTransition implements ParticleTransitionWorker<CirclingPositionParticleTransition> {
    private final Vec3 origin;
    private final Vec2 rotationAngle;
    private final boolean stopOnCollision;
    private final int transitionTime;
    private long killTick = -1;

    private Function<Object, Pair<Vec3, Double>> startOffsets;

    private CirclingPositionParticleTransition(Vec3 origin, Vec2 rotationAngle, boolean stopOnCollision, int transitionTime) {
        this.origin = origin;
        this.rotationAngle = rotationAngle.normalized();
        this.stopOnCollision = stopOnCollision;
        this.transitionTime = transitionTime;
    }

    public static CirclingPositionParticleTransition create(Vec3 origin, Vec2 rotationAngle, boolean stopOnCollision, int transitionTime) {
        return new CirclingPositionParticleTransition(origin, rotationAngle, stopOnCollision, transitionTime);
    }

    public static CirclingPositionParticleTransition create(Vec3 origin, Vec2 rotationAngle, int transitionTime) {
        return create(origin, rotationAngle, false, transitionTime);
    }

    public static CirclingPositionParticleTransition create(Vec3 origin, Vec2 rotationAngle, boolean stopOnCollision) {
        return create(origin, rotationAngle, stopOnCollision, -1);
    }

    public static CirclingPositionParticleTransition create(Vec3 origin, Vec2 rotationAngle) {
        return create(origin, rotationAngle, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.CIRCLING_POSITION;
    }

    @Override
    public long getKillTick() {
        return this.killTick;
    }

    static CirclingPositionParticleTransition decode(FriendlyByteBuf buffer) {
        if (!shouldBeAlive())
            return false;

        return new CirclingPositionParticleTransition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec2(buffer.readFloat(), buffer.readFloat()), buffer.readBoolean(), buffer.readVarInt(), this.killTick, killTick -> this.killTick = killTick);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
        buffer.writeFloat(this.rotationAngle.x);
        buffer.writeFloat(this.rotationAngle.y);
        buffer.writeBoolean(this.stopOnCollision);
        buffer.writeVarInt(this.transitionTime);
    }

    @Override
    public boolean tick(Object particle) {
        return TELClient.particleCirclingPositionTransitionTick(particle, startOffsets, this.origin, this.rotationAngle, this.stopOnCollision, this.transitionTime, function -> this.startOffsets = function);
    }
}*/
