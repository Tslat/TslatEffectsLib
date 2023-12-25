package net.tslat.effectslib;

import dev.onyxstudios.cca.api.v3.item.ItemComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.item.ItemComponentInitializer;

public final class TELComponentRegistrar implements ItemComponentInitializer {
    @Override
    public void registerItemComponentFactories(ItemComponentFactoryRegistry registry) {
        registry.register(item -> true, TELStackComponent.KEY, TELStackComponent::new);
    }
}