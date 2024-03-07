package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) on the surface of a sphere of a given radius around a target position.
 */
public class RandomAtSphereEdgeParticlePosition implements ParticlePositionWorker<RandomAtSphereEdgeParticlePosition> {
    private final Vec3 origin;
    private final double radius;

    RandomAtSphereEdgeParticlePosition(Vec3 origin, double radius) {
        this.origin = origin;
        this.radius = radius;
    }

    public static RandomAtSphereEdgeParticlePosition create(Vec3 origin, double radius) {
        return new RandomAtSphereEdgeParticlePosition(origin, radius);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_AT_SPHERE_EDGE;
    }

    static RandomAtSphereEdgeParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomAtSphereEdgeParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readDouble());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
        buffer.writeDouble(this.radius);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        Vec3 angle = Vec3.ZERO;

        while (angle.x == angle.y && angle.y == angle.z && angle.z == 0) {
            angle = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
        }

        return this.origin.add(angle.scale(this.radius));
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomAtSphereEdgeParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("center", Vec3Argument.vec3(false))
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                            .then(forward));
        }

        @Override
        public RandomAtSphereEdgeParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomAtSphereEdgeParticlePosition(Vec3Argument.getVec3(context, "center"), DoubleArgumentType.getDouble(context, "radius"));
        }
    }
}