package net.tslat.effectslib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.tslat.effectslib.command.TELCommand;

public class TslatEffectsLib implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(((dispatcher, buildContext, environment) -> TELCommand.registerSubcommands(dispatcher, buildContext)));
		TELCommon.init();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> TELConstants.SERVER = server);
	}
}
