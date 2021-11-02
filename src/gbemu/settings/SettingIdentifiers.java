package gbemu.settings;

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
    VOLUME("Volume"),
    BOOTSTRAP("Enable Bootstrap"),
    CHEAT_DATABASE("Cheats Database");

    private final String description;

    SettingIdentifiers(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}