package net.tslat.effectslib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Locale;

/**
 * Base command for all TslatEffectsLib subcommands
 */
public final class TELCommand {
    public static void registerSubcommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        final LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("tel")
                .then(ParticleCommand.register(dispatcher, context));

        dispatcher.register(cmd);
    }

    public enum CommandFeedbackType {
        INFO(ChatFormatting.GRAY),
        SUCCESS(ChatFormatting.GREEN),
        WARN(ChatFormatting.RED),
        ERROR(ChatFormatting.DARK_RED);

        private final ChatFormatting colour;

        CommandFeedbackType(ChatFormatting colour) {
            this.colour = colour;
        }

        public ChatFormatting getColour() {
            return colour;
        }
    }

    public static MutableComponent getCmdPrefix(String cmdName) {
        return Component.literal("")
                .append(Component.literal("[TEL").withStyle(ChatFormatting.DARK_RED)
                .append(Component.translatable("command.tel.prefix." + cmdName.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED)));
    }

    public static void feedback(CommandSourceStack source, String commandName, String langKey, CommandFeedbackType type, Component... args) {
        source.sendSuccess(() -> TELCommand.getCmdPrefix(commandName).append(" ").append(Component.translatable(langKey, (Object[])args).setStyle(Style.EMPTY.applyFormat(type.getColour()))), true);
    }

    public static void error(CommandSourceStack source, String commandName, String langKey, Component... args) {
        source.sendFailure(TELCommand.getCmdPrefix(commandName).append(" ").append(Component.translatable(langKey, (Object[])args).setStyle(Style.EMPTY.applyFormat(CommandFeedbackType.ERROR.getColour()))));
    }
}