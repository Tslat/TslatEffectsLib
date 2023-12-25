package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Particle position handler that generates random position(s) in a given block's shape.
 * <p>This automatically constrains the positions to the block's bounding box</p>
 */
public class RandomInBlockParticlePosition implements ParticlePositionWorker<RandomInBlockParticlePosition> {
    private final BlockPos pos;

    // Only populated on the client when generating for faster re-use
    private BlockState state = null;
    private List<AABB> shape = null;

    private RandomInBlockParticlePosition(BlockPos pos) {
        this.pos = pos;
    }

    public static RandomInBlockParticlePosition create(BlockPos pos) {
        return new RandomInBlockParticlePosition(pos);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_IN_BLOCK;
    }

    static RandomInBlockParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomInBlockParticlePosition(buffer.readBlockPos());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        AABB bounds = this.shape.get(random.nextInt(this.shape.size()));

        return new Vec3(
                random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX,
                random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY,
                random.nextFloat() * (bounds.maxZ - bounds.minZ) + bounds.minZ).add(this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    private void checkCache(final Level level) {
        if (this.state == null)
            this.state = level.getBlockState(this.pos);

        if (this.shape == null) {
            this.shape = this.state.getShape(level, this.pos).toAabbs();

            if (this.shape.isEmpty())
                this.shape = Shapes.block().toAabbs();
        }
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomInBlockParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(forward);
        }

        @Override
        public RandomInBlockParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomInBlockParticlePosition(BlockPosArgument.getBlockPos(context, "pos"));
        }
    }
}