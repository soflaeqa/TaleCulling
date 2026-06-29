package moe.taleculling.taleculling;

import org.bukkit.configuration.ConfigurationSection;

public class SettingsHolder {

	private int tileRange; // for tiles only (for nyow)
    private boolean hideLivingEntities;

	public void load(ConfigurationSection section) {
		tileRange = section.getInt("tileRange");
        hideLivingEntities = section.getBoolean("hideLivingEntities", true);
	}

	public int getTileRange() {
		return tileRange;
	}
    public boolean isHideLivingEntities() {
        return hideLivingEntities;
    }
}
