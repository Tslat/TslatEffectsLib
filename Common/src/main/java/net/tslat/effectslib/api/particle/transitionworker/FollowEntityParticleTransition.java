package net.tslat.effectslib.api.particle.transitionworker;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.api.util.CommandSegmentHandler;

import java.util.function.Function;

/**
 * Particle transition worker that continuously positions the particle the same distance from the target entity that it started as, effectively 'following' the entity.
 * <p>Optionally stop following the entity if the particle collides with something</p>
 */
public class FollowEntityParticleTransition implements ParticleTransitionWorker<FollowEntityParticleTransition> {
    private final int entityId;
    private final boolean stopOnCollision;
    private long killTick = -1;

    private Function<Object, Vec3> startDistances;
    private Entity followingEntity;

    private FollowEntityParticleTransition(int entityId, boolean stopOnCollision) {
        this.entityId = entityId;
        this.stopOnCollision = stopOnCollision;
    }

    public static FollowEntityParticleTransition create(int entityId) {
        return new FollowEntityParticleTransition(entityId, false);
    }

    public static FollowEntityParticleTransition create(int entityId, boolean stopOnCollision) {
        return new FollowEntityParticleTransition(entityId, stopOnCollision);
    }

    @Override
    public TransitionType type() {
        return TransitionType.FOLLOW_ENTITY;
    }

    @Override
    public long getKillTick() {
        return this.killTick;
    }

    static FollowEntityParticleTransition decode(FriendlyByteBuf buffer) {
        return new FollowEntityParticleTransition(buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeBoolean(this.stopOnCollision);
    }

    @Override
    public boolean tick(Object particle) {
        if (!shouldBeAlive())
            return false;

        return TELClient.particleFollowEntityTick(particle, this.startDistances, this.followingEntity, this.entityId, this.stopOnCollision, function -> this.startDistances = function, entity -> this.followingEntity = entity, this.killTick, killTick -> this.killTick = killTick);
    }

    public static class CommandSegment implements CommandSegmentHandler<FollowEntityParticleTransition> {
        @Override
        public ArgumentBuilder<CommandSourceStack, ?> constructArguments(CommandBuildContext context, CommandNode<CommandSourceStack> forward) {
            return Commands.argument("entity", EntityArgument.entity())
                    .then(forward)
                    .then(Commands.argument("stop_on_collision", BoolArgumentType.bool())
                            .then(forward));
        }

        @Override
        public FollowEntityParticleTransition createFromArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Entity entity = EntityArgument.getEntity(context, "entity");
            boolean stopOnCollision = false;

            try {
                stopOnCollision = BoolArgumentType.getBool(context, "stop_on_collision");
            }
            catch (Exception ignored) {}

            return new FollowEntityParticleTransition(entity.getId(), stopOnCollision);
        }
    }
}