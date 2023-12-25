package net.tslat.effectslib.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Extension class of {@link net.minecraft.world.item.enchantment.Enchantment Enchantment}
 */
public class ExtendedEnchantment extends Enchantment {
	protected CalculationType levelCalculationType = CalculationType.MAX;

	public ExtendedEnchantment(EnchantmentCategory category) {
		this(Rarity.COMMON, category);
	}

	public ExtendedEnchantment(Rarity rarity, EnchantmentCategory category) {
		super(rarity, category, new EquipmentSlot[0]);
	}

	/**
	 * Set the computation type for this enchantment's level for a given entity when the game attempts to get the enchantment level.<br>
	 * @see CalculationType
	 */
	public ExtendedEnchantment setLevelCalculationType(CalculationType type) {
		this.levelCalculationType = type;

		return this;
	}

	/**
	 * Enchantment function applicability method.<br>
	 * Use this method to determine whether your enchantment should apply for the current check.<br>
	 * This allows for context-sensitive usage, as opposed to vanilla's rigid slot-based checks
	 * @param stack The ItemStack the enchant is on
	 * @param level The level of the enchantment being checked for
	 * @param entity The entity the stack comes from (or null if no entity available for this context)
	 * @param slot The slot the stack comes from (or null if no slot available for this context)
	 */
	public boolean isApplicable(ItemStack stack, int level, @Nullable LivingEntity entity, @Nullable EquipmentSlot slot) {
		return true;
	}

	/**
	 * Replacement for {@link EnchantmentHelper#getEnchantmentLevel(Enchantment, LivingEntity)}, to calculate the total enchantment level for this entity.<br>
	 * Incorporates {@link CalculationType custom calculation} as needed
	 */
	public int getTotalEnchantmentLevel(LivingEntity entity) {
		int total = 0;

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (!stack.isEmpty() && this.category.canEnchant(stack.getItem())) {
				EnchantmentInstance instance = EnchantmentUtil.getEnchantInstanceForStack(this, stack);

				if (instance != null)
					total = this.levelCalculationType.combine(total, instance.level);
			}
		}

		return total;
	}

	/**
	 * Context-sensitive override for {@link Enchantment#canEnchant(ItemStack)}.<br>
	 * This allows for applicability to be determined by individual sources
	 * @param stack The ItemStack to be enchanted
	 * @param enchantSource The source of the enchantment.
	 * @see ExtendedEnchantment#ANVIL
	 * @see ExtendedEnchantment#ENCHANTING_TABLE
	 * @see ExtendedEnchantment#COMMAND
	 * @see ExtendedEnchantment#LOOT
	 * @see ExtendedEnchantment#OTHER
	 * @return Whether the item can be enchanted or not with this enchantment
	 */
	public boolean canEnchant(ItemStack stack, String enchantSource) {
		return this.category.canEnchant(stack.getItem());
	}

	/**
	 * Return whether the {@link net.minecraft.world.inventory.GrindstoneMenu#removeNonCurses(ItemStack, int, int) Grindstone} should remove this enchantment when repairing an item with this on it
	 * <p>Returning true from here will remove the enchantment, even if it is a {@link Enchantment#isCurse() curse}</p>
	 * @param stack The ItemStack being modified
	 * @return Whether the enchantment should be removed by the Grindstone or not, or null to default to vanilla behaviour
	 */
	@Nullable
	public Boolean shouldGrindstoneRemove(ItemStack stack) {
		return null;
	}

	public static final String ANVIL = "anvil";
	public static final String ENCHANTING_TABLE = "enchanting_table";
	public static final String COMMAND = "command";
	public static final String LOOT = "loot";
	public static final String OTHER = "other";

	/**
	 * Adjust the attack damage for an attack an entity with this enchantment will receive.
	 * @param entity The entity that is being attacked
	 * @param source The DamageSource for the attack
	 * @param baseAmount The current amount of damage to be dealt
	 * @param stack The ItemStack the enchantment is on
	 * @param level The enchantment level of the enchantment
	 * @param totalLevel The total enchantment level of all stacks on the player with this enchantment
	 * @return The new amount of damage to be dealt
	 */
	public float modifyIncomingAttackDamage(LivingEntity entity, DamageSource source, float baseAmount, ItemStack stack, int level, int totalLevel) {
		return baseAmount;
	}

	/**
	 * Adjust the attack damage for an attack that an entity with this enchantment will perform.
	 * @param entity The entity that is attacking
	 * @param target The target of the attack
	 * @param source The DamageSource for the attack
	 * @param baseAmount The current amount of damage to be dealt
	 * @param stack The ItemStack the enchantment is on
	 * @param level The enchantment level of the enchantment
	 * @param totalLevel The total enchantment level of all stacks on the player with this enchantment
	 * @return The new amount of damage to be dealt
	 */
	public float modifyOutgoingAttackDamage(LivingEntity entity, LivingEntity target, DamageSource source, float baseAmount, ItemStack stack, int level, int totalLevel) {
		return baseAmount;
	}

	/**
	 * Trigger an effect or otherwise listen for an attack that has successfully received by an entity with this enchantment applied.
	 * @param entity The entity being attacked
	 * @param source The DamageSource for the attack
	 * @param amount The amount of being dealt
	 * @param stack The ItemStack this method is being called for
	 * @param level The level of the enchantment on the given stack
	 * @param totalLevel The total enchantment level of all stacks on the player with this enchantment
	 * @param isLastStack Whether the current method call is the last ItemStack that will have this method called for this attack. Useful for effects you may only want to happen once per attack
	 */
	public void afterIncomingAttack(LivingEntity entity, DamageSource source, float amount, ItemStack stack, int level, int totalLevel, boolean isLastStack) {}

	/**
	 * Trigger an effect or otherwise listen for an attack that has successfully been dealt by an entity with this effect active.
	 * @param entity The entity that is attacking
	 * @param source The DamageSource for the attack
	 * @param amount The amount of being dealt
	 * @param stack The ItemStack this method is being called for
	 * @param level The level of the enchantment on the given stack
	 * @param totalLevel The total enchantment level of all stacks on the player with this enchantment
	 * @param isLastStack Whether the current method call is the last ItemStack that will have this method called for this attack. Useful for effects you may only want to happen once per attack
	 */
	public void afterOutgoingAttack(LivingEntity entity, LivingEntity victim, DamageSource source, float amount, ItemStack stack, int level, int totalLevel, boolean isLastStack) {}

	/**
	 * Trigger an effect or otherwise listen for when an entity equips an item with this enchantment on it
	 * <p><b><u>NOTE:</u></b> Due to vanilla's weird implementation of inventory management, this method will also be called when a stack's NBT, capabilities, or size changes.</p>
	 * @param entity The entity equipping the item
	 * @param slot The slot the item is being equipped in
	 * @param from The ItemStack that was previously equipped
	 * @param to The new ItemStack being equipped
	 * @param level The enchantment level on the new ItemStack
	 */
	public void onEquip(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to, int level) {}

	/**
	 * Trigger an effect or otherwise listen for when an entity unequips an item with this enchantment on it
	 * <p><b><u>NOTE:</u></b> Due to vanilla's weird implementation of inventory management, this method will also be called when a stack's NBT, capabilities, or size changes.</p>
	 * @param entity The entity equipping the item
	 * @param slot The slot the item is being equipped in
	 * @param from The ItemStack that was previously equipped
	 * @param to The new ItemStack being equipped
	 * @param level The enchantment level on the new ItemStack
	 */
	public void onUnequip(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to, int level) {}

	/**
	 * Perform any tick-based functionalities that your Enchantment should have for the given entity and stack
	 * @param entity The entity that is currently being ticked
	 * @param stack The ItemStack the enchantment is on
	 */
	public void tick(LivingEntity entity, ItemStack stack) {}

	/**
	 * The enchantment level calculation type for this enchantment.<br>
	 * Change the way your enchantment is calculated for usage
	 */
	public enum CalculationType {
		/**
		 * Every ItemStack with this enchantment adds its level to the total.<br>
		 * Final enchantment level is the sum of all enchantment instances on the entity for this enchantment.
		 */
		ADDITIVE,
		/**
		 * Every ItemStack with this enchantment multiplies the current total by its level.<br>
		 * Final enchantment level is the product of all enchantment instances on the entity for this enchantment.
		 */
		MULTIPLICATIVE,
		/**
		 * Returns the highest level of all applicable stacks on this entity.<br>
		 * This is the vanilla/default functionality
		 */
		MAX;

		public final int combine(int total, int newValue) {
			if (this == MAX) {
				return Math.max(total, newValue);
			}
			else if (this == ADDITIVE) {
				return total + newValue;
			}
			else {
				return total * newValue;
			}
		}
	}

	public CalculationType getLevelCalculationType() {
		return this.levelCalculationType;
	}

	// Disabled methods below //

	/**
	 * Disabled, use {@link ExtendedEnchantment#canEnchant(ItemStack, String)}
	 */
	public final boolean canEnchant(ItemStack stack) {
		return canEnchant(stack, OTHER);
	}

	/**
	 * Disabled, use {@link ExtendedEnchantment#isApplicable}
	 */
	@Override
	public final Map<EquipmentSlot, ItemStack> getSlotItems(LivingEntity entity) {
		Map<EquipmentSlot, ItemStack> slots = new Object2ObjectOpenHashMap<>();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (!stack.isEmpty() && this.category.canEnchant(stack.getItem())) {
				EnchantmentInstance enchantInstance = EnchantmentUtil.getEnchantInstanceForStack(this, stack);

				if (enchantInstance != null && isApplicable(stack, enchantInstance.level, entity, slot))
					slots.put(slot, stack);
			}
		}

		return slots;
	}
}
