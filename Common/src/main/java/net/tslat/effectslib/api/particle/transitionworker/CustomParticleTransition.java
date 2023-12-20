package net.tslat.effectslib.api.particle.transitionworker;

import net.minecraft.client.particle.Particle;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiConsumer;

/**
 * Particle transition handler that just hosts a callback for custom handling, saving custom tick handling for modifying the particle over the particle's lifespan or an optionally configurable length of time.
 * <p><b><u>MUST ONLY BE USED ON THE CLIENT</u></b></p>
 * <br>
 * <p>Consumer is provided the float percentage of the defined transition time that has passed and the particle itself</p>
 */
public class CustomParticleTransition implements ParticleTransitionWorker<CustomParticleTransition> {
    private final BiConsumer<Float, Particle> particleConsumer;
    private final int transitionTime;

    private CustomParticleTransition(BiConsumer<Float, Particle> consumer, int transitionTime) {
        this.particleConsumer = consumer;
        this.transitionTime = transitionTime;
    }

    public static CustomParticleTransition create(BiConsumer<Float, Particle> consumer, int transitionTime) {
        return new CustomParticleTransition(consumer, transitionTime);
    }

    public static CustomParticleTransition create(BiConsumer<Float, Particle> consumer) {
        return create(consumer, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.CUSTOM_TRANSITION;
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
        final Particle particle = (Particle)obj;

        this.particleConsumer.accept(Math.min(1f, particle.age / (float)(this.transitionTime == -1 ? particle.getLifetime() : this.transitionTime)), particle);

        return particle.isAlive();
    }
}