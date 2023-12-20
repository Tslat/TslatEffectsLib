package net.tslat.effectslib.networking.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.api.particle.ParticleBuilder;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class TELParticlePacket implements MultiloaderPacket<TELParticlePacket> {
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

    public TELParticlePacket particle(final ParticleBuilder particle) {
        this.particles.add(particle);

        return this;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeCollection(this.particles, (buf, builder) -> builder.toNetwork(buf));
    }

    public boolean isEmpty() {
        return this.particles.isEmpty();
    }

    public void send(ServerLevel level) {
        TELConstants.NETWORKING.sendToAllPlayersInWorldInternal(this, level);
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
