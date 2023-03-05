package net.tslat.effectslib.api;

/**
 * Data holder class for typeless additional data an {@code ExtendedMobEffect} might want to store
 */
public interface ExtendedMobEffectHolder {
	Object getExtendedMobEffectData();
	void setExtendedMobEffectData(Object data);
}
