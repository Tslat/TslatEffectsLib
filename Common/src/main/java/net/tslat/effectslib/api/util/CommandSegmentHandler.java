package net.tslat.effectslib.api.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * Generified command argument provider that allows for an external object to generate a subsection of a command's arguments then pass it on to the root command
 * <p>Primarily used for {@link net.tslat.effectslib.api.particle.positionworker.ParticlePositionWorker PositionWorkers} and {@link net.tslat.effectslib.api.particle.transitionworker.ParticleTransitionWorker TransitionWorkers}</p>
 */
public interface CommandSegmentHandler<O> {
    ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward);
    O createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
}