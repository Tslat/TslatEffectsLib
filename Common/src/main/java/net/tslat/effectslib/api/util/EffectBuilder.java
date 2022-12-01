package net.tslat.effectslib.api.util;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Helper class to make building {@link MobEffect MobEffects} easier
 */
public final class EffectBuilder {
	private final MobEffect effect;
	private final int duration;

	private int level = 1;
	private boolean ambient = false;
	private boolean showParticles = true;
	private boolean showIcon = true;

	public EffectBuilder(MobEffect effect) {
		this(effect, -1);
	}

	public EffectBuilder(MobEffect effect, int duration) {
		this.effect = effect;
		this.duration = duration;
	}

	/**
	 * Set the level of the MobEffect. This is equivalent to the <i>amplifier</i> + 1
	 * @param level The level of the effect
	 * @return this
	 */
	public EffectBuilder level(int level) {
		this.level = level;

		return this;
	}

	/**
	 * Set the effect to be ambient. Normally this would be used for in-world/passive effects. Changes the HUD icon slightly and reduces the opacity of the particles.
	 * @return this
	 */
	public EffectBuilder isAmbient() {
		ambient = true;

		return this;
	}

	/**
	 * Disable particles spawning on the entity while the effect is active.
	 * @return this
	 */
	public EffectBuilder hideParticles() {
		showParticles = false;

		return this;
	}

	/**
	 * Disable the HUD icon for the MobEffect (icons in the top right of screen)
	 * @return this
	 */
	public EffectBuilder hideEffectIcon() {
		showIcon = false;

		return this;
	}

	/**
	 * Get the MobEffect
	 * @return The MobEffect
	 */
	public MobEffect getEffect() {
		return this.effect;
	}

	/**
	 * Create a new {@link MobEffectInstance} from the builder's current state
	 * @return The MobEffectInstance
	 */
	public MobEffectInstance build() {
		return new MobEffectInstance(effect, duration, level - 1, ambient, showParticles, showIcon);
	}
}
