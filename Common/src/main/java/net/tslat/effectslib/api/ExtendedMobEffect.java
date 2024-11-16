package net.tslat.effectslib.api;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Extension class of {@link MobEffect}. Acts as the base for all effects and functions in this library. <br>
 * Note that the additional methods added here are also pass-throughs for the vanilla methods where applicable.
 * This will cause some parameters to be marked with {@code @Nullable} where they would not be able to be provided when passing through.
 * This should only occur when another mod is calling the vanilla methods, which should almost never happen. <br>
 * In the event it does however, it might be worth falling back to a default behaviour for best compatibility. Until Forge or Mojang fixes MobEffects themselves, this is the best we can do.<br>
 * <br>
 * An alternate approach this would have been to inject this into MobEffect directly via interface, however that doesn't actually offer any functional benefit, and it eliminates the possibility of marking <i>final</i> on the now-unused vanilla methods.
 */
public class ExtendedMobEffect extends MobEffect {
	public ExtendedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}
	public ExtendedMobEffect(MobEffectCategory category, int color, ParticleOptions particle) {
		super(category, color, particle)
	}

	/**
	 * Get the display name of the effect instance. Used for rendering the effects in inventories. <br>
	 * Handle any roman numerals relating to amplifier here if applicable
	 * @param instance The instance the effect is in. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @return The display name component
	 */
	public Component getDisplayName(@Nullable MobEffectInstance instance) {
		MutableComponent name = instance == null ? Component.translatable(getDescriptionId()) : instance.getEffect().value().getDisplayName().copy();

		if (instance != null && instance.getAmplifier() > 0 && instance.getAmplifier() < 10)
			name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)));

		return name;
	}

	/**
	 * Tick method, called once per tick while the entity has this effect active
	 * <p>
	 * Returns to determine whether the entity ticking the effect should have it removed early (prior to natural expiry)
	 * Return false to have the effect removed prematurely
	 *
	 * @param entity The entity the effect is ticking on
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 * @return Return whether the entity should keep the effect or have it removed early
	 */
	public boolean tick(LivingEntity entity, @Nullable MobEffectInstance effectInstance, int amplifier) {
		return true;
	}

	/**
	 * Check for whether an instance with this effect can be applied to an entity or not.
	 * @param entity The entity to be applied to
	 * @param effectInstance The effect instance
	 * @return true if the effect can apply, false if not
	 */
	public boolean canApply(LivingEntity entity, MobEffectInstance effectInstance) {
		return true;
	}

	/**
	 * Check for whether another effect should be applied or not. This can be used for mutual-exclusivity between effects
	 * @param entity The entity with this effect active
	 * @param otherEffectInstance The new effect instance attempting application
	 * @return Whether the new effect should apply or not
	 */
	public boolean canApplyOther(LivingEntity entity, MobEffectInstance otherEffectInstance) {
		return true;
	}

	/**
	 * Handle for when the effect is first applied to an entity. Useful for triggering additional effects or up-front additional handling.
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param source The direct entity source of the effect, if applicable. E.G. The player drinking a potion, the {@link net.minecraft.world.entity.projectile.ThrownPotion} entity
	 * @param entity The entity the effect is applying to
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 */
	public void onApplication(@Nullable MobEffectInstance effectInstance, @Nullable Entity source, LivingEntity entity, int amplifier) {}

	/**
	 * Handle for when the effect is being applied to an entity that already has the effect applied. This is called before the game checks whether the new effect will apply at all.
	 * @param existingEffectInstance The existing MobEffectInstance that the entity already has in place
	 * @param newEffectInstance The new MobEffectInstance that is pending application
	 * @param entity The entity the effect is applying to
	 * @return The effect instance to apply in place of the one that was going to be applied
	 */
	public MobEffectInstance onReapplication(MobEffectInstance existingEffectInstance, MobEffectInstance newEffectInstance, LivingEntity entity) {
		return existingEffectInstance;
	}

	/**
	 * @see ExtendedMobEffect#onRemove(MobEffectInstance, LivingEntity)
	 * @param effectInstance
	 * @param entity
	 */
	@Deprecated(forRemoval = true)
	public void onRemoval(MobEffectInstance effectInstance, LivingEntity entity) {}

	/**
	 * Handle for when the effect is being removed from the entity.<br>
	 * This callback is <u>not</u> called when the effect is being removed by expiration. See {@link ExtendedMobEffect#onExpiry}
	 * @param effectInstance Effect instance for the effect.
	 * @param entity The entity the effect is being removed from
	 * @return true to proceed with removing, or false to prevent the effect being removed
	 */
	public boolean onRemove(MobEffectInstance effectInstance, LivingEntity entity) {
		onRemoval(effectInstance, entity);

		return true;
	}

	/**
	 * Handle for when the effect has expired (run its timer out)<br>
	 * The effect has already been removed from the entity at this stage
	 * @param effectInstance Effect instance for the effect
	 * @param entity The entity the effect was removed from
	 */
	public void onExpiry(MobEffectInstance effectInstance, LivingEntity entity) {}

	/**
	 * Check whether this effect should tick on the current tick.
	 * @see MobEffect#shouldApplyEffectTickThisTick(int, int)
	 * @param effectInstance Effect instance for the effect. Marked with nullable so that the vanilla methods can be routed through for completeness
	 * @param entity The entity the effect is ticking on
	 * @param ticksRemaining The ticks remaining on this effect before it expires
	 * @param amplifier The amplifier for the current effect instance. Included for compatibility with vanilla's methods
	 * @return Whether the effect should tick this game tick or not
	 */
	public boolean shouldTickEffect(@Nullable MobEffectInstance effectInstance, @Nullable LivingEntity entity, int ticksRemaining, int amplifier) {
		return false;
	}

	/**
	 *
	 * Disabled, use {@link ExtendedMobEffect#addAttributeModifiers(LivingEntity, AttributeMap, int)}
	 */
	@Override
	public final void addAttributeModifiers(AttributeMap attributes, int amplifier) {
		addAttributeModifiers(null, attributes, amplifier);
	}

	public void addAttributeModifiers(@Nullable LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		for (Map.Entry<Holder<Attribute>, AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
			final Holder<Attribute> attribute = entry.getKey();
			final AttributeInstance attributeInstance = attributeMap.getInstance(attribute);

			if (attributeInstance != null) {
				AttributeTemplate template = entry.getValue();
				AttributeModifier modifier = entry.getValue().create(amplifier);
				double dynamicAmount = getAttributeModifierValue(entity, attribute, modifier.amount(), amplifier);

				if (dynamicAmount != modifier.amount())
					modifier = new AttributeModifier(template.id(), dynamicAmount, modifier.operation());

				attributeInstance.removeModifier(template.id());
				attributeInstance.addPermanentModifier(modifier);
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
	public double getAttributeModifierValue(@Nullable LivingEntity entity, @Nullable Holder<Attribute> attribute, double baseModifierAmount, int effectAmplifier) {
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
	 * Handle whether an effect should be cured by a player or entity consuming this item.
	 * @param effectInstance The MobEffectInstance to be cured
	 * @param stack The ItemStack of the item being consumed
	 * @param entity The entity consuming the item
	 * @return true if the effect should be cured by consuming this item
	 */
	public boolean shouldCureEffect(MobEffectInstance effectInstance, ItemStack stack, LivingEntity entity) {
		return stack.getItem() == Items.MILK_BUCKET;
	}

	/**
	 * Handle whether an effect should be removed when the player consumes a Totem of Undying
	 *
	 * @param effectInstance The effect instance applied to the entity
	 * @param entity The entity the effect is applied to
	 * @return Whether the effect should be removed or not
	 */
	public boolean shouldBeRemovedByTotemOfDeath(MobEffectInstance effectInstance, LivingEntity entity) {
		return true;
	}

	/**
	 * Return an overlay renderer for this effect. Called when the effect is present on the entity, used for rendering screen effects
	 * Clientside code must be handled outside of the MobEffect class for server-safety
	 * @return The renderer
	 */
	@Nullable
	public EffectOverlayRenderer getOverlayRenderer() {
		return null;
	}

	/**
	 * Handle a client-side tick for the effect, usually used for custom handling particle effects
	 * @param effectInstance The effect instance
	 * @param entity The entity the effect is applied to
	 * @return Whether this effect has handled itself and wants to skip the vanilla particle functionality
	 */
	public boolean doClientSideEffectTick(MobEffectInstance effectInstance, LivingEntity entity) {
		return false;
	}

	/**
	 * Called when a MobEffectInstance with this effect is being loaded from NBT.<br>
	 * This is called prior to any values being read from the nbt tag
	 * @param nbt The compoundTag that the effect instance is loaded from
	 * @param effectInstance The effect instance being loaded
	 */
	public void read(CompoundTag nbt, MobEffectInstance effectInstance) {}

	/**
	 * Called when a MobEffectInstance with this effect is saved to NBT<br>
	 * This is called after the nbt tag has been written to
	 * @param nbt The compoundTag that the effect instance is saved to
	 * @param effectInstance The effect instance being saved
	 */
	public void write(CompoundTag nbt, MobEffectInstance effectInstance) {}

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
	public final boolean applyEffectTick(LivingEntity entity, int amplifier) {
		return tick(entity, null, amplifier);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#onApplication(MobEffectInstance, Entity, LivingEntity, int)}
	 */
	@Override
	public final void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity entity, int amplifier, double sourceModifier) {
		onApplication(null, source, entity, amplifier);

		if (!isInstantenous())
			tick(entity, null, amplifier);
	}

	/**
	 * Disabled, use {@link ExtendedMobEffect#shouldTickEffect(MobEffectInstance, LivingEntity, int, int)}
	 */
	@Override
	public final boolean shouldApplyEffectTickThisTick(int ticksRemaining, int amplifier) {
		return shouldTickEffect(null, null, ticksRemaining, amplifier);
	}
}
