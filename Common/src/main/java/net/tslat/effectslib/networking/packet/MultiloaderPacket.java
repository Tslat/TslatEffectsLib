package net.tslat.effectslib.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.function.Consumer;

public interface MultiloaderPacket<P extends MultiloaderPacket<P>> {
    static ResourceLocation getId(MultiloaderPacket<?> packet) {
        return new ResourceLocation("tslatentitystatus", packet.getClass().getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Encode the packet's contents to the given buffer
     */
    void encode(FriendlyByteBuf buffer);

    /**
     * Handle the message after being received and decoded.<br>
     * Your packet should have its instance-values populated at this stage.<br>
     * This method is side-agnostic, so make sure you call out to client proxies as needed
     */
    void receiveMessage(Player sender, Consumer<Runnable> workQueue);
}
