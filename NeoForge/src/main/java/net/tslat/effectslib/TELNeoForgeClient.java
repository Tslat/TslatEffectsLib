package net.tslat.effectslib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class TELNeoForgeClient {
    @SubscribeEvent
    public static void clientTick(final TickEvent.ClientTickEvent ev) {
        TELClient.tickParticleTransitions();
    }
}