package net.tslat.effectslib.api.particle.positionworker;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) in a given axis-aligned bounding box
 */
public class RandomInBoundsParticlePosition implements ParticlePositionWorker<RandomInBoundsParticlePosition> {
    private final AABB bounds;

    private RandomInBoundsParticlePosition(AABB bounds) {
        this.bounds = bounds;
    }

    public static RandomInBoundsParticlePosition create(AABB bounds) {
        return new RandomInBoundsParticlePosition(bounds);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_IN_BOUNDS;
    }

    static RandomInBoundsParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomInBoundsParticlePosition(new AABB(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat((float)this.bounds.minX);
        buffer.writeFloat((float)this.bounds.minY);
        buffer.writeFloat((float)this.bounds.minZ);
        buffer.writeFloat((float)this.bounds.maxX);
        buffer.writeFloat((float)this.bounds.maxY);
        buffer.writeFloat((float)this.bounds.maxZ);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        return new Vec3(
                random.nextFloat() * (this.bounds.maxX - this.bounds.minX) + this.bounds.minX,
                random.nextFloat() * (this.bounds.maxY - this.bounds.minY) + this.bounds.minY,
                random.nextFloat() * (this.bounds.maxZ - this.bounds.minZ) + this.bounds.minZ);
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomInBoundsParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("from_pos", Vec3Argument.vec3())
                    .then(Commands.argument("to_pos", Vec3Argument.vec3())
                            .then(forward));
        }

        @Override
        public RandomInBoundsParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomInBoundsParticlePosition(new AABB(Vec3Argument.getVec3(context, "from_pos"), Vec3Argument.getVec3(context, "to_pos")));
        }
    }
}