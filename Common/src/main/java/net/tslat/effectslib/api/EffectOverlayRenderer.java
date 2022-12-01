package net.tslat.effectslib.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.effect.MobEffectInstance;

@FunctionalInterface
public interface EffectOverlayRenderer {
	void render(PoseStack poseStack, float partialTicks, MobEffectInstance effectInstance);
}
