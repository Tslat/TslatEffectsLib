package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tslat.effectslib.TslatEffectsLib;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

public class TELNetworkingNeoForge implements TELNetworking {
	@Override
	public <P extends MultiloaderPacket> void registerPacketInternal(ResourceLocation id, Class<P> packetClass, FriendlyByteBuf.Reader<P> decoder) {
		TslatEffectsLib.packetRegistrar.play(id, decoder, (packet, context) -> packet.receiveMessage(context.player().orElseGet(null), context.workHandler()::execute));
	}

	/**
	 * Send a packet to the server from the client
	 */
	@Override
	public void sendToServerInternal(MultiloaderPacket packet) {
		PacketDistributor.SERVER.noArg().send(packet);
	}

	/**
	 * Send a packet to all players on the server
	 */
	@Override
	public void sendToAllPlayersInternal(MultiloaderPacket packet) {
		PacketDistributor.ALL.noArg().send(packet);
	}

	/**
	 * Send a packet to all players in a given world
	 */
	@Override
	public void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level) {
		PacketDistributor.DIMENSION.with(level.dimension()).send(packet);
	}

	/**
	 * Send a packet to all players within a given radius of a position
	 */
	@Override
	public void sendToAllNearbyPlayersInternal(MultiloaderPacket packet, ServerLevel level, Vec3 origin, double radius) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(origin) <= radius * radius)
				sendToPlayerInternal(packet, player);
		}
	}

	/**
	 * Send a packet to a given player
	 */
	@Override
	public void sendToPlayerInternal(MultiloaderPacket packet, ServerPlayer player) {
		PacketDistributor.PLAYER.with(player).send(packet);
	}

	/**
	 * Send a packet to all players currently tracking a given entity.<br>
	 * Good as a shortcut for sending a packet to all players that may have an interest in a given entity or its dealings.<br>
	 * <br>
	 * Will also send the packet to the entity itself if the entity is also a player
	 */
	@Override
	public void sendToAllPlayersTrackingEntityInternal(MultiloaderPacket packet, Entity trackingEntity) {
		if (trackingEntity instanceof Player) {
			PacketDistributor.TRACKING_ENTITY_AND_SELF.with(trackingEntity).send(packet);
		}
		else {
			PacketDistributor.TRACKING_ENTITY.with(trackingEntity).send(packet);
		}
	}

	/**
	 * Send a packet to all players tracking a given block position
	 */
	@Override
	public void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
		PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)).send(packet);
	}
}