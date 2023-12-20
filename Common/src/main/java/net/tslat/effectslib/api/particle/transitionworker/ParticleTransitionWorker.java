package net.tslat.effectslib.api.particle.transitionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * A particle transition handler to go with {@link net.tslat.effectslib.api.particle.ParticleBuilder}.
 * <p>Allows for server-side handling of some limited lifetime-modification of particles</p>
 */
public interface ParticleTransitionWorker<T extends ParticleTransitionWorker<T>> {
    void toNetwork(FriendlyByteBuf buffer);

    /**
     * Apply the relevant transition changes to the provided particle for this tick.
     * <p>Transition time should be based on the particle's age</p>
     * <p>Particle is left as Object to avoid classloading, can be safely cast in a client-only class as needed</p>
     * @return whether the transition should continue to be applied or not (usually in the event of the particle's removal)
     */
    boolean tick(Object particle);
    TransitionType type();

    /**
     * Get the float percentage that the transition worker is through its transition based on the defined transition time and particle's lifespan
     */
    static float getTransitionProgress(int particleAge, int particleLifespan, int transitionTime) {
        return Math.min(1f, particleAge / (float)(transitionTime == -1 ? particleLifespan : transitionTime));
    }

    enum TransitionType {
        CUSTOM_TRANSITION(CustomParticleTransition::decode, null),
        COLOUR_TRANSITION(ColourParticleTransition::decode, new ColourParticleTransition.CommandSegment()),
        POSITION_TRANSITION(PositionParticleTransition::decode, new PositionParticleTransition.CommandSegment()),
        SCALE_TRANSITION(ScaleParticleTransition::decode, new ScaleParticleTransition.CommandSegment()),
        FOLLOW_ENTITY(FollowEntityParticleTransition::decode, new FollowEntityParticleTransition.CommandSegment()),
        VELOCITY_TRANSITION(VelocityParticleTransition::decode, new VelocityParticleTransition.CommandSegment())/*,
        CIRCLING_POSITION(CirclingPositionParticleTransition::decode),
        CIRCLING_ENTITY(CirclingEntityParticleTransition::decode)*/;

        private final Function<FriendlyByteBuf, ParticleTransitionWorker<?>> decoder;
        private final CommandSegmentHandler<?> commandSegmentHandler;

        TransitionType(Function<FriendlyByteBuf, ParticleTransitionWorker<?>> decoder, CommandSegmentHandler<?> commandSegmentHandler) {
            this.decoder = decoder;
            this.commandSegmentHandler = commandSegmentHandler;
        }

        public ParticleTransitionWorker<?> constructFromNetwork(FriendlyByteBuf buffer) {
            return this.decoder.apply(buffer);
        }

        @Nullable
        public ArgumentBuilder<CommandSourceStack, ?> getCommandArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            if (this.commandSegmentHandler == null)
                return null;

            return this.commandSegmentHandler.constructArguments(context, forward);
        }

        @Nullable
        public ParticleTransitionWorker<?> buildFromCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            if (this.commandSegmentHandler == null)
                return null;

            return (ParticleTransitionWorker)this.commandSegmentHandler.createFromArguments(context);
        }
    }
}
