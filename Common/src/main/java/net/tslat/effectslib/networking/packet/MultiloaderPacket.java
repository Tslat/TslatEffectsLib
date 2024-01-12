package net.tslat.effectslib.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface MultiloaderPacket extends CustomPacketPayload {
    /**
     * Encode the packet's contents to the given buffer
     */
    @Override
    void write(FriendlyByteBuf buffer);

    /**
     * Handle the message after being received and decoded.<br>
     * Your packet should have its instance-values populated at this stage.<br>
     * This method is side-agnostic, so make sure you call out to client proxies as needed
     * <p>The player may be null if the packet is being sent before the player loads in</p>
     */
    void receiveMessage(@Nullable Player sender, Consumer<Runnable> workQueue);
}
