package net.tslat.effectslib;

import net.minecraft.server.MinecraftServer;
import net.tslat.effectslib.networking.TELNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class TELConstants {
	public static final String VERSION = "1.7";
	public static final String MOD_ID = "tslateffectslib";
	public static final String MOD_NAME = "TslatEffectsLib";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
	public static MinecraftServer SERVER = null;

	public static final TELCommon COMMON = ServiceLoader.load(TELCommon.class).findFirst().get();
	public static final TELNetworking NETWORKING = ServiceLoader.load(TELNetworking.class).findFirst().get();
}