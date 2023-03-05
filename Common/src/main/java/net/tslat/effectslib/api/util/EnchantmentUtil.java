package net.tslat.effectslib.api.util;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.api.ExtendedEnchantment;

import javax.annotation.Nullable;
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
	public static Pair<Integer, Map<ItemStack, List<EnchantmentInstance>>> collectAllEnchantments(LivingEntity entity, boolean filterForExtendedEnchants) {
		Map<ItemStack, List<EnchantmentInstance>> enchantMap = new Object2ObjectOpenHashMap<>();
		int total = 0;

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (stack.isEmpty())
				continue;

			List<EnchantmentInstance> enchants = getStackEnchantmentsForUse(entity, stack, slot, filterForExtendedEnchants);

			if (!enchants.isEmpty()) {
				for (EnchantmentInstance instance : enchants) {
					total += instance.level;
				}

				enchantMap.put(stack, enchants);
			}
		}

		return Pair.of(total, enchantMap);
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

		for (Map.Entry<Enchantment, Integer> enchantEntry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
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
		for (Map.Entry<Enchantment, Integer> enchantEntry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
			if (enchantEntry.getKey() == enchant)
				return new EnchantmentInstance(enchantEntry.getKey(), enchantEntry.getValue());
		}

		return null;
	}
}
