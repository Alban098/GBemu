package gbemu.settings;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.settings.wrapper.*;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.lwjgl.glfw.GLFW;
import utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class represent a Container with all Settings of the emulator,
 * all settings interaction flows through this container
 */
public class SettingsContainer {

    private final String file;
    private final Map<SettingIdentifiers, Setting<?>> settings;
    private final GameBoy gameboy;

    /**
     * Create a new SettingsContainer
     * @param gameboy the Game Boy to link to
     * @param file the file to load settings from and save to
     */
    public SettingsContainer(GameBoy gameboy, String file) {
        this.settings = new HashMap<>();
        this.gameboy = gameboy;
        this.file = file;

        //System
        settings.put(SettingIdentifiers.RTC, new Setting<>(SettingIdentifiers.RTC, new BooleanWrapper(false), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SPEED, new Setting<>(SettingIdentifiers.SPEED, new IntegerWrapper(1), (Setting<IntegerWrapper> setting) -> {
            int[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderInt(setting.getIdentifier().getDescription(), tmp, 1, 5)) {
                setting.getValue().wrap(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.BOOTSTRAP, new Setting<>(SettingIdentifiers.BOOTSTRAP, new BooleanWrapper(false), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_BOOTROM, new Setting<>(SettingIdentifiers.DMG_BOOTROM, new StringWrapper("DMG.bin"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("", tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load DMG")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc, .bin)", "*.gb", "*.gbc", "*.bin"));
                Platform.runLater(() -> {
                    File dmg = chooser.showOpenDialog(null);
                    if (dmg != null) {
                        try {
                            setting.getValue().wrap(dmg.getAbsolutePath());
                            if (dmg.length() != 0x100)
                                throw new Exception("Invalid DMG Size (must be 256 bytes");
                            gameboy.propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.CHEAT_DATABASE, new Setting<>(SettingIdentifiers.CHEAT_DATABASE, new StringWrapper("gameshark.cht"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("", tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameShark Database", ".cht"));
                Platform.runLater(() -> {
                    File cht = chooser.showOpenDialog(null);
                    if (cht != null) {
                        try {
                            setting.getValue().wrap(cht.getAbsolutePath());
                            gameboy.propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.CGB_BOOTROM, new Setting<>(SettingIdentifiers.CGB_BOOTROM, new StringWrapper("CGB.bin"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("", tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load CGB")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc, .bin)", "*.gb", "*.gbc", "*.bin"));
                Platform.runLater(() -> {
                    File cgb = chooser.showOpenDialog(null);
                    if (cgb != null) {
                        try {
                            setting.getValue().wrap(cgb.getAbsolutePath());
                            if (cgb.length() != 0x900)
                                throw new Exception("Invalid CGB Size (must be 2304 bytes");
                            gameboy.propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        //Graphics
        settings.put(SettingIdentifiers.DMG_PALETTE_0, new Setting<>( SettingIdentifiers.DMG_PALETTE_0, new ColorWrapper(0xE0, 0xF8, 0xD0, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_1, new Setting<>(SettingIdentifiers.DMG_PALETTE_1, new ColorWrapper(0x88, 0xC0, 0x70, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_2, new Setting<>( SettingIdentifiers.DMG_PALETTE_2, new ColorWrapper(0x34, 0x58, 0x66, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_3, new Setting<>(SettingIdentifiers.DMG_PALETTE_3, new ColorWrapper(0x08, 0x18, 0x20, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.GAMMA, new Setting<>(SettingIdentifiers.GAMMA, new FloatWrapper(2f), (Setting<FloatWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 1f, 3f)) {
                setting.getValue().wrap(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        //Sound
        settings.put(SettingIdentifiers.SQUARE_1_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_1_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SQUARE_2_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_2_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.WAVE_ENABLED, new Setting<>(SettingIdentifiers.WAVE_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.NOISE_ENABLED, new Setting<>(SettingIdentifiers.NOISE_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.VOLUME, new Setting<>(SettingIdentifiers.VOLUME, new FloatWrapper(1f), (Setting<FloatWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 0f, 1f)) {
                setting.getValue().wrap(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.KEYBOARD_CONTROL_MAP, new Setting<>(SettingIdentifiers.KEYBOARD_CONTROL_MAP, Button.getKeyboardMap(), (Setting<HashMapWrapper<ButtonWrapper, IntegerWrapper>> setting) -> {
            for (Button button : Button.values()) {
                if (ImGui.button(button.name(), 80, 20)) {
                    while(true) {
                        if (ImGui.isKeyDown(GLFW.GLFW_KEY_ESCAPE))
                            break;
                        IntegerWrapper keycode = new IntegerWrapper(Utils.getPressedKey());
                        if (keycode.unwrap() != -1 && !setting.getValue().containsValue(keycode)) {
                            setting.getValue().put(new ButtonWrapper(button), keycode);
                            break;
                        }

                    }
                }
                ImGui.sameLine(120);
                ImGui.text(Utils.getKeyName(setting.getValue().get(new ButtonWrapper(button)).unwrap()));
            }
        }));
    }

    /**
     * Load settings from the settings file
     */
    public void loadFile() {
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(file));
            for (Setting<?> setting : settings.values())
                setting.deserialize(prop.getProperty(setting.getIdentifier().toString()));
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when saving settings : " + e.getMessage());
        }
        applySettings();
    }

    /**
     * Save settings to the settings file
     */
    public void saveFile() {
        try {
            Properties prop = new Properties();
            for (Setting<?> setting : settings.values())
                prop.put(setting.getIdentifier().toString(), setting.serialize());
            prop.store(new FileWriter(file), null);
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when saving settings : " + e.getMessage());
        }
    }

    /**
     * Get a settings by his identifier
     * @param name the identifier to find
     * @return the corresponding Setting, null if not found
     */
    public Setting<?> getSetting(SettingIdentifiers name) {
        return settings.get(name);
    }

    /**
     * Apply all the settings to the emulator
     */
    private void applySettings() {
        for (Setting<?> setting : settings.values())
            gameboy.propagateSetting(setting);
    }

    /**
     * Apply a palette to the emulator if in DMG mode
     * @param colors the colors to apply
     */
    public void applyPalette(int[] colors) {
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_0).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_1).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_2).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_3).getValue()).wrap(new Color(colors[0]));

        gameboy.propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_0));
        gameboy.propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_1));
        gameboy.propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_2));
        gameboy.propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_3));
    }
}
