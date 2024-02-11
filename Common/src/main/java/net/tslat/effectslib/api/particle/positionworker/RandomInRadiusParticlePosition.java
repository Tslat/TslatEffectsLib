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
 * Particle position handler that generates random position(s) in a lateral circle in a given radius around a target position.
 */
public class RandomInRadiusParticlePosition implements ParticlePositionWorker<RandomInRadiusParticlePosition> {
    private final Vec3 origin;
    private final double radius;

    private RandomInRadiusParticlePosition(Vec3 origin, double radius) {
        this.origin = origin;
        this.radius = radius;
    }

    public static RandomInRadiusParticlePosition create(Vec3 origin, double radius) {
        return new RandomInRadiusParticlePosition(origin, radius);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_IN_RADIUS;
    }

    static RandomInRadiusParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomInRadiusParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readDouble());
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
        int attempts = 0;
        Vec3 pos = this.origin.add(random.nextGaussian() * this.radius, 0, random.nextGaussian() * this.radius);

        while (attempts++ < 10) {
            if (pos.distanceToSqr(this.origin) <= this.radius * this.radius)
                break;

            double radiusX = this.origin.x - pos.x;
            double radiusZ = this.origin.z - pos.z;

            pos = this.origin.add(random.nextDouble() * 0.95f * radiusX, 0, random.nextDouble() * 0.95f * radiusZ);
        }

        return pos;
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomInRadiusParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("center", Vec3Argument.vec3())
                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, Double.MAX_VALUE))
                            .then(forward));
        }

        @Override
        public RandomInRadiusParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomInRadiusParticlePosition(Vec3Argument.getVec3(context, "center"), DoubleArgumentType.getDouble(context, "radius"));
        }
    }
}