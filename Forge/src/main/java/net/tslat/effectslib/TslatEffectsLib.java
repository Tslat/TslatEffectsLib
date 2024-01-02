package net.tslat.effectslib;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.tslat.effectslib.command.TELCommand;
import net.tslat.effectslib.networking.TELNetworking;

@Mod(TELConstants.MOD_ID)
public class TslatEffectsLib {
    public TslatEffectsLib()  {
        MinecraftForge.EVENT_BUS.addListener(TslatEffectsLib::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(TslatEffectsLib::serverStarted);
        TELNetworking.init();
    }

    private static void registerCommands(final RegisterCommandsEvent ev) {
        TELCommand.registerSubcommands(ev.getDispatcher(), ev.getBuildContext());
    }

    private static void serverStarted(final ServerStartedEvent ev) {
        TELConstants.SERVER = ev.getServer();
    }
}
