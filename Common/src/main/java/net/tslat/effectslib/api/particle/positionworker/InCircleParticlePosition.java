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

import java.util.Iterator;

/**
 * Particle position handler that generates in the shape of a circle of a given radius around a target position.
 */
public class InCircleParticlePosition implements ParticlePositionWorker<InCircleParticlePosition> {
    private final Vec3 origin;
    private final Vec3 angle;
    private final double radius;
    private final int totalParticles;

    private Iterator<Vec3> positionIterator = null;

    InCircleParticlePosition(Vec3 origin, Vec3 angle, double radius, int totalParticles) {
        this.origin = origin;
        this.angle = angle.lengthSqr() == 0 ? Vec3.ZERO : angle;
        this.radius = radius;
        this.totalParticles = totalParticles;
    }

    public static InCircleParticlePosition create(Vec3 origin, Vec3 angle, double radius, int totalParticles) {
        return new InCircleParticlePosition(origin, angle, radius, totalParticles);
    }

    public static InCircleParticlePosition create(Vec3 origin, double radius, int totalParticles) {
        return new InCircleParticlePosition(origin, Vec3.ZERO, radius, totalParticles);
    }

    @Override
    public PositionType type() {
        return PositionType.IN_CIRCLE;
    }

    static InCircleParticlePosition decode(FriendlyByteBuf buffer) {
        return new InCircleParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readDouble(), buffer.readVarInt());
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
        buffer.writeVarInt(this.totalParticles);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        return this.positionIterator.next();
    }

    private void checkCache(final Level level) {
        if (this.positionIterator == null || !this.positionIterator.hasNext()) {
            this.positionIterator = new Iterator<>() {
                private final double increment = Mth.TWO_PI / InCircleParticlePosition.this.totalParticles;
                private final double yawLength = InCircleParticlePosition.this.angle.horizontalDistance();
                private double theta = 0;

                @Override
                public boolean hasNext() {
                    return this.theta < Mth.TWO_PI;
                }

                @Override
                public Vec3 next() {
                    double circleAngle = this.theta;
                    this.theta += this.increment;

                    if (InCircleParticlePosition.this.angle == Vec3.ZERO)
                        return InCircleParticlePosition.this.origin.add(Math.cos(circleAngle) * InCircleParticlePosition.this.radius, 0, Math.sin(circleAngle) * InCircleParticlePosition.this.radius);

                    double yaw = Math.cos(circleAngle) * InCircleParticlePosition.this.radius;
                    double pitch = Math.sin(circleAngle) * InCircleParticlePosition.this.radius;

                    return InCircleParticlePosition.this.origin.add(
                            (yaw * (Math.sinh(-InCircleParticlePosition.this.angle.z) / this.yawLength)) - (pitch * InCircleParticlePosition.this.angle.y * (InCircleParticlePosition.this.angle.x / this.yawLength)),
                            pitch * this.yawLength,
                            (yaw * (Math.sinh(InCircleParticlePosition.this.angle.x) / this.yawLength)) - (pitch * InCircleParticlePosition.this.angle.y * (InCircleParticlePosition.this.angle.z / this.yawLength)));
                }
            };
        }
    }

    public static class CommandSegment implements CommandSegmentHandler<InCircleParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("center", Vec3Argument.vec3())
                    .then(Commands.argument("angle", Vec3Argument.vec3())
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                                    .then(Commands.argument("particles_for_circle", IntegerArgumentType.integer(0, 16384))
                                            .then(forward))))
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                            .then(Commands.argument("particles_for_circle", IntegerArgumentType.integer(0, 16384))
                                    .then(forward)));
        }

        @Override
        public InCircleParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Vec3 angle = Vec3.ZERO;

            try {
                Coordinates coordinates = Vec3Argument.getCoordinates(context, "angle");
                angle = coordinates.getPosition(context.getSource());

                if (coordinates instanceof LocalCoordinates)
                    angle = angle.subtract(context.getSource().getAnchor().apply(context.getSource())).normalize();
            }
            catch (Exception ignored) {}

            return new InCircleParticlePosition(Vec3Argument.getVec3(context, "center"), angle, DoubleArgumentType.getDouble(context, "radius"), IntegerArgumentType.getInteger(context, "particles_for_circle"));
        }
    }
}