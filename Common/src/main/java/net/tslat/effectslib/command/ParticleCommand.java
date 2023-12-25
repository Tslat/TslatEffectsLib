package net.tslat.effectslib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.particle.ParticleBuilder;
import net.tslat.effectslib.api.particle.positionworker.ParticlePositionWorker;
import net.tslat.effectslib.api.particle.transitionworker.ParticleTransitionWorker;
import net.tslat.effectslib.command.TELCommand;
import net.tslat.effectslib.networking.TELNetworking;
import net.tslat.effectslib.networking.packet.TELParticlePacket;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ParticleCommand implements Command<CommandSourceStack> {
	private static final Map<String, ParticlePropertyParser<?>> PROPERTIES_PARSERS = Util.make(new Object2ObjectOpenHashMap<>(11), map -> {
		map.put("amountPerPos", new ParticlePropertyParser<>("<number>", ParticlePropertyParser::parseInt, ParticleBuilder::particlesPerPosition));
		map.put("cutoffDistance", new ParticlePropertyParser<>("<distanceInBlocks>", ParticlePropertyParser::parseDouble, ParticleBuilder::cutoffDistance));
		map.put("bypassRestrictions", new ParticlePropertyParser<>("<true|false>", ParticlePropertyParser::parseBoolean, (builder, value) -> {
			if (value)
				builder.ignoreDistanceAndLimits();
		}));
		map.put("isAmbient", new ParticlePropertyParser<>("<true|false>", ParticlePropertyParser::parseBoolean, (builder, value) -> {
			if (value)
				builder.isAmbient();
		}));
		map.put("velocity", new ParticlePropertyParser<>("<x y z>", ParticlePropertyParser::parseVec3, ParticleBuilder::velocity));
		map.put("colour", new ParticlePropertyParser<>("<red green blue alpha>", ParticlePropertyParser::parseColour, ParticleBuilder::colourOverride));
		map.put("lifespan", new ParticlePropertyParser<>("<tickLifespan>", ParticlePropertyParser::parseInt, ParticleBuilder::lifespan));
		map.put("gravity", new ParticlePropertyParser<>("<gravityMod>", ParticlePropertyParser::parseFloat, ParticleBuilder::gravityOverride));
		map.put("drag", new ParticlePropertyParser<>("<dragCoefficient>", ParticlePropertyParser::parseFloat, ParticleBuilder::velocityDrag));
		map.put("scale", new ParticlePropertyParser<>("<scaleMultiplier>", ParticlePropertyParser::parseFloat, ParticleBuilder::scaleMod));
	});
	private static final ParticleCommand CMD = new ParticleCommand();

	public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("particle").requires(sender -> sender.hasPermission(2)).executes(CMD);

		RequiredArgumentBuilder<CommandSourceStack, ParticleOptions> typeArg = Commands.argument("type", ParticleArgument.particle(context));
		CommandNode<CommandSourceStack> propertiesArg = Commands.argument("amount", IntegerArgumentType.integer(1, 16384))
				.executes(ParticleCommand::spawnParticles)
				.then(Commands.literal("properties")
						.then(Commands.argument("props", StringArgumentType.string())
								.executes(ParticleCommand::spawnParticles))).build();

		for (ParticlePositionWorker.PositionType positionType : ParticlePositionWorker.PositionType.values()) {
			ArgumentBuilder<CommandSourceStack, ?> positionArg = Commands.literal(positionType.name());
			ArgumentBuilder<CommandSourceStack, ?> transitionsArg = Commands.literal("transition");

			positionArg.then(positionType.getCommandArguments(context, propertiesArg));

			for (ParticleTransitionWorker.TransitionType transitionType : ParticleTransitionWorker.TransitionType.values()) {
				ArgumentBuilder<CommandSourceStack, ?> commandSegment = transitionType.getCommandArguments(context, propertiesArg);

				if (commandSegment != null)
					transitionsArg.then(Commands.literal(transitionType.name()).then(commandSegment));
			}

			positionArg.then(positionType.getCommandArguments(context, transitionsArg.build()));
			typeArg.then(positionArg);
		}

		builder.then(Commands.literal("printproperties")
				.executes(ParticleCommand::printProperties));
		builder.then(typeArg);

		return builder;
	}

	private static int spawnParticles(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final ParticleOptions particle = ParticleArgument.getParticle(context, "type");
		final Set<String> positionTypes = Arrays.stream(ParticlePositionWorker.PositionType.values()).map(Enum::name).collect(Collectors.toSet());
		final ParticlePositionWorker.PositionType positionType = ParticlePositionWorker.PositionType.valueOf(getLiteralNode(context, positionTypes::contains).orElseThrow().getNode().getUsageText());
		final ParticleBuilder particleBuilder = ParticleBuilder.fromCommand(particle, positionType.buildFromCommand(context)).spawnNTimes(IntegerArgumentType.getInteger(context, "amount"));

		handleTransitions(context, particleBuilder);
		handleProperties(context, particleBuilder);

		TELNetworking.sendToAllPlayersInWorld(new TELParticlePacket(particleBuilder), context.getSource().getLevel());
		TELCommand.feedback(context.getSource(), "Particle", "command.tel.particle.success", TELCommand.CommandFeedbackType.SUCCESS, Component.literal(BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()).toString()), Component.literal(context.getSource().getLevel().dimension().location().toString()));

		return 1;
	}

	private static void handleTransitions(CommandContext<CommandSourceStack> context, ParticleBuilder particleBuilder) throws CommandSyntaxException {
		if (getLiteralNode(context, "transition"::contains).isEmpty())
			return;

		final Set<String> transitionTypes = Arrays.stream(ParticleTransitionWorker.TransitionType.values()).map(Enum::name).collect(Collectors.toSet());
		final ParticleTransitionWorker.TransitionType transitionType = ParticleTransitionWorker.TransitionType.valueOf(getLiteralNode(context, transitionTypes::contains).orElseThrow().getNode().getUsageText());

		particleBuilder.addTransition(transitionType.buildFromCommand(context));
	}

	private static Optional<ParsedCommandNode<CommandSourceStack>> getLiteralNode(CommandContext<CommandSourceStack> context, Predicate<String> literalNodePredicate) {
		for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
			if (literalNodePredicate.test(node.getNode().getUsageText()))
				return Optional.of(node);
		}

		return Optional.empty();
	}

	private static void handleProperties(CommandContext<CommandSourceStack> context, ParticleBuilder particleBuilder) throws CommandSyntaxException {
		try {
			final StringReader propertiesReader = new StringReader(StringArgumentType.getString(context, "props"));

			while (propertiesReader.canRead()) {
				propertiesReader.skipWhitespace();

				if (!propertiesReader.canRead())
					return;

				String property = propertiesReader.readStringUntil(':');

				if (PROPERTIES_PARSERS.containsKey(property)) {
					ParticlePropertyParser<?> parser = PROPERTIES_PARSERS.get(property);
					StringBuilder valueBuilder = new StringBuilder();

					if (propertiesReader.canRead()) {
						String read = propertiesReader.readUnquotedString();

						if (propertiesReader.canRead() && propertiesReader.peek() == ' ')
							propertiesReader.skip();

						valueBuilder.append(read);
					}

					parser.parseValue(valueBuilder.toString(), particleBuilder);
				}
			}
		}
		catch (Exception ex) {
			if (!ex.getLocalizedMessage().contains("'props'")) {
				Message exceptionMsg = Component.translatable("command.tel.particle.invalidProperties", Component.literal("?"));

				try {
					exceptionMsg = Component.translatable("command.tel.particle.invalidProperties", Component.literal(StringArgumentType.getString(context, "props")));
				}
				catch (Exception ex2) {
					ex.printStackTrace();
				}

				throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMsg), exceptionMsg);
			}
		}
	}

	private static int printProperties(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		TELCommand.feedback(context.getSource(), "Particle", "command.tel.particle.properties", TELCommand.CommandFeedbackType.INFO);

		for (Map.Entry<String, ParticlePropertyParser<?>> property : PROPERTIES_PARSERS.entrySet()) {
			context.getSource().sendSuccess(() -> Component.literal("")
					.append(Component.literal(property.getKey()).withStyle(ChatFormatting.GOLD))
					.append(Component.literal(":").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(property.getValue().formatString).withStyle(ChatFormatting.BLUE)), true);
		}

		return 1;
	}

	@Override
	public int run(CommandContext<CommandSourceStack> context) {
		TELCommand.feedback(context.getSource(), "Particle", "command.tel.particle.desc", TELCommand.CommandFeedbackType.INFO);

		return 1;
	}

	static class ParticlePropertyParser<V> {
		final String formatString;
		private final Function<String, Either<V, CommandSyntaxException>> valueParser;
		private final BiConsumer<ParticleBuilder, V> propertyAdder;

		ParticlePropertyParser(String formatString, Function<String, Either<V, CommandSyntaxException>> valueParser, BiConsumer<ParticleBuilder, V> propertyAdder) {
			this.formatString = formatString;
			this.valueParser = valueParser;
			this.propertyAdder = propertyAdder;
		}

		void parseValue(String valueString, ParticleBuilder particleBuilder) throws CommandSyntaxException {
			final Either<V, CommandSyntaxException> result = this.valueParser.apply(valueString);

			this.propertyAdder.accept(particleBuilder, result.left().orElseThrow(() -> result.right().orElseThrow()));
		}

		private static <V> Either<V, CommandSyntaxException> parseOrError(Supplier<V> parser, Function<String, CommandSyntaxException> errSupplier) {
			try {
				return Either.left(parser.get());
			}
			catch (Exception ex) {
				return Either.right(errSupplier.apply(ex.getLocalizedMessage()));
			}
		}

		protected static Either<Boolean, CommandSyntaxException> parseBoolean(String valueString) {
			return parseOrError(() -> Boolean.valueOf(valueString), ex -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedBool().createWithContext(new StringReader(ex)));
		}

		protected static Either<Integer, CommandSyntaxException> parseInt(String valueString) {
			return parseOrError(() -> Integer.valueOf(valueString), ex -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt().createWithContext(new StringReader(ex)));
		}

		protected static Either<Float, CommandSyntaxException> parseFloat(String valueString) {
			return parseOrError(() -> Float.valueOf(valueString), ex -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedFloat().createWithContext(new StringReader(ex)));
		}

		protected static Either<Double, CommandSyntaxException> parseDouble(String valueString) {
			return parseOrError(() -> Double.valueOf(valueString), ex -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().createWithContext(new StringReader(ex)));
		}

		protected static Either<Integer, CommandSyntaxException> parseColour(String valueString) {
			return parseOrError(() -> {
				String[] components = valueString.split(" ");

				return FastColor.ARGB32.color(Mth.clamp(Integer.parseInt(components[3]), 0, 255), Mth.clamp(Integer.parseInt(components[0]), 0, 255), Mth.clamp(Integer.parseInt(components[1]), 0, 255), Mth.clamp(Integer.parseInt(components[2]), 0, 255));
			}, ex -> Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(new StringReader(ex)));
		}

		protected static Either<Vec3, CommandSyntaxException> parseVec3(String valueString) {
			return parseOrError(() -> {
				String[] components = valueString.split(" ");

				return new Vec3(Double.parseDouble(components[0]), Double.parseDouble(components[1]), Double.parseDouble(components[2]));
			}, ex -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt().createWithContext(new StringReader(ex)));
		}
	}
}