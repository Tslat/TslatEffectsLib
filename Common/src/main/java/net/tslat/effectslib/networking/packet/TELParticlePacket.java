package net.tslat.effectslib.networking.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.api.particle.ParticleBuilder;
import net.tslat.effectslib.networking.TELNetworking;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class TELParticlePacket implements MultiloaderPacket {
    public static final ResourceLocation ID = new ResourceLocation(TELConstants.MOD_ID, "tel_particle");

    private final Collection<ParticleBuilder> particles;

    public TELParticlePacket() {
        this(1);
    }

    public TELParticlePacket(int amount) {
        this(new ObjectArrayList<>(amount));
    }

    public TELParticlePacket(List<ParticleBuilder> particles) {
        this.particles = particles;
    }

    public TELParticlePacket(ParticleBuilder... particles) {
        this(ObjectArrayList.of(particles));
    }

    public TELParticlePacket(FriendlyByteBuf buffer) {
        this.particles = buffer.readCollection(ObjectArrayList::new, ParticleBuilder::fromNetwork);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public TELParticlePacket particle(final ParticleBuilder particle) {
        this.particles.add(particle);

        return this;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeCollection(this.particles, (buf, builder) -> builder.toNetwork(buf));
    }

    public boolean isEmpty() {
        return this.particles.isEmpty();
    }

    public void send(ServerLevel level) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersInWorld(this, level);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingEntity(ServerLevel level, Entity entity) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersTrackingEntity(this, entity);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingBlock(ServerLevel level, BlockPos pos) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllPlayersTrackingBlock(this, level, pos);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllNearbyPlayers(ServerLevel level, Vec3 origin, double radius) {
        if (isEmpty())
            return;

        TELNetworking.sendToAllNearbyPlayers(this, level, origin, radius);
    }

    @Override
    public void receiveMessage(Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(() -> {
            for (ParticleBuilder builder : this.particles) {
                builder.spawnParticles(sender.level());
            }
        });
    }
}