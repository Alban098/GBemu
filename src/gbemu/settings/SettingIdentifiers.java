package gbemu.settings;

import java.util.Locale;

/**
 * This enum contains all possible settings Identifiers
 */
public enum SettingIdentifiers {
    RTC("MBC3 RTC Capability"),
    SPEED("Speed Mult."),
    DMG_BOOTROM("DMG Bootrom path"),
    CGB_BOOTROM("CGB Bootrom path"),
    DMG_PALETTE_0("DMG Color 0"),
    DMG_PALETTE_1("DMG Color 1"),
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
    FILTER_SETTINGS("Filter file location"),
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

    public static SettingIdentifiers get(String attribute) {
        return switch (attribute.toLowerCase(Locale.ROOT)) {
            case "rtc" -> RTC;
            case "speed" -> SPEED;
            case "dmg_bootrom" -> DMG_BOOTROM;
            case "cgb_bootrom" -> CGB_BOOTROM;
            case "dmg_palette_0" -> DMG_PALETTE_0;
            case "dmg_palette_1" -> DMG_PALETTE_1;
            case "dmg_palette_2" -> DMG_PALETTE_2;
            case "dmg_palette_3" -> DMG_PALETTE_3;
            case "gamma" -> GAMMA;
            case "square_1_enabled" -> SQUARE_1_ENABLED;
            case "square_2_enabled" -> SQUARE_2_ENABLED;
            case "wave_enabled" -> WAVE_ENABLED;
            case "noise_enabled" -> NOISE_ENABLED;
            case "pulse_mode" -> PULSE_MODE;
            case "pulse_harmonics" -> PULSE_HARMONICS;
            case "volume" -> VOLUME;
            case "bootstrap" -> BOOTSTRAP;
            case "cheat_database" -> CHEAT_DATABASE;
            case "filter_settings" -> FILTER_SETTINGS;
            case "keyboard_control_map" -> KEYBOARD_CONTROL_MAP;
            case "joypad_control_map" -> JOYPAD_CONTROL_MAP;
            default -> null;
        };
    }

    /**
     * Return the description of the Setting
     * @return the Setting's description
     */
    public String getDescription() {
        return description;
    }
}