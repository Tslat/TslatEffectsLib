package net.tslat.effectslib;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.item.ItemComponent;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

public class TELStackComponent extends ItemComponent {
    public static final ComponentKey<TELStackComponent> KEY = ComponentRegistry.getOrCreate(new ResourceLocation(TELConstants.MOD_ID, "tel_stack"), TELStackComponent.class);
    private Map<Enchantment, Integer> enchantsCache = Map.of();
    private long lastHash = -1;

    public TELStackComponent(ItemStack stack) {
        super(stack);
    }

    private void computeCachedEnchantments() {
        final ListTag enchants = this.stack.is(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantments(this.stack) : this.stack.getEnchantmentTags();

        if (this.lastHash != enchants.hashCode()) {
            this.enchantsCache = EnchantmentHelper.deserializeEnchantments(enchants);
            this.lastHash = enchants.hashCode();
        }
    }

    public Map<Enchantment, Integer> getCachedEnchantments() {
        if (!this.stack.hasTag())
            return Map.of();

        computeCachedEnchantments();

        return this.enchantsCache;
    }

    public static TELStackComponent getDataFor(ItemStack stack) {
        return KEY.get(stack);
    }
}
