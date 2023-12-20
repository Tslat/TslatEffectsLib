package net.tslat.effectslib;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class TELForgeClient {
    @SubscribeEvent
    public static void clientTick(final TickEvent.ClientTickEvent ev) {
        TELClient.tickParticleTransitions();
    }
}
