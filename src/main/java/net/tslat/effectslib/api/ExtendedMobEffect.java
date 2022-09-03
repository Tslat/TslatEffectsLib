package net.tslat.effectslib.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Extension class of MobEffect. Acts as the base for all effects and functions in this library. <br>
 * Note that the additional methods added here are also passthroughs for the vanilla methods where applicable.
 * This will cause some parameters to be marked with {@code @Nullable} where they would not be able to be provided when passing through.
 * This should only occur when another mod is calling the vanilla methods, which should almost never happen. <br>
 * In the event it does however, it might be worth falling back to a default behaviour for best compatibility. Until Forge or Mojang fixes MobEffects themselves, this is the best we can do.
 */
public class ExtendedMobEffect extends MobEffect {
	protected ExtendedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	/**
	 * Get the display name of the effect instance. Used for rendering the effects in inventories. <br>
	 * Handle any roman numerals relating to amplifier here if applicable
	 * @param instance The instance the effect is in. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @return The display name component
	 */
	public Component getDisplayName(@Nullable MobEffectInstance instance) {
		MutableComponent component = Component.translatable(this.getDescriptionId());

		if (instance != null && instance.getAmplifier() > 0 && instance.getAmplifier() < 10)
			component.append(" ").append(Component.translatable("enchantment.level." + instance.getAmplifier() + 1));

		return component;
	}

	/**
	 * Tick method, called once per tick while the entity has this effect active. <br>
	 * @param entity The entity the effect is ticking on
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 */
	public void tick(LivingEntity entity, @Nullable MobEffectInstance effectInstance, int amplifier) {}

	/**
	 * Handle for when the effect is first applied to an entity. Useful for triggering additional effects or up-front additional handling.
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param source The direct entity source of the effect, if applicable. E.G. The player drinking a potion, the {@link net.minecraft.world.entity.projectile.ThrownPotion} entity
	 * @param indirectSource The indirect source entity of the effect, if applicable. E.G. The thrower of a splash potion
	 * @param entity The entity the effect is applying to
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 * @param sourceModifier An optionally usable modifier to handle the strength/efficacy of an effect, based on the source it came from. E.G. Distance from a splash potion
	 */
	public void onApplication(@Nullable MobEffectInstance effectInstance, @Nullable Entity source, @Nullable Entity indirectSource, LivingEntity entity, int amplifier, double sourceModifier) {}

	/**
	 * Handle for when the effect is being applied to an entity that already has the effect applied. This is called before the game checks whether the new effect will apply at all.
	 * @param existingEffectInstance The existing MobEffectInstance that the entity already has in place
	 * @param newEffectInstance The new MobEffectInstance that is pending application
	 * @param entity The entity the effect is applying to
	 * @param source The direct entity source of the effect, if applicable. E.G. The player drinking a potion, the {@link net.minecraft.world.entity.projectile.ThrownPotion} entity
	 * @return The effect instance to apply in place of the one that was going to be applied, or null to use the default vanilla functionality
	 */
	@Nullable
	public MobEffectInstance onUpdated(MobEffectInstance existingEffectInstance, MobEffectInstance newEffectInstance, LivingEntity entity, @Nullable Entity source) {
		return null;
	}

	/**
	 * Handle for when the effect is being removed from an entity. Can be called either when expiring or when being manually removed by some other mechanic.
	 * @param effectInstance Effect instance for the effect.
	 * @param entity The entity the effect is being removed from
	 */
	public void onRemoval(MobEffectInstance effectInstance, LivingEntity entity) {}

	/**
	 * Check whether this effect should tick on the current tick.
	 * @see MobEffect#isDurationEffectTick(int, int)
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param entity The entity the effect is ticking on
	 * @param ticksRemaining The ticks remaining on this effect before it expires
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 * @return Whether the effect should tick this game tick or not
	 */
	public boolean shouldTickEffect(@Nullable MobEffectInstance effectInstance, @Nullable LivingEntity entity, int ticksRemaining, int amplifier) {
		return false;
	}

	@Override
	public final void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		for(Map.Entry<Attribute, AttributeModifier> entry : this.attributeModifiers.entrySet()) {
			AttributeInstance attributeInstance = attributeMap.getInstance(entry.getKey());

			if (attributeInstance != null) {
				AttributeModifier baseModifier = entry.getValue();

				attributeInstance.removeModifier(baseModifier);
				attributeInstance.addPermanentModifier(new AttributeModifier(baseModifier.getId(), getDescriptionId() + " " + amplifier, getAttributeModifierValue(entity, entry.getKey(), baseModifier.getAmount(), amplifier), baseModifier.getOperation()));
			}
		}
	}

	/**
	 * Return the instance-sensitive modifier amount for this Attribute
	 * @param entity The entity the modifier is being used on. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param attribute The attribute the modifier is for. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param baseModifierAmount The base amount the effect's predefined AttributeModifier contains
	 * @param effectAmplifier The effect amplifier from the MobEffectInstance
	 * @return The calculated attribute value
	 */
	public double getAttributeModifierValue(@Nullable LivingEntity entity, @Nullable Attribute attribute, double baseModifierAmount, int effectAmplifier) {
		return baseModifierAmount * (effectAmplifier + 1);
	}

	/**
	 * Handle an incoming attack on an entity that has this effect active. <br>
	 * Return false to prevent the attack entirely.
	 * @param entity The entity with this effect active
	 * @param effectInstance The effect instance applied to the entity
	 * @param source The DamageSource for the attack
	 * @param amount The base amount of damage being dealt
	 * @return Whether the attack should proceed or not
	 */
	public boolean beforeIncomingAttack(LivingEntity entity, MobEffectInstance effectInstance, DamageSource source, float amount) {
		return true;
	}

	/**
	 * Adjust the attack damage for an attack an entity with this effect will receive.
	 * @param entity The entity that is being attacked
	 * @param effectInstance The effect instance applied to the victim
	 * @param source The DamageSource for the attack
	 * @param baseAmount The current amount of damage to be dealt
	 * @return The new amount of damage to be dealt
	 */
	public float modifyIncomingAttackDamage(LivingEntity entity, MobEffectInstance effectInstance, DamageSource source, float baseAmount) {
		return baseAmount;
	}

	/**
	 * Adjust the attack damage for an attack that an entity with this effect will perform.
	 * @param entity The entity that is attacking
	 * @param target The target of the attack
	 * @param effectInstance The effect instance applied to the attacking entity
	 * @param source The DamageSource for the attack
	 * @param baseAmount The current amount of damage to be dealt
	 * @return The new amount of damage to be dealt
	 */
	public float modifyOutgoingAttackDamage(LivingEntity entity, LivingEntity target, MobEffectInstance effectInstance, DamageSource source, float baseAmount) {
		return baseAmount;
	}

	/**
	 * Trigger an effect or otherwise listen for an attack that has successfully received by an entity with this effect active.
	 * @param entity The entity being attacked
	 * @param effectInstance The effect instance applied to the victim
	 * @param source The DamageSource for the attack
	 * @param amount The amount of being dealt
	 */
	public void afterIncomingAttack(LivingEntity entity, MobEffectInstance effectInstance, DamageSource source, float amount) {}

	/**
	 * Trigger an effect or otherwise listen for an attack that has successfully been dealt by an entity with this effect active.
	 * @param entity The entity that is attacking
	 * @param effectInstance The effect instance applied to the attacker
	 * @param source The DamageSource for the attack
	 * @param amount The amount of being dealt
	 */
	public void afterOutgoingAttack(LivingEntity entity, LivingEntity victim, MobEffectInstance effectInstance, DamageSource source, float amount) {}

	/**
	 * Return an overlay renderer for this effect. Called when the effect is present on the entity, used for rendering screen effects
	 * Clientside code must be handled outside of the MobEffect class for server-safety
	 * @return The renderer
	 */
	@Nullable
	public EffectOverlayRenderer getOverlayRenderer() {
		return null;
	}

	// START DISABLED METHOD HANDLES

	/**
	 * Disabled, use {@link ExtendedMobEffect#getDisplayName(MobEffectInstance)}
	 */
	@Override
	public final Component getDisplayName() {
		return getDisplayName(null);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#tick(LivingEntity, MobEffectInstance, int)}
	 */
	@Override
	public final void applyEffectTick(LivingEntity entity, int amplifier) {
		tick(entity, null, amplifier);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#onApplication(MobEffectInstance, Entity, Entity, LivingEntity, int, double)}
	 */
	@Override
	public final void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity entity, int amplifier, double sourceModifier) {
		onApplication(null, source, indirectSource, entity, amplifier, sourceModifier);

		if (!isInstantenous())
			tick(entity, null, amplifier);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#shouldTickEffect(MobEffectInstance, LivingEntity, int, int)}
	 */
	@Override
	public final boolean isDurationEffectTick(int ticksRemaining, int amplifier) {
		return shouldTickEffect(null, null, ticksRemaining, amplifier);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#getAttributeModifierValue(LivingEntity, Attribute, double, int)}
	 */
	@Override
	public final double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
		return getAttributeModifierValue(null, null, modifier.getAmount(), amplifier);
	}
}
