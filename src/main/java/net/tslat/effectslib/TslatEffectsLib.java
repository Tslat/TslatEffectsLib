package net.tslat.effectslib;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.tslat.effectslib.TslatEffectsLib.MOD_ID;

@Mod(MOD_ID)
public class TslatEffectsLib {
	public static final String VERSION = "1.0";
	public static final String MOD_ID = "tslateffectslib";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public TslatEffectsLib() {}
}
