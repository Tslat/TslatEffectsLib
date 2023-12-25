package net.tslat.effectslib;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface TELStackCapability {
    ResourceLocation ID = new ResourceLocation(TELConstants.MOD_ID, "tel_stack");

    static Impl getDataFor(ItemStack stack) {
        return (Impl)stack.getCapability(Provider.CAPABILITY).orElse(new Impl(stack));
    }

    class Impl implements TELStackCapability {
        private final ItemStack stack;
        private Map<Enchantment, Integer> enchantsCache = Map.of();
        private long lastHash = -1;

        public Impl(ItemStack stack) {
            this.stack = stack;
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
    }

    @Mod.EventBusSubscriber(modid = TELConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    class Provider implements ICapabilityProvider {
        public static final Capability<TELStackCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
        private final LazyOptional<TELStackCapability> implContainer;

        public Provider(ItemStack stack) {
            this.implContainer = LazyOptional.of(() -> new TELStackCapability.Impl(stack));
        }

        @SubscribeEvent
        public static void register(final RegisterCapabilitiesEvent ev) {
            ev.register(TELStackCapability.class);
            MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, Provider::attach);
        }

        private static void attach(final AttachCapabilitiesEvent<ItemStack> ev) {
            ev.addCapability(ID, new Provider(ev.getObject()));
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY == cap ? implContainer.cast() : LazyOptional.empty();
        }
    }
}