package net.tslat.effectslib.api.particle.transitionworker;

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
 * Particle transition worker that adjusts the particle's velocity over the particle's lifespan or an optionally configurable length of time.
 */
public class VelocityParticleTransition implements ParticleTransitionWorker<VelocityParticleTransition> {
    private final Vec3 toVelocity;
    private final int transitionTime;
    private long killTick = -1;

    private Function<Object, Vec3> startVelocities;

    private VelocityParticleTransition(Vec3 toVelocity, int transitionTime) {
        this.toVelocity = toVelocity;
        this.transitionTime = transitionTime;
    }

    public static VelocityParticleTransition create(Vec3 toPos, int transitionTime) {
        return new VelocityParticleTransition(toPos, transitionTime);
    }

    public static VelocityParticleTransition create(Vec3 toPos) {
        return create(toPos, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.VELOCITY_TRANSITION;
    }

    @Override
    public long getKillTick() {
        return this.killTick;
    }

    static VelocityParticleTransition decode(FriendlyByteBuf buffer) {
        return new VelocityParticleTransition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.toVelocity.x);
        buffer.writeDouble(this.toVelocity.y);
        buffer.writeDouble(this.toVelocity.z);
        buffer.writeVarInt(this.transitionTime);
    }

    @Override
    public boolean tick(Object particle) {
        if (!shouldBeAlive())
            return false;

        return TELClient.particleVelocityTransitionTick(particle, startVelocities, this.toVelocity, this.transitionTime, function -> this.startVelocities = function, this.killTick, killTick -> this.killTick = killTick);
    }

    public static class CommandSegment implements CommandSegmentHandler<VelocityParticleTransition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("to_velocity", Vec3Argument.vec3(false))
                    .then(forward)
                    .then(Commands.argument("transition_time", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                            .then(forward));
        }

        @Override
        public VelocityParticleTransition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            int transitionTime = -1;

            try {
                transitionTime = IntegerArgumentType.getInteger(context, "transition_time");
            }
            catch (Exception ignored) {}

            return new VelocityParticleTransition(Vec3Argument.getVec3(context, "to_velocity"), transitionTime);
        }
    }
}