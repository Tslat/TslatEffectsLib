package net.tslat.effectslib.api.particle.transitionworker;

import net.minecraft.client.particle.Particle;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.LongConsumer;

/**
 * Particle transition handler that just hosts a callback for custom handling, saving custom tick handling for modifying the particle over the particle's lifespan or an optionally configurable length of time.
 * <p><b><u>MUST ONLY BE USED ON THE CLIENT</u></b></p>
 * <br>
 * <p>Consumer is provided the float percentage of the defined transition time that has passed and the particle itself</p>
 */
public class CustomParticleTransition implements ParticleTransitionWorker<CustomParticleTransition> {
    private final Handler handler;
    private final int transitionTime;
    private long killTick = -1;

    private CustomParticleTransition(Handler handler, int transitionTime) {
        this.handler = handler;
        this.transitionTime = transitionTime;
    }

    public static CustomParticleTransition create(Handler consumer, int transitionTime) {
        return new CustomParticleTransition(consumer, transitionTime);
    }

    public static CustomParticleTransition create(Handler consumer) {
        return create(consumer, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.CUSTOM_TRANSITION;
    }

    @Override
    public long getKillTick() {
        return this.killTick;
    }

    static CustomParticleTransition decode(FriendlyByteBuf buffer) {
        return null;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        throw new IllegalStateException("Custom position particle transition must not be used on the server side!");
    }

    @Override
    public boolean tick(Object obj) {
        if (!shouldBeAlive())
            return false;
        final Particle particle = (Particle)obj;

        this.handler.tick(ParticleTransitionWorker.getTransitionProgress(particle.age, particle.getLifetime(), this.transitionTime), particle, this.killTick, killTick -> this.killTick = killTick);

        return particle.isAlive();
    }

    @FunctionalInterface
    public interface Handler {
        void tick(float transitionTime, Particle particle, long killTick, LongConsumer killTickConsumer);
    }
}