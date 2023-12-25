package net.tslat.effectslib.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.networking.packet.MultiloaderPacket;

import java.util.function.Function;

public class TELNetworkingNeoForge implements TELNetworking {
	private static final String REVISION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(TELConstants.MOD_ID, "tel_channel"), () -> REVISION, REVISION::equals, REVISION::equals);

	private static int id = 0;

	/**
	 * Register a custom packet for networking
	 * Packet must extend {@link MultiloaderPacket} for ease-of-use
	 */
	@Override
	public <P extends MultiloaderPacket<P>> void registerPacketInternal(Class<P> messageType, Function<FriendlyByteBuf, P> decoder) {
		CHANNEL.registerMessage(id++, messageType, MultiloaderPacket::encode, decoder::apply, (packet, context) -> {
			packet.receiveMessage(context.getSender() != null ? context.getSender() : TELClient.getClientPlayer(), context::enqueueWork);
			context.setPacketHandled(true);
		});
	}

	/**
	 * Send a packet to the server from the client
	 */
	@Override
	public void sendToServerInternal(MultiloaderPacket packet) {
		CHANNEL.sendToServer(packet);
	}

	/**
	 * Send a packet to all players on the server
	 */
	@Override
	public void sendToAllPlayersInternal(MultiloaderPacket packet) {
		CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
	}

	/**
	 * Send a packet to all players in a given world
	 */
	@Override
	public void sendToAllPlayersInWorldInternal(MultiloaderPacket packet, ServerLevel level) {
		CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
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
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
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
			CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> trackingEntity), packet);
		}
		else {
			CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> trackingEntity), packet);
		}
	}

	/**
	 * Send a packet to all players tracking a given block position
	 */
	@Override
	public void sendToAllPlayersTrackingBlockInternal(MultiloaderPacket packet, ServerLevel level, BlockPos pos) {
		CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
	}
}