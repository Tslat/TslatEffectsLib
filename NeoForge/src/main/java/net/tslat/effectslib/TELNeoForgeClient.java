package net.tslat.effectslib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class TELNeoForgeClient {
    @SubscribeEvent
    public static void clientTick(final ClientTickEvent.Post ev) {
        TELClient.tickParticleTransitions();
    }
}