package net.tslat.effectslib;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

import java.util.Locale;
import java.util.function.Function;

public class TELFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> TELClient.tickParticleTransitions());
    }

    public static void sendPacketToServer(MultiloaderPacket packet) {
        FriendlyByteBuf buffer = PacketByteBufs.create();

        packet.encode(buffer);

        ClientPlayNetworking.send(MultiloaderPacket.getId(packet), buffer);
    }

    public static <P extends MultiloaderPacket<P>> void registerPacket(Class<P> messageType, Function<FriendlyByteBuf, P> decoder) {
        ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation(TELConstants.MOD_ID, messageType.getName().toLowerCase(Locale.ROOT)), (client, handler, buf, responseSender) -> decoder.apply(buf).receiveMessage(client.player, client::execute));
    }
}
