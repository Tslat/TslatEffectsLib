package net.tslat.effectslib;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.tslat.effectslib.api.particle.ParticleBuilder;
import net.tslat.effectslib.api.particle.transitionworker.ParticleTransitionWorker;
import net.tslat.effectslib.api.util.MemoizedFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * Helper functions for TEL for client-only code
 */
public final class TELClient {
    private static final ArrayDeque<Runnable> PARTICLE_TRANSITION_HANDLERS = new ArrayDeque<>();

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static long getGameTick() {
        return Minecraft.getInstance().level.getGameTime();
    }

    public static void addParticleTransitionHandler(@NotNull Runnable handler) {
        PARTICLE_TRANSITION_HANDLERS.add(handler);
    }

    public static void tickParticleTransitions() {
        if (PARTICLE_TRANSITION_HANDLERS.isEmpty())
            return;

        for (int i = PARTICLE_TRANSITION_HANDLERS.size(); i > 0; i--) {
            Runnable runnable = PARTICLE_TRANSITION_HANDLERS.poll();

            if (runnable != null)
                runnable.run();
        }
    }

    public static void clearParticles() {
        Minecraft.getInstance().particleEngine.clearParticles();
    }

    public static void addParticle(ParticleBuilder particleBuilder) {
        Minecraft mc = Minecraft.getInstance();

        if (mc == null || mc.particleEngine == null)
            return;

        Camera camera = mc.gameRenderer.getMainCamera();

        if (!camera.isInitialized())
            return;

        if (!particleBuilder.getShouldForce() && mc.levelRenderer.calculateParticleLevel(particleBuilder.getIsAmbient()) == ParticleStatus.MINIMAL)
            return;

        double cutoffDist = particleBuilder.getShouldForce() ? -1 : Math.pow(particleBuilder.getCutoffDistance(), 2);
        Vec3 velocity = particleBuilder.getVelocity();
        RandomSource random = particleBuilder.getRandom();

        for (int i = 0; i < particleBuilder.getCount(); i++) {
            Vec3 pos = particleBuilder.getPosition(mc.level, random);

            if (!particleBuilder.getShouldForce() && camera.getPosition().distanceToSqr(pos) > cutoffDist)
                continue;

            for (int j = 0; j < particleBuilder.getCountPerPosition(); j++) {
                Particle particle = mc.particleEngine.createParticle(particleBuilder.getParticle(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);

                if (particle == null)
                    continue;

                if (particleBuilder.getColourOverride() != null) {
                    int colour = particleBuilder.getColourOverride();

                    particle.setColor(FastColor.ARGB32.red(colour) / 255f, FastColor.ARGB32.green(colour) / 255f, FastColor.ARGB32.blue(colour) / 255f);
                    particle.setAlpha(FastColor.ARGB32.alpha(colour) / 255f);
                }

                if (particleBuilder.getLifespan() > 0)
                    particle.setLifetime(particleBuilder.getLifespan());

                if (particleBuilder.getGravity() != Float.MAX_VALUE)
                    particle.gravity = particleBuilder.getGravity();

                if (particleBuilder.getDrag() > 0)
                    particle.friction = particleBuilder.getDrag();

                if (particleBuilder.getScaleMod() != 1)
                    particle.scale(particleBuilder.getScaleMod());

                if (particleBuilder.getParticleConsumer() != null)
                    particleBuilder.getParticleConsumer().accept(particle);
            }
        }
    }

    public static boolean particleColourTransitionTick(Object obj, @Nullable Function<Object, Integer> fromColourGetter, int toColour, int transitionTime, Consumer<Function<Object, Integer>> startPosSetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (fromColourGetter == null)
            startPosSetter.accept(fromColourGetter = new MemoizedFunction<>(obj2 -> FastColor.ARGB32.color((int)(particle.alpha * 255), (int)(particle.rCol * 255), (int)(particle.gCol * 255), (int)(particle.bCol * 255))));

        final float transitionProgress = ParticleTransitionWorker.getTransitionProgress(particle.age, particle.getLifetime(), transitionTime);
        final int fromColour = fromColourGetter.apply(particle);

        particle.setColor(
                Mth.lerpInt(transitionProgress, FastColor.ARGB32.red(fromColour), FastColor.ARGB32.red(toColour)) / 255f,
                Mth.lerpInt(transitionProgress, FastColor.ARGB32.green(fromColour), FastColor.ARGB32.green(toColour)) / 255f,
                Mth.lerpInt(transitionProgress, FastColor.ARGB32.blue(fromColour), FastColor.ARGB32.blue(toColour)) / 255f);
        particle.setAlpha(Mth.lerpInt(transitionProgress, FastColor.ARGB32.alpha(fromColour), FastColor.ARGB32.alpha(toColour)) / 255f);

        return particle.isAlive();
    }

    public static boolean particlePositionTransitionTick(Object obj, @Nullable Function<Object, Vec3> startPosGetter, Vec3 toPos, int transitionTime, boolean stopOnCollision, Consumer<Function<Object, Vec3>> startPosSetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (startPosGetter == null)
            startPosSetter.accept(startPosGetter = new MemoizedFunction<>(obj2 -> new Vec3(((Particle)obj2).x, ((Particle)obj2).y, ((Particle)obj2).z)));

        if (stopOnCollision && particle.stoppedByCollision)
            return particle.isAlive();

        final float transitionProgress = ParticleTransitionWorker.getTransitionProgress(particle.age, particle.getLifetime(), transitionTime);
        final Vec3 startPos = startPosGetter.apply(particle);

        particle.setPos(Mth.lerp(transitionProgress, startPos.x, toPos.x), Mth.lerp(transitionProgress, startPos.y, toPos.y), Mth.lerp(transitionProgress, startPos.z, toPos.z));

        return particle.isAlive();
    }

    public static boolean particleAwayFromPositionTransitionTick(Object obj, @Nullable Function<Object, Vec3> startPosGetter, Vec3 awayFromPos, int transitionTime, boolean stopOnCollision, Consumer<Function<Object, Vec3>> startPosSetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (startPosGetter == null)
            startPosSetter.accept(startPosGetter = new MemoizedFunction<>(obj2 -> new Vec3(((Particle)obj2).x, ((Particle)obj2).y, ((Particle)obj2).z)));

        if (stopOnCollision && particle.stoppedByCollision)
            return particle.isAlive();

        final float transitionProgress = particle.age / (float)(transitionTime == -1 ? particle.getLifetime() : transitionTime);
        final Vec3 startPos = startPosGetter.apply(particle);
        final Vec3 awayPos = startPos.add(awayFromPos.vectorTo(startPos).normalize().scale(transitionProgress));

        particle.setPos(awayPos.x, awayPos.y, awayPos.z);

        return particle.isAlive();
    }

    public static boolean particleVelocityTransitionTick(Object obj, @Nullable Function<Object, Vec3> startVelocityGetter, Vec3 toVelocity, int transitionTime, Consumer<Function<Object, Vec3>> startVelocitySetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (startVelocityGetter == null)
            startVelocitySetter.accept(startVelocityGetter = new MemoizedFunction<>(obj2 -> new Vec3(((Particle)obj2).xd, ((Particle)obj2).yd, ((Particle)obj2).zd)));

        final float transitionProgress = ParticleTransitionWorker.getTransitionProgress(particle.age, particle.getLifetime(), transitionTime);
        final Vec3 startVelocity = startVelocityGetter.apply(particle);

        particle.xd = Mth.lerp(transitionProgress, startVelocity.x, toVelocity.x);
        particle.yd = Mth.lerp(transitionProgress, startVelocity.y, toVelocity.y);
        particle.zd = Mth.lerp(transitionProgress, startVelocity.z, toVelocity.z);

        return particle.isAlive();
    }

    public static boolean particleScaleTransitionTick(Object obj, @Nullable Function<Object, Float> fromScaleGetter, float toScale, int transitionTime, Consumer<Function<Object, Float>> startScaleSetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (fromScaleGetter == null)
            startScaleSetter.accept(fromScaleGetter = new MemoizedFunction<>(obj2 -> ((Particle)obj2).bbWidth / 0.2f));

        final float transitionProgress = ParticleTransitionWorker.getTransitionProgress(particle.age, particle.getLifetime(), transitionTime);
        final float fromScale = fromScaleGetter.apply(particle);

        particle.scale(Mth.lerp(transitionProgress, fromScale, toScale));

        return particle.isAlive();
    }

    public static boolean particleFollowEntityTick(Object obj, @Nullable Function<Object, Vec3> relativePositionGetter, @Nullable Entity entity, int entityId, boolean stopOnCollision, Consumer<Function<Object, Vec3>> relativePositionSetter, Consumer<Entity> entitySetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (entity == null)
            entitySetter.accept(entity = Minecraft.getInstance().level.getEntity(entityId));

        if (stopOnCollision && particle.stoppedByCollision)
            return particle.isAlive();

        if (entity == null || !entity.isAlive())
            return false;

        final Entity entity2 = entity;

        if (relativePositionGetter == null)
            relativePositionSetter.accept(relativePositionGetter = new MemoizedFunction<>(obj2 -> entity2.position().vectorTo(new Vec3(((Particle)obj2).x, ((Particle)obj2).y, ((Particle)obj2).z))));

        final Vec3 newPos = entity2.position().add(relativePositionGetter.apply(particle));

        particle.setPos(newPos.x, newPos.y, newPos.z);

        return particle.isAlive();
    }

    public static boolean particleCirclingPositionTransitionTick(Object obj, @Nullable Function<Object, Pair<Vec3, Double>> startOffsetsGetter, Vec3 origin, Vec2 angle, boolean stopOnCollision, int transitionTime, Consumer<Function<Object, Pair<Vec3, Double>>> startOffsetsSetter, long killTick, LongConsumer killTickSetter) {
        final Particle particle = (Particle)obj;

        if (killTick == -1)
            killTickSetter.accept(TELClient.getGameTick() + particle.getLifetime());

        if (startOffsetsGetter == null) {
            startOffsetsSetter.accept(startOffsetsGetter = new MemoizedFunction<>(obj2 -> {
                final Vec3 offsetAngle = origin.vectorTo(new Vec3(((Particle)obj2).x, ((Particle)obj2).y, ((Particle)obj2).z));

                return Pair.of(offsetAngle.normalize(), offsetAngle.length());
            }));
        }

        if (stopOnCollision && particle.stoppedByCollision)
            return particle.isAlive();

        float transitionProgress = particle.age / (float)(transitionTime == -1 ? particle.getLifetime() : transitionTime);
        transitionProgress -= (float)Math.floor(transitionProgress);
        final Pair<Vec3, Double> startOffset = startOffsetsGetter.apply(particle);

        float addAngle = Mth.TWO_PI * (transitionProgress - 1);
        Vec3 newPos = origin.add(startOffset.left().add(new Vec3(Mth.cos(addAngle) * angle.x, Mth.cos(addAngle) * angle.y, Mth.sin(addAngle) * angle.x).normalize()).normalize().scale(startOffset.right()));

        particle.setPos(newPos.x, newPos.y, newPos.z);

        return particle.isAlive();
    }
}