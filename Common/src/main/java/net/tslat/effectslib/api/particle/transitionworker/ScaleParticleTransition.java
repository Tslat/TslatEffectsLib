package net.tslat.effectslib.api.particle.transitionworker;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.api.util.CommandSegmentHandler;

import java.util.function.Function;

/**
 * Particle transition handler that transitions to a target scale value over the particle's lifespan or an optionally configurable length of time.
 */
public class ScaleParticleTransition implements ParticleTransitionWorker<ScaleParticleTransition> {
    private final float toScale;
    private final int transitionTime;

    private Function<Object, Float> startScales;

    private ScaleParticleTransition(float toScale, int transitionTime) {
        this.toScale = toScale;
        this.transitionTime = transitionTime;
    }

    public static ScaleParticleTransition create(float toScale, int transitionTime) {
        return new ScaleParticleTransition(toScale, transitionTime);
    }

    public static ScaleParticleTransition create(float toScale) {
        return create(toScale, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.SCALE_TRANSITION;
    }

    static ScaleParticleTransition decode(FriendlyByteBuf buffer) {
        return new ScaleParticleTransition(buffer.readFloat(), buffer.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.toScale);
        buffer.writeVarInt(this.transitionTime);
    }

    @Override
    public boolean tick(Object particle) {
        return TELClient.particleScaleTransitionTick(particle, this.startScales, this.toScale, this.transitionTime, function -> this.startScales = function);
    }

    public static class CommandSegment implements CommandSegmentHandler<ScaleParticleTransition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("to_scale", FloatArgumentType.floatArg(0, Float.MAX_VALUE))
                    .then(forward)
                    .then(Commands.argument("transition_time", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                            .then(forward));
        }

        @Override
        public ScaleParticleTransition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            int transitionTime = -1;

            try {
                transitionTime = IntegerArgumentType.getInteger(context, "transition_time");
            }
            catch (Exception ignored) {}

            return new ScaleParticleTransition(FloatArgumentType.getFloat(context, "to_scale"), transitionTime);
        }
    }
}