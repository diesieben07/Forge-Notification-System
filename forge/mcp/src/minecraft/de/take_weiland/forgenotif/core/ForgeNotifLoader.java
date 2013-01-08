package de.take_weiland.forgenotif.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class ForgeNotifLoader implements IFMLLoadingPlugin {

	@Override
	public String[] getLibraryRequestClass() {
		return null;
	}

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return "de.take_weiland.forgenotif.core.ForgeNotifModContainer";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) { }
}