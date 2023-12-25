package net.tslat.effectslib.api.util;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.TELConstants;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Helper class for performing miscellaneous work relating to {@link net.minecraft.world.item.enchantment.Enchantment Enchantments}
 */
public final class EnchantmentUtil {
	/**
	 * Gathers all the enchantment information from the given entity, keeping it all cached for efficient usage
	 * @param entity The entity to get the enchantment information from
	 * @param filterForExtendedEnchants Whether to skip any enchants that aren't {@link ExtendedEnchantment ExtendedEnchantments}
	 * @return The total enchantment level, and full list of enchantments and the stacks they are on
	 */
	public static Map<Enchantment, EntityEnchantmentData> collectAllEnchantments(LivingEntity entity, boolean filterForExtendedEnchants) {
		Map<Enchantment, EntityEnchantmentData> data = new Object2ObjectOpenHashMap<>();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (stack.isEmpty())
				continue;

			for (EnchantmentInstance instance : getStackEnchantmentsForUse(entity, stack, slot, filterForExtendedEnchants)) {
				data.computeIfAbsent(instance.enchantment, EntityEnchantmentData::new).accountStack(stack, instance.level);
			}
		}

		return data;
	}

	/**
	 * Get the raw Enchantment:Level map for the given ItemStack
	 * <p>This allows TEL to take advantage of cached enchantments to increase performance</p>
	 */
	public static Map<Enchantment, Integer> getRawStackEnchantments(ItemStack stack) {
		if (stack.isEmpty() || !stack.isEnchanted())
			return Map.of();

		return TELConstants.COMMON.getEnchantmentsFromStack(stack);
	}

	/**
	 * Get all enchantments on this ItemStack for the context of usage for handling
	 * @param entity The entity that this stack belongs to
	 * @param stack The ItemStack to get the enchantments from
	 * @param slot The slot the stack is equipped in
	 * @param filterForExtendedEnchants Whether to skip any enchantments that aren't {@link ExtendedEnchantment Extended Enchantments}
	 */
	public static List<EnchantmentInstance> getStackEnchantmentsForUse(LivingEntity entity, ItemStack stack, EquipmentSlot slot, boolean filterForExtendedEnchants) {
		List<EnchantmentInstance> enchants = new ObjectArrayList<>();

		for (Map.Entry<Enchantment, Integer> enchantEntry : getRawStackEnchantments(stack).entrySet()) {
			EnchantmentInstance instance = new EnchantmentInstance(enchantEntry.getKey(), enchantEntry.getValue());

			if (instance.enchantment instanceof ExtendedEnchantment extendedEnchant) {
				if (extendedEnchant.isApplicable(stack, instance.level, entity, slot))
					enchants.add(instance);
			}
			else if (!filterForExtendedEnchants)
				enchants.add(instance);
		}

		return enchants;
	}

	/**
	 * Get an EnchantmentInstance for the provided ItemStack, if applicable
	 * @param stack The stack to get the enchantment from
	 * @return The instance retrieved from the stack, or null if this enchantment is not on the provided stack
	 */
	@Nullable
	public static EnchantmentInstance getEnchantInstanceForStack(Enchantment enchant, ItemStack stack) {
		for (Map.Entry<Enchantment, Integer> enchantEntry : getRawStackEnchantments(stack).entrySet()) {
			if (enchantEntry.getKey() == enchant)
				return new EnchantmentInstance(enchantEntry.getKey(), enchantEntry.getValue());
		}

		return null;
	}

	/**
	 * Container object that holds collated enchantment data for an entity.
	 */
	public static class EntityEnchantmentData {
		private final Enchantment enchant;
		private final List<IntObjectPair<ItemStack>> stacks = new ObjectArrayList<>();
		private int totalLevel = 0;

		public EntityEnchantmentData(Enchantment enchant) {
			this.enchant = enchant;
		}

		public Enchantment getEnchantment() {
			return this.enchant;
		}

		public List<IntObjectPair<ItemStack>> getEnchantedStacks() {
			return this.stacks;
		}

		public int getTotalEnchantmentLevel() {
			return this.totalLevel;
		}

		/**
		 * Return what percentage (0-1 inclusive) of the totalLevel this stack represents for this enchantment
		 */
		public float fractionOfTotal(ItemStack stack) {
			for (IntObjectPair<ItemStack> entry : this.stacks) {
				if (entry.second() == stack)
					return entry.firstInt() / (float)this.totalLevel;
			}

			return 0;
		}

		public void accountStack(ItemStack stack, int level) {
			this.stacks.add(IntObjectPair.of(level, stack));
			this.totalLevel += level;
		}
	}
}
