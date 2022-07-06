package gbemu.settings;



public interface SettingsContainerListener {

    /**
     * Propagate the value of a Setting to the right component of the Game Boy
     * @param setting the setting to propagate
     */
    void propagateSetting(Setting<?> setting);
}
