package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A particle position handler to go with {@link net.tslat.effectslib.api.particle.ParticleBuilder}.
 * <p>The idea behind this is that it allows the particle builder to defer full computation of the particle position(s) until the client is about to spawn them.</p>
 * <p>This reduces network bandwidth and packet data, and allows for more interesting particle deployment</p>
 */
public interface ParticlePositionWorker<T extends ParticlePositionWorker<T>> {
    void toNetwork(FriendlyByteBuf buffer);

    /**
     * Generate a new position for this particle position provider using the provided {@link RandomSource}
     * <p>Must always return a position, even if using a pre-defined or limited set of locations</p>
     */
    @NotNull
    Vec3 supplyPosition(Level level, RandomSource random);
    PositionType type();

    enum PositionType {
        CUSTOM(CustomParticlePosition::decode, new CustomParticlePosition.CommandSegment()),
        RANDOM_IN_BLOCK(RandomInBlockParticlePosition::decode, new RandomInBlockParticlePosition.CommandSegment()),
        RANDOM_IN_LINE(RandomInLineParticlePosition::decode, new RandomInLineParticlePosition.CommandSegment()),
        RANDOM_IN_ENTITY(RandomInEntityParticlePosition::decode, new RandomInEntityParticlePosition.CommandSegment()),
        RANDOM_IN_BOUNDS(RandomInBoundsParticlePosition::decode, new RandomInBoundsParticlePosition.CommandSegment()),
        RANDOM_IN_RADIUS(RandomInRadiusParticlePosition::decode, new RandomInRadiusParticlePosition.CommandSegment()),
        RANDOM_IN_SPHERE(RandomInSphereParticlePosition::decode, new RandomInSphereParticlePosition.CommandSegment()),
        RANDOM_AT_SPHERE_EDGE(RandomAtSphereEdgeParticlePosition::decode, new RandomAtSphereEdgeParticlePosition.CommandSegment()),
        RANDOM_AT_CIRCLE_EDGE(RandomAtCircleEdgeParticlePosition::decode, new RandomAtCircleEdgeParticlePosition.CommandSegment()),
        RANDOM_AT_BOUNDS_EDGE(RandomAtBoundsEdgeParticlePosition::decode, new RandomAtBoundsEdgeParticlePosition.CommandSegment()),
        IN_LINE(InLineParticlePosition::decode, new InLineParticlePosition.CommandSegment()),
        IN_CIRCLE(InCircleParticlePosition::decode, new InCircleParticlePosition.CommandSegment()),
        IN_SPHERE(InSphereParticlePosition::decode, new InSphereParticlePosition.CommandSegment());

        private final Function<FriendlyByteBuf, ParticlePositionWorker<?>> decoder;
        private final CommandSegmentHandler<?> commandSegmentHandler;

        PositionType(Function<FriendlyByteBuf, ParticlePositionWorker<?>> decoder, CommandSegmentHandler<?> commandSegmentHandler) {
            this.decoder = decoder;
            this.commandSegmentHandler = commandSegmentHandler;
        }

        public ParticlePositionWorker<?> constructFromNetwork(FriendlyByteBuf buffer) {
            return this.decoder.apply(buffer);
        }

        public ArgumentBuilder<CommandSourceStack, ?> getCommandArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return this.commandSegmentHandler.constructArguments(context, forward);
        }

        public ParticlePositionWorker<?> buildFromCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return (ParticlePositionWorker)this.commandSegmentHandler.createFromArguments(context);
        }
    }
}