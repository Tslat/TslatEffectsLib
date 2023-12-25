package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

import java.util.function.Function;

public interface TELNetworking {
    /**
     * Register a custom packet for networking
     * <p>Packet must extend {@link MultiloaderPacket} for ease-of-use</p>
     */
    static <P extends MultiloaderPacket<P>> void registerPacket(Class<P> messageType, Function<FriendlyByteBuf, P> decoder) {
        TELConstants.NETWORKING.registerPacketInternal(messageType, decoder);
    }

    /**
     * Send a packet to the server from the client
     */
    static void sendToServer(MultiloaderPacket packet) {
        TELConstants.NETWORKING.sendToServerInternal(packet);
    }

    /**
     * Send a packet to all players on the server
     */
    static void sendToAllPlayers(MultiloaderPacket packet) {
        TELConstants.NETWORKING.sendToAllPlayersInternal(packet);
    }

    /**
     * Send a packet to all players in a given world
     */
    static void sendToAllPlayersInWorld(MultiloaderPacket packet, ServerLevel level) {
        TELConstants.NETWORKING.sendToAllPlayersInWorldInternal(packet, level);
    }

    /**
     * Send a packet to all players within a given radius of a position
     */
    static void sendToAllNearbyPlayers(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius) {
        TELConstants.NETWORKING.sendToAllNearbyPlayersInternal(packet, level, origin, radius);
    }

    /**
     * Send a packet to a given player
     */
    static void sendToPlayer(MultiloaderPacket packet, ServerPlayer player) {
        TELConstants.NETWORKING.sendToPlayerInternal(packet, player);
    }

    /**
     * Send a packet to all players currently tracking a given entity.<br>
     * Good as a shortcut for sending a packet to all players that may have an interest in a given entity or its dealings.<br>
     * <br>
     * Will also send the packet to the entity itself if the entity is also a player
     */
    static void sendToAllPlayersTrackingEntity(MultiloaderPacket packet, Entity trackingEntity) {
        TELConstants.NETWORKING.sendToAllPlayersTrackingEntityInternal(packet, trackingEntity);
    }

    /**
     * Send a packet to all players tracking a given block position
     */
    static void sendToAllPlayersTrackingBlock(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
        TELConstants.NETWORKING.sendToAllPlayersTrackingBlockInternal(packet, level, pos);
    }

    // <-- Multiloader instanced methods --> //

    <P extends MultiloaderPacket<P>> void registerPacketInternal(Class<P> messageType, Function<FriendlyByteBuf, P> decoder);
    void sendToServerInternal(MultiloaderPacket packet);
    void sendToAllPlayersInternal(MultiloaderPacket packet);
    void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level);
    void sendToAllNearbyPlayersInternal(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius);
    void sendToPlayerInternal(MultiloaderPacket packet, ServerPlayer player);
    void sendToAllPlayersTrackingEntityInternal(MultiloaderPacket packet, Entity trackingEntity);
    void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos);
}