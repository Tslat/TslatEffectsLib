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
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) in a line between two specified positions.
 */
public class RandomInLineParticlePosition implements ParticlePositionWorker<RandomInLineParticlePosition> {
    private final Vec3 fromPos;
    private final Vec3 toPos;

    private RandomInLineParticlePosition(Vec3 fromPos, Vec3 toPos) {
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    public static RandomInLineParticlePosition create(Vec3 fromPos, Vec3 toPos) {
        return new RandomInLineParticlePosition(fromPos, toPos);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_IN_LINE;
    }

    static RandomInLineParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomInLineParticlePosition(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.fromPos.x);
        buffer.writeDouble(this.fromPos.y);
        buffer.writeDouble(this.fromPos.z);
        buffer.writeDouble(this.toPos.x);
        buffer.writeDouble(this.toPos.y);
        buffer.writeDouble(this.toPos.z);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        return this.fromPos.add(this.fromPos.vectorTo(this.toPos).scale(random.nextDouble()));
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomInLineParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("from_pos", Vec3Argument.vec3())
                    .then(Commands.argument("to_pos", Vec3Argument.vec3())
                            .then(forward));
        }

        @Override
        public RandomInLineParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new RandomInLineParticlePosition(Vec3Argument.getVec3(context, "from_pos"), Vec3Argument.getVec3(context, "to_pos"));
        }
    }
}