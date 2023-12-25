package net.tslat.effectslib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tslat.effectslib.command.TELCommand;

import java.util.function.Supplier;

@Mod(TELConstants.MOD_ID)
public class TslatEffectsLib {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TELConstants.MOD_ID);

    protected static final Supplier<AttachmentType<TELItemStackData>> ITEMSTACK_DATA = ATTACHMENT_TYPES.register("tel_stack", () -> AttachmentType.builder(TELItemStackData::new).build());

    public TslatEffectsLib(IEventBus modBus)  {
        NeoForge.EVENT_BUS.addListener(TslatEffectsLib::registerCommands);
        NeoForge.EVENT_BUS.addListener(TslatEffectsLib::serverStarted);
        ATTACHMENT_TYPES.register(modBus);
        TELCommon.init();
    }

    private static void registerCommands(final RegisterCommandsEvent ev) {
        TELCommand.registerSubcommands(ev.getDispatcher(), ev.getBuildContext());
    }

    private static void serverStarted(final ServerStartedEvent ev) {
        TELConstants.SERVER = ev.getServer();
    }
}
