package net.tslat.effectslib.api.particle.transitionworker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.FastColor;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.api.util.CommandSegmentHandler;

import java.util.function.Function;

/**
 * Particle transition handler that fades to the target colour over the particle's lifespan or an optionally configurable length of time.
 * <p>Uses ARGB format</p>
 */
public class ColourParticleTransition implements ParticleTransitionWorker<ColourParticleTransition> {
    private final int toColour;
    private final int transitionTime;
    private long killTick = -1;

    private Function<Object, Integer> startColours;

    private ColourParticleTransition(int toColour, int transitionTime) {
        this.toColour = toColour;
        this.transitionTime = transitionTime;
    }

    public static ColourParticleTransition create(int toColour, int transitionTime) {
        return new ColourParticleTransition(toColour, transitionTime);
    }

    public static ColourParticleTransition create(int toColour) {
        return create(toColour, -1);
    }

    @Override
    public TransitionType type() {
        return TransitionType.COLOUR_TRANSITION;
    }

    @Override
    public long getKillTick() {
        return this.killTick;
    }

    static ColourParticleTransition decode(FriendlyByteBuf buffer) {
        return new ColourParticleTransition(buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.toColour);
        buffer.writeVarInt(this.transitionTime);
    }

    @Override
    public boolean tick(Object particle) {
        if (!shouldBeAlive())
            return false;

        return TELClient.particleColourTransitionTick(particle, this.startColours, this.toColour, this.transitionTime, function -> this.startColours = function, this.killTick, killTick -> this.killTick = killTick);
    }

    public static class CommandSegment implements CommandSegmentHandler<ColourParticleTransition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("to_colour_red", IntegerArgumentType.integer(0, 255))
                    .then(Commands.argument("to_colour_green", IntegerArgumentType.integer(0, 255))
                            .then(Commands.argument("to_colour_blue", IntegerArgumentType.integer(0, 255))
                                    .then(Commands.argument("to_colour_alpha", IntegerArgumentType.integer(0, 255))
                                            .then(forward)
                                            .then(Commands.argument("transition_time", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                                    .then(forward)))));
        }

        @Override
        public ColourParticleTransition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            int transitionTime = -1;

            try {
                transitionTime = IntegerArgumentType.getInteger(context, "transition_time");
            }
            catch (Exception ignored) {}

            return new ColourParticleTransition(FastColor.ARGB32.color(IntegerArgumentType.getInteger(context, "to_colour_alpha"), IntegerArgumentType.getInteger(context, "to_colour_red"), IntegerArgumentType.getInteger(context, "to_colour_green"), IntegerArgumentType.getInteger(context, "to_colour_blue")), transitionTime);
        }
    }
}