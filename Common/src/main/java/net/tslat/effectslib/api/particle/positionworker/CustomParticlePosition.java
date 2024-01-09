package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

import java.util.Iterator;
import java.util.List;

/**
 * Particle position handler that just holds a collection of server-defined positions.
 * <p>Automatically returns to the first position in the list if all positions have been used</p>
 */
public class CustomParticlePosition implements ParticlePositionWorker<CustomParticlePosition> {
    private final List<Vec3> positions;

    private Iterator<Vec3> positionIterator = null;

    private CustomParticlePosition(List<Vec3> positions) {
        this.positions = positions;
    }

    public static CustomParticlePosition create(List<Vec3> positions) {
        return new CustomParticlePosition(positions);
    }

    @Override
    public PositionType type() {
        return PositionType.CUSTOM;
    }

    @Override
    public int getParticleCountForSumOfPositions() {
        return this.positions.size();
    }

    static CustomParticlePosition decode(FriendlyByteBuf buffer) {
        return new CustomParticlePosition(buffer.readCollection(ObjectArrayList::new, buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeCollection(this.positions, (buf, pos) -> {
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
        });
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        return this.positionIterator.next();
    }

    private void checkCache(final Level level) {
        if (this.positionIterator == null || !this.positionIterator.hasNext())
            this.positionIterator = this.positions.iterator();
    }

    public static class CommandSegment implements CommandSegmentHandler<CustomParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("pos", Vec3Argument.vec3()).then(forward);
        }

        @Override
        public CustomParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return new CustomParticlePosition(List.of(Vec3Argument.getVec3(context, "pos")));
        }
    }
}