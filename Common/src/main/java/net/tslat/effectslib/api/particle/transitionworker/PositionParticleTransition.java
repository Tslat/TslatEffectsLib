package net.tslat.effectslib.api.particle.transitionworker;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.api.util.CommandSegmentHandler;

import java.util.function.Function;

/**
 * Particle transition worker that moves the particle from its start position to the target position over the particle's lifespan or an optionally configurable length of time.
 * <p>Optionally stop moving if the particle collides with something</p>
 */
public class PositionParticleTransition implements ParticleTransitionWorker<PositionParticleTransition> {
    private final Vec3 toPos;
    private final int transitionTime;
    private final boolean stopOnCollision;

    private Function<Object, Vec3> startPositions;

    private PositionParticleTransition(Vec3 toPos, int transitionTime, boolean stopOnCollision) {
        this.toPos = toPos;
        this.transitionTime = transitionTime;
        this.stopOnCollision = stopOnCollision;
    }

    public static PositionParticleTransition create(Vec3 toPos, int transitionTime, boolean stopOnCollision) {
        return new PositionParticleTransition(toPos, transitionTime, stopOnCollision);
    }

    public static PositionParticleTransition create(Vec3 toPos, int transitionTime) {
        return create(toPos, transitionTime, false);
    }

    public static PositionParticleTransition create(Vec3 toPos, boolean stopOnCollision) {
        return create(toPos, -1, stopOnCollision);
    }

    public static PositionParticleTransition create(Vec3 toPos) {
        return create(toPos, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.POSITION_TRANSITION;
    }

    static PositionParticleTransition decode(FriendlyByteBuf buffer) {
        return new PositionParticleTransition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.toPos.x);
        buffer.writeDouble(this.toPos.y);
        buffer.writeDouble(this.toPos.z);
        buffer.writeVarInt(this.transitionTime);
        buffer.writeBoolean(this.stopOnCollision);
    }

    @Override
    public boolean tick(Object particle) {
        return TELClient.particlePositionTransitionTick(particle, this.startPositions, this.toPos, this.transitionTime, this.stopOnCollision, function -> this.startPositions = function);
    }

    public static class CommandSegment implements CommandSegmentHandler<PositionParticleTransition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("to_pos", Vec3Argument.vec3())
                    .then(forward)
                    .then(Commands.argument("transition_time", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                            .then(forward))
                    .then(Commands.argument("stop_on_collision", BoolArgumentType.bool())
                            .then(forward)
                            .then(Commands.argument("transition_time", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                    .then(forward)));
        }

        @Override
        public PositionParticleTransition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            boolean stopOnColllision = false;
            int transitionTime = -1;

            try {
                transitionTime = IntegerArgumentType.getInteger(context, "transition_time");
            }
            catch (Exception ignored) {}

            try {
                stopOnColllision = BoolArgumentType.getBool(context, "stop_on_collision");
            }
            catch (Exception ignored) {}

            return new PositionParticleTransition(Vec3Argument.getVec3(context, "to_pos"), transitionTime, stopOnColllision);
        }
    }
}