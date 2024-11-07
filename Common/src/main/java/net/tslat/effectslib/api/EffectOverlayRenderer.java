package net.tslat.effectslib.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix4f;

import java.util.function.Supplier;

@FunctionalInterface
public interface EffectOverlayRenderer {
	void render(PoseStack poseStack, Supplier<Matrix4f> projectionMatrix, DeltaTracker deltaTracker, float screenEffectScale, MobEffectInstance effectInstance);
}
