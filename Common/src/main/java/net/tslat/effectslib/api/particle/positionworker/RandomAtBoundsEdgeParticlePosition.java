package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) on the surface of a given axis-aligned bounding box
 */
public class RandomAtBoundsEdgeParticlePosition implements ParticlePositionWorker<RandomAtBoundsEdgeParticlePosition> {
    private final AABB bounds;

    private RandomAtBoundsEdgeParticlePosition(AABB bounds) {
        this.bounds = bounds;
    }

    public static RandomAtBoundsEdgeParticlePosition create(AABB bounds) {
        return new RandomAtBoundsEdgeParticlePosition(bounds);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_AT_BOUNDS_EDGE;
    }

    static RandomAtBoundsEdgeParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomAtBoundsEdgeParticlePosition(new AABB(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
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
        return switch(Direction.getRandom(random)) {
            case DOWN -> new Vec3(this.bounds.minX + random.nextDouble() * this.bounds.getXsize(), this.bounds.minY, this.bounds.minZ + random.nextDouble() * this.bounds.getZsize());
            case UP -> new Vec3(this.bounds.minX + random.nextDouble() * this.bounds.getXsize(), this.bounds.maxY, this.bounds.minZ + random.nextDouble() * this.bounds.getZsize());
            case NORTH -> new Vec3(this.bounds.minX + random.nextDouble() * this.bounds.getXsize(), this.bounds.minY + random.nextDouble() * this.bounds.getYsize(), this.bounds.minZ);
            case SOUTH -> new Vec3(this.bounds.minX + random.nextDouble() * this.bounds.getXsize(), this.bounds.minY + random.nextDouble() * this.bounds.getYsize(), this.bounds.maxZ);
            case WEST -> new Vec3(this.bounds.minX, this.bounds.minY + random.nextDouble() * this.bounds.getYsize(), this.bounds.minZ + random.nextDouble() * this.bounds.getZsize());
            case EAST -> new Vec3(this.bounds.maxX, this.bounds.minY + random.nextDouble() * this.bounds.getYsize(), this.bounds.minZ + random.nextDouble() * this.bounds.getZsize());
        };
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomAtBoundsEdgeParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("from_pos", Vec3Argument.vec3())
                    .then(Commands.argument("to_pos", Vec3Argument.vec3())
                            .then(forward));
        }

        @Override
        public RandomAtBoundsEdgeParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomAtBoundsEdgeParticlePosition(new AABB(Vec3Argument.getVec3(context, "from_pos"), Vec3Argument.getVec3(context, "to_pos")));
        }
    }
}