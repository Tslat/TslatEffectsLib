package net.tslat.effectslib.api.particle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.TELClient;
import net.tslat.effectslib.api.particle.positionworker.*;
import net.tslat.effectslib.api.particle.transitionworker.ParticleTransitionWorker;
import net.tslat.effectslib.networking.TELNetworking;
import net.tslat.effectslib.networking.packet.TELParticlePacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Flexible & powerful custom particle builder that allows for definition of a particle & various modifications to it, that can then be deployed on the client or sent via a packet.
 * <p>For networking, use {@link #toNetwork(FriendlyByteBuf)} and {@link #fromNetwork(FriendlyByteBuf)} for de/serialization</p>
 */
public final class ParticleBuilder {
    private final ParticleOptions particle;

    private long seed;

    private ParticlePositionWorker<?> positionHandler;
    private ParticlePositionWorker.PositionType positionType;

    private List<ParticleTransitionWorker<?>> transitions = null;
    private Consumer<Object> particleConsumer = null;

    private boolean isSimple = true;

    private int particlesPerPosition = 1;
    private int particleCount = 1;

    private double cutoffDistance = 32;
    private boolean force = false;
    private boolean ambient = false;

    private Vec3 velocity = null;
    private Vec3 power = Vec3.ZERO;
    private Integer colourOverride = null;
    private int lifespan = 0;
    private float gravity = Float.MAX_VALUE;
    private float drag = 0;
    private float scale = 1;

    private ParticleBuilder(ParticleOptions particle, ParticlePositionWorker<?> positionHandler) {
        this.particle = particle;
        this.positionHandler = positionHandler;
        this.positionType = positionHandler.type();
        this.seed = ThreadLocalRandom.current().nextLong();

        defaultParticleCount();
    }

    /**
     * Create a particle builder for the given specified coordinates
     */
    public static ParticleBuilder forPosition(ParticleOptions particle, double x, double y, double z) {
        return forPositions(particle, new Vec3(x, y, z));
    }

    /**
     * Create a particle builder for the given coordinates
     */
    public static ParticleBuilder forPositions(ParticleOptions particle, Vec3... positions) {
        return new ParticleBuilder(particle, CustomParticlePosition.create(new ObjectArrayList<>(positions)));
    }

    /**
     * Create a particle builder for the given coordinates, generating each one from the supplier, with the amount of generated positions specified by the positionCount
     */
    public static ParticleBuilder forPositions(ParticleOptions particle, Supplier<Vec3> positionSupplier, int positionCount) {
        final List<Vec3> positions = new ObjectArrayList<>(positionCount);

        for (int i = 0; i < positionCount; i++) {
            positions.add(positionSupplier.get());
        }

        return new ParticleBuilder(particle, CustomParticlePosition.create(positions));
    }

    /**
     * Generate a particle builder for a line between the from and to positions, with a customisable density
     */
    public static ParticleBuilder forPositionsInLine(ParticleOptions particle, Vec3 from, Vec3 to, int particlesPerBlock) {
        return new ParticleBuilder(particle, InLineParticlePosition.create(from, to, particlesPerBlock));
    }

    /**
     * Generate a particle builder for positions forming a lateral circle of a given radius based around a center point, with a customisable density
     */
    public static ParticleBuilder forPositionsInCircle(ParticleOptions particle, Vec3 center, double radius, int particlesForCircle) {
        return new ParticleBuilder(particle, InCircleParticlePosition.create(center, radius, particlesForCircle));
    }

    /**
     * Generate a particle builder for positions forming a lateral circle of a given radius and angle based around a center point, with a customisable density
     */
    public static ParticleBuilder forPositionsInCircle(ParticleOptions particle, Vec3 center, Vec3 angle, double radius, int particlesForCircle) {
        return new ParticleBuilder(particle, InCircleParticlePosition.create(center, angle, radius, particlesForCircle));
    }

    /**
     * Generate a particle builder for positions forming a sphere of a given radius at the center point, with a customisable density
     */
    public static ParticleBuilder forPositionsInSphere(ParticleOptions particle, Vec3 center, double radius, int particlesPerQuadrant) {
        return new ParticleBuilder(particle, InSphereParticlePosition.create(center, radius, particlesPerQuadrant));
    }

    /**
     * Generate a particle builder for random positions inside a block's hitbox
     */
    public static ParticleBuilder forRandomPosInBlock(ParticleOptions particle, BlockPos pos) {
        return new ParticleBuilder(particle, RandomInBlockParticlePosition.create(pos));
    }

    /**
     * Generate a particle builder for random positions inside a given entity's bounding box(es)
     */
    public static ParticleBuilder forRandomPosInEntity(ParticleOptions particle, Entity entity) {
        return new ParticleBuilder(particle, RandomInEntityParticlePosition.create(entity.getId(), entity.position()));
    }

    /**
     * Generate a particle builder for random positions inside a lateral circle of a given radius based around a center point
     */
    public static ParticleBuilder forRandomPosInCircleRadius(ParticleOptions particle, Vec3 center, double radius) {
        return new ParticleBuilder(particle, RandomInRadiusParticlePosition.create(center, radius));
    }

    /**
     * Generate a particle builder for random positions inside a sphere of a given radius based around a center point
     */
    public static ParticleBuilder forRandomPosInSphere(ParticleOptions particle, Vec3 center, double radius) {
        return new ParticleBuilder(particle, RandomInSphereParticlePosition.create(center, radius));
    }

    /**
     * Generate a particle builder for random positions inside a given bounding box
     */
    public static ParticleBuilder forRandomPosInBounds(ParticleOptions particle, AABB bounds) {
        return new ParticleBuilder(particle, RandomInBoundsParticlePosition.create(bounds));
    }

    /**
     * Generate a particle builder for random positions at the edges of a given bounding box
     */
    public static ParticleBuilder forRandomPosAtBoundsEdge(ParticleOptions particle, AABB bounds) {
        return new ParticleBuilder(particle, RandomAtBoundsEdgeParticlePosition.create(bounds));
    }

    /**
     * Generate a particle builder for random positions forming a circle of a specified radius at the center point
     */
    public static ParticleBuilder forRandomPosAtCircleEdge(ParticleOptions particle, Vec3 center, double radius) {
        return forRandomPosAtCircleEdge(particle, center, new Vec3(0, 1, 0), radius);
    }

    /**
     * Generate a particle builder for random positions forming a circle of a specified radius and angle at the center point
     */
    public static ParticleBuilder forRandomPosAtCircleEdge(ParticleOptions particle, Vec3 center, Vec3 angle, double radius) {
        return new ParticleBuilder(particle, RandomAtCircleEdgeParticlePosition.create(center, angle, radius));
    }

    /**
     * Generate a particle builder for random positions forming a sphere of a specified radius at the center point
     */
    public static ParticleBuilder forRandomPosAtSphereEdge(ParticleOptions particle, Vec3 center, double radius) {
        return new ParticleBuilder(particle, RandomAtSphereEdgeParticlePosition.create(center, radius));
    }

    /**
     * Generate a particle builder using a given ParticlePositionWorker
     */
    public static ParticleBuilder fromCommand(ParticleOptions particle, ParticlePositionWorker<?> position) {
        return new ParticleBuilder(particle, position);
    }

    public ParticleBuilder addTransition(ParticleTransitionWorker<?> transition) {
        if (this.transitions == null)
            this.transitions = new ObjectArrayList<>();

        this.transitions.add(transition);
        this.isSimple = false;

        return this;
    }

    /**
     * Set the amount of particles that should be generated.
     * <p>Particles with varying/random {@link ParticlePositionWorker positions} will generate a new position for each particle</p>
     */
    public ParticleBuilder spawnNTimes(int amount) {
        this.particleCount = amount;

        return this;
    }

    /**
     * Set the amount of particles that should be generated based on the calculated number required for the defined {@link ParticlePositionWorker} to complete one full cycle of positions
     * <p>This only works for PositionTypes that have a definable number of positions (in-line, in circle, etc)</p>
     */
    public ParticleBuilder defaultParticleCount() {
        this.particleCount = this.positionHandler.getParticleCountForSumOfPositions();

        return this;
    }

    /**
     * Set the amount of particles to spawn <u>per position generated</u>
     * <p>This stacks multiplicatively with {@link #spawnNTimes}</p>
     */
    public ParticleBuilder particlesPerPosition(int amount) {
        this.particlesPerPosition = amount;
        this.isSimple = false;

        return this;
    }

    /**
     * Set the particle's initial velocity override (if applicable)
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder velocity(Vec3 velocity) {
        this.velocity = velocity;
        this.isSimple = false;

        return this;
    }

    /**
     * Set the particle's initial velocity (if applicable)
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder velocity(double x, double y, double z) {
        return velocity(new Vec3(x, y, z));
    }

    /**
     * Set the particle's initial 'power' (if applicable)
     * <p>This is somewhat of a magic value that vanilla uses to set initial values on particles. Usually influences speed or distance from origin point</p>
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder power(Vec3 power) {
        this.power = power;
        this.isSimple = false;

        return this;
    }

    /**
     * Set this particle to skip the usual distance and particle settings limitations.
     * <p>This will force this particle to be added, with the only limit being the global particle count limit of 16384</p>
     */
    public ParticleBuilder ignoreDistanceAndLimits() {
        this.force = true;
        this.isSimple = false;

        return this;
    }

    /**
     * Set the distance (in blocks) after which the particle shouldn't be added for the player. This reduces load for particles that are far enough away to not matter
     */
    public ParticleBuilder cutoffDistance(double distance) {
        this.cutoffDistance = distance;
        this.isSimple = false;

        return this;
    }

    /**
     * Set the particle as ambient.
     * <p>This adds a 10% chance that the particle won't be culled if the player's particle options are set to MINIMAL</p>
     * <p>Has no effect if {@link #ignoreDistanceAndLimits()} is used or if the player does not have MINIMAL particles set</p>
     */
    public ParticleBuilder isAmbient() {
        this.ambient = true;
        this.isSimple = false;

        return this;
    }

    /**
     * Apply a tint/colour override to the particle.
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder colourOverride(float red, float green, float blue, float alpha) {
        return colourOverride((int)(red * 255), (int)(green * 255), (int)(blue * 255), (int)(alpha * 255));
    }

    /**
     * Apply a tint/colour override to the particle.
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder colourOverride(int red, int green, int blue, int alpha) {
        return colourOverride(FastColor.ARGB32.color(alpha, red, green, blue));
    }

    /**
     * Apply a tint/colour override to the particle.
     * <p>May not work on all particle types</p>
     */
    public ParticleBuilder colourOverride(int argb) {
        this.colourOverride = (argb & -67108864) == 0 ? argb | -16777216 : argb;
        this.isSimple = false;

        return this;
    }

    /**
     * How long the particle should live for before disappearing.
     * <p>May cause some particles to change how they appear visually</p>
     */
    public ParticleBuilder lifespan(int ticks) {
        this.lifespan = ticks;
        this.isSimple = false;

        return this;
    }

    /**
     * Define how much the particle will be dragged downwards per tick
     * <p>May not function on all particle types </p>
     */
    public ParticleBuilder gravityOverride(float gravity) {
        this.gravity = gravity;
        this.isSimple = false;

        return this;
    }

    /**
     * Define how much the particle should slow down (in percent of current velocity) per tick
     * <p>May not function on all particle types</p>
     */
    public ParticleBuilder velocityDrag(float inertia) {
        this.drag = inertia;
        this.isSimple = false;

        return this;
    }

    /**
     * Define a size multiplier for the particle
     */
    public ParticleBuilder scaleMod(float modifier) {
        this.scale = modifier;
        this.isSimple = false;

        return this;
    }

    /**
     * Define a handler that is given the {@link net.minecraft.client.particle.Particle Particle} instance added to the particle manager
     * <p>The consumer generic is left as empty/unknown to avoid classloading, but it can be safely blind-cast to particle on the client side</p>
     * <p><b>NOTE: </b>This method must only be used on the client side</p>
     */
    public ParticleBuilder particleConsumer(Consumer<Object> consumer) {
        this.particleConsumer = consumer;
        this.isSimple = false;

        return this;
    }

    public ParticleOptions getParticle() {
        return this.particle;
    }

    public Vec3 getPosition(Level level, RandomSource seededRandom) {
        return this.positionHandler.supplyPosition(level, seededRandom);
    }

    public int getCountPerPosition() {
        return this.particlesPerPosition;
    }

    public int getCount() {
        return this.particleCount;
    }

    @Nullable
    public Vec3 getVelocity() {
        return this.velocity;
    }

    public Vec3 getPower() {
        return this.power;
    }

    public double getCutoffDistance() {
        return this.cutoffDistance;
    }

    public boolean getShouldForce() {
        return this.force;
    }

    public boolean getIsAmbient() {
        return this.ambient;
    }

    @Nullable
    public Integer getColourOverride() {
        return this.colourOverride;
    }

    public int getLifespan() {
        return this.lifespan;
    }

    public float getGravity() {
        return this.gravity;
    }

    public float getDrag() {
        return this.drag;
    }

    public float getScaleMod() {
        return this.scale;
    }

    public RandomSource getRandom() {
        return new SingleThreadedRandomSource(this.seed);
    }

    @Nullable
    public Consumer<Object> getParticleConsumer() {
        return this.particleConsumer;
    }

    public List<ParticleTransitionWorker<?>> getTransitions() {
        return this.transitions == null ? List.of() : this.transitions;
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersInWorld(ServerLevel level) {
        TELNetworking.sendToAllPlayersInWorld(new TELParticlePacket(this), level);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingEntity(ServerLevel level, Entity entity) {
        TELNetworking.sendToAllPlayersTrackingEntity(new TELParticlePacket(this), entity);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllPlayersTrackingBlock(ServerLevel level, BlockPos pos) {
        TELNetworking.sendToAllPlayersTrackingBlock(new TELParticlePacket(this), level, pos);
    }

    /**
     * Use this or one of its equivalent methods to deploy this ParticleBuilder's contents to the client side from the server
     */
    public void sendToAllNearbyPlayers(ServerLevel level, Vec3 origin, double radius) {
        TELNetworking.sendToAllNearbyPlayers(new TELParticlePacket(this), level, origin, radius);
    }

    /**
     * Only use on the client side
     */
    public void spawnParticles(Level level) {
        if (this.particle != null && level.isClientSide) {
            if (!getTransitions().isEmpty()) {
                final Consumer<Object> consumer = this.particleConsumer;

                this.particleConsumer = particle -> {
                    TELClient.addParticleTransitionHandler(createTransitionHandler(particle, this.transitions, TELClient::addParticleTransitionHandler));

                    if (consumer != null)
                        consumer.accept(particle);
                };
            }

            TELClient.addParticle(this);
        }
    }

    public void toNetwork(final FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(BuiltInRegistries.PARTICLE_TYPE.getKey(this.particle.getType()));
        this.particle.writeToNetwork(buffer);
        buffer.writeVarInt(this.particleCount);

        buffer.writeEnum(this.positionType);
        this.positionHandler.toNetwork(buffer);

        buffer.writeLong(this.seed);
        buffer.writeBoolean(this.isSimple);

        if (this.isSimple)
            return;

        buffer.writeBoolean(this.transitions != null);

        if (this.transitions != null) {
            buffer.writeCollection(this.transitions, (buf, transition) -> {
                buf.writeEnum(transition.type());
                transition.toNetwork(buf);
            });
        }

        buffer.writeVarInt(this.particlesPerPosition);
        buffer.writeBoolean(this.velocity != null);
        buffer.writeDouble(this.velocity.x);
        buffer.writeDouble(this.velocity.y);
        buffer.writeDouble(this.velocity.z);

        buffer.writeDouble(this.power.x);
        buffer.writeDouble(this.power.y);
        buffer.writeDouble(this.power.z);

        buffer.writeDouble(this.cutoffDistance);
        buffer.writeBoolean(this.force);
        buffer.writeBoolean(this.ambient);
        buffer.writeBoolean(this.colourOverride != null);

        if (this.colourOverride != null)
            buffer.writeVarInt(this.colourOverride);

        buffer.writeVarInt(this.lifespan);
        buffer.writeFloat(this.gravity);
        buffer.writeFloat(this.drag);
        buffer.writeFloat(this.scale);
    }

    public static ParticleBuilder fromNetwork(final FriendlyByteBuf buffer) {
        ParticleType<? extends ParticleOptions> particleType = BuiltInRegistries.PARTICLE_TYPE.get(buffer.readResourceLocation());

        if (particleType == null)
            return new ParticleBuilder(null, null);

        final ParticleOptions particle = deserializeParticle(buffer, particleType);
        final int particleCount = buffer.readVarInt();
        ParticleBuilder builder = new ParticleBuilder(particle, buffer.readEnum(ParticlePositionWorker.PositionType.class).constructFromNetwork(buffer));

        builder.particleCount = particleCount;
        builder.seed = buffer.readLong();

        if (buffer.readBoolean())
            return builder;

        if (buffer.readBoolean())
            builder.transitions = buffer.readCollection(ObjectArrayList::new, buf -> buf.readEnum(ParticleTransitionWorker.TransitionType.class).constructFromNetwork(buf));

        builder.isSimple = false;
        builder.particlesPerPosition = buffer.readVarInt();
        builder.velocity = buffer.readBoolean() ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()) : null;
        builder.power = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        builder.cutoffDistance = buffer.readDouble();
        builder.force = buffer.readBoolean();
        builder.ambient = buffer.readBoolean();

        if (buffer.readBoolean())
            builder.colourOverride = buffer.readVarInt();

        builder.lifespan = buffer.readVarInt();
        builder.gravity = buffer.readFloat();
        builder.drag = buffer.readFloat();
        builder.scale = buffer.readFloat();

        return builder;
    }

    /**
     * Create a basic native transition handler, taking the generated particle instance and a list of transitions to apply.
     * <p>This handler will then apply itself to the given scheduler, and will continue to apply itself each time it is run until all transitions are empty</p>
     * @return The created transition handler
     */
    public static Runnable createTransitionHandler(Object particle, List<ParticleTransitionWorker<?>> transitions, Consumer<Runnable> scheduler) {
        if (transitions.isEmpty())
            return () -> {};

        return transitionHandlerRunnable(new ObjectArrayList<>(transitions), particle, scheduler);
    }

    private static Runnable transitionHandlerRunnable(List<ParticleTransitionWorker<?>> transitions, Object particle, Consumer<Runnable> scheduler) {
        return () -> {
            transitions.removeIf(transition -> !transition.tick(particle));

            if (!transitions.isEmpty())
                scheduler.accept(transitionHandlerRunnable(transitions, particle, scheduler));
        };
    }

    private static <T extends ParticleOptions> T deserializeParticle(FriendlyByteBuf buffer, ParticleType<T> particleType) {
        return particleType.getDeserializer().fromNetwork(particleType, buffer);
    }
}
