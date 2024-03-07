package net.tslat.effectslib.api.particle.positionworker;

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
 * Particle position handler that generates positions for a line between two specified positions.
 */
public class InLineParticlePosition implements ParticlePositionWorker<InLineParticlePosition> {
    private final Vec3 fromPos;
    private final Vec3 toPos;
    private final int particlesPerBlock;

    private Iterator<Vec3> positionIterator = null;

    private InLineParticlePosition(Vec3 fromPos, Vec3 toPos, int particlesPerBlock) {
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.particlesPerBlock = particlesPerBlock;
    }

    public static InLineParticlePosition create(Vec3 fromPos, Vec3 toPos, int particlesPerBlock) {
        return new InLineParticlePosition(fromPos, toPos, particlesPerBlock);
    }

    @Override
    public PositionType type() {
        return PositionType.IN_LINE;
    }

    static InLineParticlePosition decode(FriendlyByteBuf buffer) {
        return new InLineParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.fromPos.x);
        buffer.writeDouble(this.fromPos.y);
        buffer.writeDouble(this.fromPos.z);
        buffer.writeDouble(this.toPos.x);
        buffer.writeDouble(this.toPos.y);
        buffer.writeDouble(this.toPos.z);
        buffer.writeVarInt(this.particlesPerBlock);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        return this.positionIterator.next();
    }

    @Override
    public int getParticleCountForSumOfPositions() {
        return Mth.floor(this.fromPos.distanceTo(this.toPos) * this.particlesPerBlock);
    }

    private void checkCache(final Level level) {
        if (this.positionIterator == null || !this.positionIterator.hasNext()) {
            this.positionIterator = new Iterator<>() {
                private final Vec3 angle = InLineParticlePosition.this.fromPos.vectorTo(InLineParticlePosition.this.toPos).normalize();
                private final double length = InLineParticlePosition.this.fromPos.distanceTo(InLineParticlePosition.this.toPos);
                private final double increment = 1 / (float)InLineParticlePosition.this.particlesPerBlock;
                private double step = 0;

                @Override
                public boolean hasNext() {
                    return this.step <= this.length;
                }

                @Override
                public Vec3 next() {
                    double scale = this.step;
                    this.step += this.increment;

                    return InLineParticlePosition.this.fromPos.add(this.angle.multiply(scale, scale, scale));
                }
            };
        }
    }

    public static class CommandSegment implements CommandSegmentHandler<InLineParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("from_pos", Vec3Argument.vec3(false))
                    .then(Commands.argument("to_pos", Vec3Argument.vec3(false))
                            .then(Commands.argument("particles_per_block", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                    .then(forward)));
        }

        @Override
        public InLineParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new InLineParticlePosition(Vec3Argument.getVec3(context, "from_pos"), Vec3Argument.getVec3(context, "to_pos"), IntegerArgumentType.getInteger(context, "particles_per_block"));
        }
    }
}