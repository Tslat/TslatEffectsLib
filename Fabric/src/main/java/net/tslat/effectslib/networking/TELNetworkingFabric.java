package net.tslat.effectslib.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.TELFabricClient;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

import java.util.Locale;
import java.util.function.Function;

public final class TELNetworkingFabric implements TELNetworking {
    /**
     * Register a custom packet for networking
     * Packet must extend {@link MultiloaderPacket} for ease-of-use
     */
    @Override
    public <P extends MultiloaderPacket<P>> void registerPacketInternal(Class<P> messageType, Function<FriendlyByteBuf, P> decoder) {
        TELFabricClient.registerPacket(messageType, decoder);
        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation(TELConstants.MOD_ID, messageType.getName().toLowerCase(Locale.ROOT)), (server, player, packetListener, buffer, sender) -> decoder.apply(buffer).receiveMessage(player, server::execute));
    }

    /**
     * Send a packet to the server from the client
     */
    @Override
    public void sendToServerInternal(MultiloaderPacket packet) {
        TELFabricClient.sendPacketToServer(packet);
    }

    /**
     * Send a packet to all players on the server
     */
    @Override
    public void sendToAllPlayersInternal(MultiloaderPacket packet) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        ResourceLocation id = MultiloaderPacket.getId(packet);

        packet.encode(buffer);

        for (ServerPlayer pl : TELConstants.SERVER.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(pl, id, buffer);
        }
    }

    /**
     * Send a packet to all players in a given world
     */
    @Override
    public void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        ResourceLocation id = MultiloaderPacket.getId(packet);

        packet.encode(buffer);

        for (ServerPlayer pl : PlayerLookup.world(level)) {
            ServerPlayNetworking.send(pl, id, buffer);
        }
    }

    /**
     * Send a packet to all players within a given radius of a position
     */
    @Override
    public void sendToAllNearbyPlayersInternal(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius) {
        for (ServerPlayer pl : PlayerLookup.around(level, origin, radius)) {
            sendToPlayerInternal(packet, pl);
        }
    }

    /**
     * Send a packet to a given player
     */
    @Override
    public void sendToPlayerInternal(MultiloaderPacket packet, ServerPlayer player) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        ResourceLocation id = MultiloaderPacket.getId(packet);

        packet.encode(buffer);

        ServerPlayNetworking.send(player, id, buffer);
    }

    /**
     * Send a packet to all players currently tracking a given entity.<br>
     * Good as a shortcut for sending a packet to all players that may have an interest in a given entity or its dealings.<br>
     * <br>
     * Will also send the packet to the entity itself if the entity is also a player
     */
    @Override
    public void sendToAllPlayersTrackingEntityInternal(MultiloaderPacket packet, Entity trackingEntity) {
        if (trackingEntity instanceof ServerPlayer pl)
            sendToPlayerInternal(packet, pl);

        for (ServerPlayer player : PlayerLookup.tracking(trackingEntity)) {
            sendToPlayerInternal(packet, player);
        }
    }

    /**
     * Send a packet to all players tracking a given block position
     */
    @Override
    public void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            sendToPlayerInternal(packet, player);
        }
    }
}