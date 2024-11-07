package net.tslat.effectslib;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;
import org.jetbrains.annotations.ApiStatus;

public class TELFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TELClient.tickParticleTransitions());
    }

    public static void sendPacketToServer(MultiloaderPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @ApiStatus.Internal
    public static <B extends FriendlyByteBuf, P extends MultiloaderPacket> void registerPacket(CustomPacketPayload.Type<P> packetType, StreamCodec<B, P> codec) {
        PayloadTypeRegistry.playS2C().register(packetType, (StreamCodec<FriendlyByteBuf, P>)codec);
        ClientPlayNetworking.registerGlobalReceiver(packetType, (packet, context) -> packet.receiveMessage(context.player(), context.client()::execute));
    }
}