package gbemu.settings;

/**
 * This enum contains all possible settings Identifiers
 */
public enum SettingIdentifiers {
    RTC("MBC3 RTC Capability"),
    SPEED("Speed Mult."),
    DMG_BOOTROM("DMG Bootrom path"),
    CGB_BOOTROM("CGB Bootrom path"),
    DMG_PALETTE_0("DMG Color 0"),
    DMG_PALETTE_1 ("DMG Color 1"),
    DMG_PALETTE_2("DMG Color 2"),
    DMG_PALETTE_3("DMG Color 3"),
    GAMMA("Gamma"),
    SQUARE_1_ENABLED("Square Channel 1"),
    SQUARE_2_ENABLED("Square Channel 2"),
    WAVE_ENABLED("Wave Channel"),
    NOISE_ENABLED("Noise Channel"),
    PULSE_MODE("Square Channel Filtering"),
    PULSE_HARMONICS("Filtering harmonics"),
    VOLUME("Volume"),
    BOOTSTRAP("Enable Bootstrap"),
    CHEAT_DATABASE("Cheats Database"),
    KEYBOARD_CONTROL_MAP("Keyboard Controls"),
    JOYPAD_CONTROL_MAP("Joypad Controls");

    private final String description;

    /**
     * Create a new Identifier
     * @param description the Setting's description to be displayed on screen
     */
    SettingIdentifiers(String description) {
        this.description = description;
    }

    /**
     * Return the description of the Setting
     * @return the Setting's description
     */
    public String getDescription() {
        return description;
    }
}