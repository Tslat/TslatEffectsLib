package net.tslat.effectslib.api.util;

import net.minecraft.world.effect.MobEffectInstance;

/**
 * Helper util class for effect-related functions
 */
public final class EffectUtil {
	public static final int MAX_POTION_DURATION = 9999999;
	public static final int AMBIENT_POTION_DURATION = -1;

	public static boolean amplifyEffect(MobEffectInstance effect, int amplification) {
		return effect.update(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier() + amplification, effect.isAmbient(), effect.isVisible(), effect.showIcon()));
	}
}
