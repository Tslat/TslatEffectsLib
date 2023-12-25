package net.tslat.effectslib;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

public class TELItemStackData {
    private Map<Enchantment, Integer> enchantsCache = Map.of();
    private long lastHash = -1;

    private void computeCachedEnchantments(ItemStack stack) {
        final ListTag enchants = stack.is(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantments(stack) : stack.getEnchantmentTags();

        if (this.lastHash != enchants.hashCode()) {
            this.enchantsCache = EnchantmentHelper.deserializeEnchantments(enchants);
            this.lastHash = enchants.hashCode();
        }
    }

    public Map<Enchantment, Integer> getCachedEnchantments(ItemStack stack) {
        if (!stack.hasTag())
            return Map.of();

        computeCachedEnchantments(stack);

        return this.enchantsCache;
    }

    public static TELItemStackData getDataFor(ItemStack stack) {
        return stack.getData(TslatEffectsLib.ITEMSTACK_DATA.get());
    }
}