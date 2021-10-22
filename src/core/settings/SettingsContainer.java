package core.settings;

import imgui.app.Color;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SettingsContainer {

    private static SettingsContainer instance;

    private final String configFile;

    private final Map<String, Setting<?>> settings;

    public SettingsContainer(String configFile) throws IOException {
        this.configFile = configFile;
        this.settings = new HashMap<>();
        //System
        settings.put("emulation_speed", new Setting<>("emulation Speed", 1));
        settings.put("rtc", new Setting<>("MBC3 RTC", false));
        settings.put("dmg_bootrom", new Setting<>("DMG Bootrom", "DMG.bin"));
        settings.put("cgb_bootrom", new Setting<>("CGB Bootrom", "CGB.bin"));
        //Graphics
        settings.put("dmg_palette_0", new Setting<>("DMG Palette 0", new Color(0xE0, 0xF8, 0xD0, 0xFF)));
        settings.put("dmg_palette_1", new Setting<>("DMG Palette 1", new Color(0x88, 0xC0, 0x70, 0xFF)));
        settings.put("dmg_palette_2", new Setting<>("DMG Palette 2", new Color(0x34, 0x58, 0x66, 0xFF)));
        settings.put("dmg_palette_3", new Setting<>("DMG Palette 3", new Color(0x08, 0x18, 0x20, 0xFF)));
        settings.put("gamma", new Setting<>("CGB Colors Gamma factor", 2f));
        settings.put("resolution_scaling", new Setting<>("Resolution factor", 2));
        //Sound
        settings.put("square_1_enabled", new Setting<>("Square Channel 1", true));
        settings.put("square_2_enabled", new Setting<>("Square Channel 2", true));
        settings.put("wave_enabled", new Setting<>("Wave Channel", true));
        settings.put("noise_enabled", new Setting<>("Noise Channel", true));
        settings.put("audio_mixer", new Setting<>("Audio Output", 3));
        settings.put("master_volume", new Setting<>("Volume", 3));

        Properties file = new Properties();
    }

    public static SettingsContainer getInstance() {
        try {
            if (instance == null)
                instance = new SettingsContainer("GBemu.ini");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public Setting<?> getSetting(String name) {
        return settings.get(name);
    }

    public void setSetting(String name, Object value) {
        Setting setting = settings.get(name);
        if (setting.getType() == value.getClass())
            setting.setValue((setting.getType()).cast(value));
    }

    public static class Setting<T> {

        private final String identifier;
        private T value;

        public Setting(String identifier, T defaultValue) {
            this.identifier = identifier;
            this.value = defaultValue;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Class<?> getType() {
            return value.getClass();
        }
    }
}
