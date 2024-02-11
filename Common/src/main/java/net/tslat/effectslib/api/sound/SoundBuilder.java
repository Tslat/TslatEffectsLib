/*
package net.tslat.effectslib.api.sound;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

*/
/**
 * Flexible and powerful sound-playing handler that allows for quickly and clearly playing sounds, bypassing vanilla's significant restrictions on S2C sound events.
 * <p>Also allows for stopping sounds remotely</p>
 *//*

public final class SoundBuilder {
	private final SoundEvent sound;
	@Nullable
	private final Level level;
	@Nullable
	private final Vec3 location;
	private final Entity followingEntity;
	private final Type type;
	private boolean stopSound = false;
	private final EnumSet<Section> sectionsToWrite = EnumSet.noneOf(Section.class);

	private SoundSource category = SoundSource.MASTER;

	private long seed = 0L;

	private Supplier<Float> pitch = () -> 1f;
	private float pitchVariation = 0;
	private float radius = 16f;

	private int scheduleDelay = 0;
	private boolean applyTimeDilation = false;

	private boolean inWorld = true;
	private boolean loop = false;
	private int loopDelay = 0;

	private Set<Player> playTo = null;
	private Set<UUID> exclude = null;

	*/
/**
	 * Play a sound at a given entity's position
	 * <p>Automatically applies an entity {@link SoundSource} category</p>
	 *//*

	public static SoundBuilder atPos(SoundEvent sound, Entity entity) {
		return atPos(sound, entity.level(), entity.blockPosition()).category(getEntityCategory(entity));
	}

	*/
/**
	 * Play a sound at a given block position
	 *//*

	public static SoundBuilder atPos(SoundEvent sound, Level level, BlockPos pos) {
		return atPos(sound, level, Vec3.atCenterOf(pos));
	}

	*/
/**
	 * Play a sound at a given position
	 *//*

	public static SoundBuilder atPos(SoundEvent sound, Level level, double x, double y, double z) {
		return atPos(sound, level, new Vec3(x, y, z));
	}

	*/
/**
	 * Play a sound at a given position
	 *//*

	public static SoundBuilder atPos(SoundEvent sound, Level level, Vec3 pos) {
		return new SoundBuilder(sound, level, pos);
	}

	*/
/**
	 * Play a sound at a given entity's position, following them as it moves
	 * <p>Automatically applies an entity {@link SoundSource} category</p>
	 *//*

	public static SoundBuilder followingEntity(SoundEvent sound, Entity entity) {
		return new SoundBuilder(sound, entity).category(getEntityCategory(entity));
	}

	*/
/**
	 * Play a non-positioned music-type sound, automatically applying its {@link SoundSource category}
	 *//*

	public static SoundBuilder forMusic(SoundEvent sound) {
		return new SoundBuilder(sound, null, null).isMusic();
	}

	*/
/**
	 * Stop a given sound for a given {@link SoundSource category}
	 * <p>You can further refine who receives this stop instruction via {@link SoundBuilder#audibleRadius(float)}, {@link SoundBuilder#dontPlayTo(Player...)}, and {@link SoundBuilder#onlyPlayFor(Player...)}</p>
	 *//*

	public static SoundBuilder stopSound(SoundEvent sound, SoundSource category) {
		SoundBuilder soundBuilder = new SoundBuilder(sound, null, null).category(category);

		soundBuilder.sectionsToWrite.add(Section.STOPPING);
		soundBuilder.stopSound = true;

		return soundBuilder;
	}

	private SoundBuilder(SoundEvent sound, @Nullable Level level, @Nullable Vec3 pos) {
		this.sound = sound;
		this.level = level;
		this.location = pos;
		this.followingEntity = null;
		this.type = pos == null ? Type.GLOBAL : Type.AT_POS;
	}

	private SoundBuilder(SoundEvent sound, Entity followingEntity) {
		this.sound = sound;
		this.level = followingEntity.level();
		this.location = followingEntity.position();
		this.followingEntity = followingEntity;
		this.type = Type.FOLLOWING_ENTITY;
	}

	private static SoundBuilder constructFromNetwork(FriendlyByteBuf buffer) {
		final SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(buffer.readResourceLocation());
		final Player pl = TELClient.getClientPlayer();

		if (pl == null)
			return new SoundBuilder(sound, null, null);

		final Type type = buffer.readEnum(Type.class);

		return type.reader.apply(buffer, sound);
	}

	private static SoundSource getEntityCategory(@NotNull Entity entity) {
		if (entity instanceof Player)
			return SoundSource.PLAYERS;

		if (entity instanceof Enemy)
			return SoundSource.HOSTILE;

		return SoundSource.NEUTRAL;
	}

	*/
/**
	 * Set the SoundSource for the sound
	 *//*

	public SoundBuilder category(SoundSource source) {
		this.sectionsToWrite.add(Section.CATEGORY);
		this.category = source;

		return this;
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#MUSIC}
	 *//*

	public SoundBuilder isMusic() {
		notInWorld();
		return category(SoundSource.MUSIC);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#RECORDS}
	 *//*

	public SoundBuilder isRecord() {
		return category(SoundSource.RECORDS);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#WEATHER}
	 *//*

	public SoundBuilder isWeather() {
		return category(SoundSource.WEATHER);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#BLOCKS}
	 *//*

	public SoundBuilder isBlocks() {
		return category(SoundSource.BLOCKS);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#HOSTILE}
	 *//*

	public SoundBuilder isMonster() {
		return category(SoundSource.HOSTILE);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#NEUTRAL}
	 *//*

	public SoundBuilder isFriendlyMob() {
		return category(SoundSource.NEUTRAL);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#PLAYERS}
	 *//*

	public SoundBuilder isPlayer() {
		return category(SoundSource.PLAYERS);
	}

	*/
/**
	 * Categorise the music as {@link SoundSource#AMBIENT}
	 *//*

	public SoundBuilder isAmbience() {
		return category(SoundSource.AMBIENT);
	}

	*/
/**
	 * Sets the random seed of the sound
	 * <p>This only affects things like variable volume and pitch, and weighted sound events</p>
	 *//*

	public SoundBuilder withSeed(long seed) {
		this.sectionsToWrite.add(Section.SEED);
		this.seed = seed;

		return this;
	}

	*/
/**
	 * Define the pitch for the sound. This value will be resolved once per send call, with each client receiving the same value even if the supplier returns random values
	 *//*

	public SoundBuilder pitch(Supplier<Float> pitch) {
		this.sectionsToWrite.add(Section.PITCH);
		this.pitch = pitch;

		return this;
	}

	*/
/**
	 * Define a constant pitch for the sound
	 *//*

	public SoundBuilder pitch(float pitch) {
		return pitch(() -> pitch);
	}

	*/
/**
	 * Randomly vary the sound by the given deviation value, in a Normal Distribution
	 *//*

	public SoundBuilder randomPitchVariation(float deviation) {
		this.pitchVariation = deviation;

		return this;
	}

	*/
/**
	 * Set the audible radius of this sound.
	 * <p>This affects both who receives the sound instruction, and the resulting sound volume depending on distance to the source</p>
	 *//*

	public SoundBuilder audibleRadius(float radius) {
		this.sectionsToWrite.add(Section.RADIUS);
		this.radius = radius;

		return this;
	}

	*/
/**
	 * Applies a time delay to the sound being played based on how far the listener is to the source of the sound
	 *//*

	public SoundBuilder applyTimeDilation() {
		this.sectionsToWrite.add(Section.TIME_DILATION);
		this.applyTimeDilation = true;

		return this;
	}

	*/
/**
	 * Set an initial delay (in ticks) before the sound should play
	 *//*

	public SoundBuilder delay(int ticksDelay) {
		this.sectionsToWrite.add(Section.DELAY);
		this.scheduleDelay = ticksDelay;

		return this;
	}

	*/
/**
	 * Mark the sound as not being 'in the world'
	 * <p>This really should only be used for things like narration, music, or menu sounds</p>
	 *//*

	public SoundBuilder notInWorld() {
		this.sectionsToWrite.add(Section.IN_WORLD);
		this.inWorld = false;

		return this;
	}

	*/
/**
	 * Mark the sound as looping
	 *//*

	public SoundBuilder loopSound() {
		this.sectionsToWrite.add(Section.LOOP);
		this.loop = true;

		return this;
	}

	*/
/**
	 * Mark the sound as looping, with a configurable tick-delay between finishing and starting again
	 *//*

	public SoundBuilder loopSound(int afterDelay) {
		this.sectionsToWrite.add(Section.LOOP_DELAY);
		loopSound();
		this.loopDelay = afterDelay;

		return this;
	}

	*/
/**
	 * Explicitly skip sending the sound to these players
	 *//*

	public SoundBuilder dontPlayTo(Player... players) {
		if (this.exclude == null)
			this.exclude = new ObjectOpenHashSet<>();

		for (Player pl : players) {
			this.exclude.add(pl.getUUID());
		}

		return this;
	}

	*/
/**
	 * Only send the sound instruction to these players, regardless of anything else
	 *//*

	public SoundBuilder onlyPlayFor(Player... players) {
		if (this.playTo == null)
			this.playTo = new ObjectOpenHashSet<>();

		Collections.addAll(this.playTo, players);

		return this;
	}

	*/
/**
	 * Play the sound.
	 * <p>Sidedness is automatically handled here, so you can use this safely on both logical sides</p>
	 *//*

	public void play() {
		if (this.stopSound) {
			executeSoundStop();
		}
		else {
			executeSoundPlay();
		}
	}

	@ApiStatus.Internal
	public void toNetwork(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(BuiltInRegistries.SOUND_EVENT.getKey(sound));
		buffer.writeEnum(this.type);

		float pitch = this.pitch.get();
		this.pitch = () -> pitch;

		buffer.writeEnumSet(this.sectionsToWrite, Section.class);
		this.sectionsToWrite.forEach(section -> section.writer.accept(this, buffer));
	}

	@ApiStatus.Internal
	public static SoundBuilder fromNetwork(FriendlyByteBuf buffer) {
		SoundBuilder builder = SoundBuilder.constructFromNetwork(buffer);

		buffer.readEnumSet(Section.class).forEach(section -> section.reader.accept(builder, buffer));

		return builder;
	}

	private void executeSoundPlay() {
		if (inWorld) {
			PlayLevelSoundEvent event = followingEntity != null ? EventHooks.onPlaySoundAtEntity(followingEntity, Holder.direct(sound), category, radius / 16f, pitch) : EventHooks.onPlaySoundAtPosition(level, location.x, location.y, location.z, Holder.direct(sound), category, radius / 16f, pitch);

			if (event.isCanceled() || event.getSound() == null)
				return;

			this.sound = event.getSound().value();
		}

		if (this.level instanceof ServerLevel serverLevel) {
			final AoASoundBuilderPacket packet = new AoASoundBuilderPacket(this);
			this.level = null;

			if (playTo != null) {
				for (Player pl : playTo) {
					if (exclude == null || !exclude.contains(pl))
						AoANetworking.sendToPlayer((ServerPlayer)pl, packet);
				}
			}
			else {
				for (ServerPlayer pl : serverLevel.getServer().getPlayerList().getPlayers()) {
					if (pl.level() == serverLevel && pl.distanceToSqr(location) <= radius * radius && (exclude == null || !exclude.contains(pl)))
						AoANetworking.sendToPlayer(pl, packet);
				}
			}
		}
		else {
			ClientOperations.playSoundFromBuilder(this);
		}
	}

	private void executeSoundStop() {
		if (level == null || level.isClientSide()) {
			ClientOperations.stopSoundFromBuilder(this);
		}
		else {
			AoASoundBuilderPacket packet = new AoASoundBuilderPacket(this);

			if (playTo != null) {
				for (Player pl : playTo) {
					if (exclude == null || !exclude.contains(pl))
						AoANetworking.sendToPlayer((ServerPlayer)pl, packet);
				}
			}
			else {
				for (ServerPlayer pl : level.getServer().getPlayerList().getPlayers()) {
					if (pl.level() == level && pl.distanceToSqr(location) <= radius * radius && (exclude == null || !exclude.contains(pl)))
						AoANetworking.sendToPlayer(pl, packet);
				}
			}
		}
	}

	protected enum Type {
		AT_POS((builder, buffer) -> buffer.writeVec3(builder.location),
				(buffer, sound) -> new SoundBuilder(sound, TELClient.getClientPlayer().level(), buffer.readVec3())),
		FOLLOWING_ENTITY((builder, buffer) -> {
			buffer.writeVec3(builder.location);
			buffer.writeVarInt(builder.followingEntity.getId());
		}, (buffer, sound) -> {
			Vec3 fallbackLoc = buffer.readVec3();
			Entity followingEntity = TELClient.getClientPlayer().level().getEntity(buffer.readVarInt());

			return followingEntity != null ? new SoundBuilder(sound, followingEntity) : new SoundBuilder(sound, TELClient.getClientPlayer().level(), fallbackLoc);
		}),
		GLOBAL((builder, buffer) -> {},
				(buffer, sound) -> new SoundBuilder(sound, null, null));

		final BiConsumer<SoundBuilder, FriendlyByteBuf> writer;
		final BiFunction<FriendlyByteBuf, SoundEvent, SoundBuilder> reader;

		Type(BiConsumer<SoundBuilder, FriendlyByteBuf> writer, BiFunction<FriendlyByteBuf, SoundEvent, SoundBuilder> reader) {
			this.writer = writer;
			this.reader = reader;
		}
	}

	protected enum Section {
		CATEGORY((builder, buffer) -> buffer.writeEnum(builder.category),
				(builder, buffer) -> builder.category = buffer.readEnum(SoundSource.class)),
		STOPPING((builder, buffer) -> buffer.writeBoolean(builder.stopSound),
				(builder, buffer) -> builder.stopSound = buffer.readBoolean()),
		SEED((builder, buffer) -> buffer.writeVarLong(builder.seed),
				(builder, buffer) -> builder.seed = buffer.readVarLong()),
		PITCH((builder, buffer) -> {
			float pitch = builder.pitch.get();

			if (builder.pitchVariation > 0)
				pitch += (float)new SingleThreadedRandomSource(builder.seed).nextGaussian() * builder.pitchVariation;

			buffer.writeFloat(pitch);
			}, (builder, buffer) -> {
			float pitch = buffer.readFloat();
			builder.pitch = () -> pitch;
		}),
		RADIUS((builder, buffer) -> buffer.writeFloat(builder.radius),
				(builder, buffer) -> builder.radius = buffer.readFloat()),
		DELAY((builder, buffer) -> buffer.writeVarInt(builder.scheduleDelay),
				(builder, buffer) -> builder.scheduleDelay = buffer.readVarInt()),
		TIME_DILATION((builder, buffer) -> buffer.writeBoolean(builder.applyTimeDilation),
				(builder, buffer) -> builder.applyTimeDilation = buffer.readBoolean()),
		IN_WORLD((builder, buffer) -> buffer.writeBoolean(builder.inWorld),
				(builder, buffer) -> builder.inWorld = buffer.readBoolean()),
		LOOP((builder, buffer) -> buffer.writeBoolean(builder.loop),
				(builder, buffer) -> builder.loop = buffer.readBoolean()),
		LOOP_DELAY((builder, buffer) -> buffer.writeVarInt(builder.loopDelay),
				(builder, buffer) -> builder.loopDelay = buffer.readVarInt());

		final BiConsumer<SoundBuilder, FriendlyByteBuf> writer;
		final BiConsumer<SoundBuilder, FriendlyByteBuf> reader;

		Section(BiConsumer<SoundBuilder, FriendlyByteBuf> writer, BiConsumer<SoundBuilder, FriendlyByteBuf> reader) {
			this.writer = writer;
			this.reader = reader;
		}
	}
}
*/
