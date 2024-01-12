package net.tslat.effectslib.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.TELConstants;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TELClearParticlesPacket implements MultiloaderPacket<TELClearParticlesPacket> {
    public static final ResourceLocation ID = new ResourceLocation(TELConstants.MOD_ID, "tel_clear_particles");

    public TELClearParticlesPacket() {}

    public TELClearParticlesPacket(FriendlyByteBuf buffer) {}

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue) {
        workQueue.accept(TELClient::clearParticles);
    }
}
