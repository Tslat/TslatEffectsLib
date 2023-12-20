package net.tslat.effectslib.api.particle.positionworker;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.api.util.CommandSegmentHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Particle position handler that generates random position(s) in the targeted entity's bounding box.
 * <p>If the entity isn't present on the client, it returns the position the entity is at on the server</p>
 */
public class RandomInEntityParticlePosition implements ParticlePositionWorker<RandomInEntityParticlePosition> {
    private final int entityId;
    private final Vec3 origin;

    private Entity entity = null;

    private RandomInEntityParticlePosition(int entityId, Vec3 origin) {
        this.entityId = entityId;
        this.origin = origin;
    }

    public static RandomInEntityParticlePosition create(int entityId, Vec3 origin) {
        return new RandomInEntityParticlePosition(entityId, origin);
    }

    @Override
    public PositionType type() {
        return PositionType.RANDOM_IN_ENTITY;
    }

    static RandomInEntityParticlePosition decode(FriendlyByteBuf buffer) {
        return new RandomInEntityParticlePosition(buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
    }

    @Override
    public @NotNull Vec3 supplyPosition(Level level, RandomSource random) {
        checkCache(level);

        if (this.entity == null)
            return this.origin;

        final AABB bounds = TELConstants.COMMON.getRandomEntityBoundingBox(this.entity, random);

        return new Vec3(
                random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX,
                random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY,
                random.nextFloat() * (bounds.maxZ - bounds.minZ) + bounds.minZ);
    }

    private void checkCache(final Level level) {
        if (this.entity == null)
            this.entity = level.getEntity(this.entityId);
    }

    public static class CommandSegment implements CommandSegmentHandler<RandomInEntityParticlePosition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("entity", EntityArgument.entity())
                    .then(forward);
        }

        @Override
        public RandomInEntityParticlePosition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Entity entity = null;

            try {
                entity = EntityArgument.getEntity(context, "entity");
            }
            catch (Exception ignored) {}

            return new RandomInEntityParticlePosition(entity.getId(), entity.position());
        }
    }
}