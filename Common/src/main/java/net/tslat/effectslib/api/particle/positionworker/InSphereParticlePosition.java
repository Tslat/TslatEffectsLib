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
 * Particle position handler that generates positions forming a sphere of a given radius around a target position.
 */
public class InSphereParticlePosition implements ParticlePositionWorker<InSphereParticlePosition> {
    private final Vec3 origin;
    private final double radius;
    private final int granularity;

    private Iterator<Vec3> positionIterator = null;

    InSphereParticlePosition(Vec3 origin, double radius, int granularity) {
        this.origin = origin;
        this.radius = radius;
        this.granularity = granularity;
    }

    public static InSphereParticlePosition create(Vec3 origin, double radius, int granularity) {
        return new InSphereParticlePosition(origin, radius, granularity);
    }

    @Override
    public PositionType type() {
        return PositionType.IN_SPHERE;
    }

    static InSphereParticlePosition decode(FriendlyByteBuf buffer) {
        return new InSphereParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readDouble(), buffer.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
        buffer.writeDouble(this.radius);
        buffer.writeVarInt(this.granularity);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        return this.positionIterator.next();
    }

    @Override
    public int getParticleCountForSumOfPositions() {
        return this.granularity * this.granularity * 2;
    }

    private void checkCache(final Level level) {
        if (this.positionIterator == null || !this.positionIterator.hasNext()) {
            this.positionIterator = new Iterator<>() {
                private final double increment = Mth.PI / InSphereParticlePosition.this.granularity;
                private double theta = -Mth.HALF_PI;
                private double phi = 0;

                @Override
                public boolean hasNext() {
                    return this.theta < Mth.HALF_PI || this.phi < Mth.TWO_PI;
                }

                @Override
                public Vec3 next() {
                    double currentTheta = this.theta;
                    double currentPhi = this.phi;

                    if (this.phi >= Mth.TWO_PI) {
                        this.theta += this.increment;
                        this.phi = 0;
                    }

                    this.phi += this.increment;

                    return InSphereParticlePosition.this.origin.add(Math.cos(currentTheta) * Math.sin(currentPhi) * InSphereParticlePosition.this.radius, Math.cos(currentTheta) * Math.cos(currentPhi) * InSphereParticlePosition.this.radius, Math.sin(currentTheta) * InSphereParticlePosition.this.radius);
                }
            };
        }
    }

    public static class CommandSegment implements CommandSegmentHandler<InSphereParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("center", Vec3Argument.vec3())
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                            .then(Commands.argument("granularity", IntegerArgumentType.integer(0, 16384 / 2))
                                    .then(forward)));
        }

        @Override
        public InSphereParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new InSphereParticlePosition(Vec3Argument.getVec3(context, "center"), DoubleArgumentType.getDouble(context, "radius"), IntegerArgumentType.getInteger(context, "granularity"));
        }
    }
}