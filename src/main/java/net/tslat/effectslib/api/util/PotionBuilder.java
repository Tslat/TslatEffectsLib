package net.tslat.effectslib.api.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.ArrayList;

/**
 * Helper class to make building potion ItemStacks easier.
 */
public final class PotionBuilder {
	private final Item potionItem;
	private String displayName = null;
	private boolean translatable = false;
	private ArrayList<MobEffectInstance> effects = null;
	private Integer colour = null;
	private boolean dynamicColour = true;

	public PotionBuilder(Item potionItem) {
		this.potionItem = potionItem;
	}

	/**
	 * Set the literal display name of the potion ItemStack.
	 * @param name The literal name of the stack
	 * @return this
	 */
	public PotionBuilder withName(String name) {
		this.displayName = name;

		return this;
	}

	/**
	 * Set the locale key for the display name of the potion ItemStack
	 * @param nameLangKey The locale key
	 * @return this
	 */
	public PotionBuilder withTranslationKey(String nameLangKey) {
		this.displayName = nameLangKey;
		this.translatable = true;

		return this;
	}

	/**
	 * Add an additional effect to the potion ItemStack.
	 * @param effect The effect instance
	 * @return this
	 */
	public PotionBuilder addEffect(MobEffectInstance effect) {
		if (effects == null)
			effects = new ArrayList<MobEffectInstance>(1);

		this.effects.add(effect);

		return this;
	}

	/**
	 * Set the colour of the potion in the stack.
	 * @param colour The RGB packed colour int
	 * @return this
	 */
	public PotionBuilder withColour(int colour) {
		this.colour = Integer.parseInt(String.valueOf(colour), 16);
		this.dynamicColour = false;

		return this;
	}

	/**
	 * Build a potion {@link ItemStack} based on the builder's current state.
	 * @return The ItemStack
	 */
	public ItemStack build() {
		ItemStack stack = new ItemStack(potionItem);
		CompoundTag nbt = stack.getOrCreateTag();
		CompoundTag displayTag = stack.getOrCreateTagElement("display");

		if (displayName != null)
			stack.setHoverName(translatable ? Component.translatable(displayName) : Component.literal(displayName));

		if (dynamicColour && effects != null)
			colour = PotionUtils.getColor(effects);

		if (colour != null)
			nbt.putString("CustomPotionColor", String.valueOf(colour));

		if (effects != null && !effects.isEmpty())
			PotionUtils.setCustomEffects(stack, effects);

		if (!displayTag.isEmpty())
			nbt.put("display", displayTag);

		return stack;
	}
}
