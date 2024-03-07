package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) at the edge of a circle of a given radius around a target position.
 */
public class RandomAtCircleEdgeParticlePosition implements ParticlePositionWorker<RandomAtCircleEdgeParticlePosition> {
    private final Vec3 origin;
    private final Vec3 angle;
    private final double radius;

    RandomAtCircleEdgeParticlePosition(Vec3 origin, Vec3 angle, double radius) {
        this.origin = origin;
        this.angle = angle.lengthSqr() == 0 ? Vec3.ZERO : angle;
        this.radius = radius;
    }

    public static RandomAtCircleEdgeParticlePosition create(Vec3 origin, Vec3 angle, double radius) {
        return new RandomAtCircleEdgeParticlePosition(origin, angle, radius);
    }

    public static RandomAtCircleEdgeParticlePosition create(Vec3 origin, double radius) {
        return new RandomAtCircleEdgeParticlePosition(origin, Vec3.ZERO, radius);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_AT_CIRCLE_EDGE;
    }

    static RandomAtCircleEdgeParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomAtCircleEdgeParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readDouble());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
        buffer.writeDouble(this.angle.x);
        buffer.writeDouble(this.angle.y);
        buffer.writeDouble(this.angle.z);
        buffer.writeDouble(this.radius);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        final double randomDirection = Mth.TWO_PI * random.nextDouble();

        if (this.angle == Vec3.ZERO)
            return this.origin.add(Math.cos(randomDirection) * this.radius, 0, Math.sin(randomDirection) * this.radius);

        final double yawLength = this.angle.horizontalDistance();
        final double yaw = Math.cos(randomDirection) * this.radius;
        final double pitch = Math.sin(randomDirection) * this.radius;

        return this.origin.add(
                (yaw * (Math.sinh(-this.angle.z) / yawLength)) - (pitch * this.angle.y * (this.angle.x / yawLength)),
                pitch * yawLength,
                (yaw * (Math.sinh(this.angle.x) / yawLength)) - (pitch * this.angle.y * (this.angle.z / yawLength)));
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomAtCircleEdgeParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("center", Vec3Argument.vec3(false))
                    .then(Commands.argument("angle", Vec3Argument.vec3(false))
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                                    .then(Commands.argument("particles_for_circle", IntegerArgumentType.integer(0, 16384))
                                            .then(forward))))
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                            .then(Commands.argument("particles_for_circle", IntegerArgumentType.integer(0, 16384))
                                    .then(forward)));
        }

        @Override
        public RandomAtCircleEdgeParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Vec3 angle = Vec3.ZERO;

            try {
                Coordinates coordinates = Vec3Argument.getCoordinates(context, "angle");
                angle = coordinates.getPosition(context.getSource());

                if (coordinates instanceof LocalCoordinates)
                    angle = angle.subtract(context.getSource().getAnchor().apply(context.getSource())).normalize();
            }
            catch (Exception ignored) {}

            return new RandomAtCircleEdgeParticlePosition(Vec3Argument.getVec3(context, "center"), angle, DoubleArgumentType.getDouble(context, "radius"));
        }
    }
}