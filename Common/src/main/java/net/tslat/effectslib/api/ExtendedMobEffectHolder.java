package net.tslat.effectslib.api;

/**
 * Data holder class for typeless additional data an {@link ExtendedMobEffect} might want to store
 */
public interface ExtendedMobEffectHolder {
	Object setExtendedMobEffectData();
	void getExtendedMobEffectData(Object data);
}
