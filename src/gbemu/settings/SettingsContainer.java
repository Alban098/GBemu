package gbemu.settings;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.level.ImBoolean;
import imgui.level.ImString;
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

public class SettingsContainer {

    private final String file;
    private final Map<SettingIdentifiers, Setting<?>> settings;
    private final GameBoy gameboy;
    private long window;

    public SettingsContainer(GameBoy gameboy, String file) {
        this.settings = new HashMap<>();
        this.gameboy = gameboy;
        this.file = file;

        //System
        settings.put(SettingIdentifiers.RTC, new Setting<>(SettingIdentifiers.RTC, false, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SPEED, new Setting<>(SettingIdentifiers.SPEED, 1, (Setting<Integer> setting) -> {
            int[] tmp = {setting.getValue()};
            if (ImGui.sliderInt(setting.getIdentifier().getDescription(), tmp, 1, 5)) {
                setting.setValue(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.BOOTSTRAP, new Setting<>(SettingIdentifiers.BOOTSTRAP, false, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_BOOTROM, new Setting<>(SettingIdentifiers.DMG_BOOTROM, "DMG.bin", (Setting<String> setting) -> {
            ImString tmp = new ImString(setting.getValue());
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
                            setting.setValue(dmg.getAbsolutePath());
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
        settings.put(SettingIdentifiers.CHEAT_DATABASE, new Setting<>(SettingIdentifiers.CHEAT_DATABASE, "gameshark.cht", (Setting<String> setting) -> {
            ImString tmp = new ImString(setting.getValue());
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
                            setting.setValue(cht.getAbsolutePath());
                            gameboy.propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.CGB_BOOTROM, new Setting<>(SettingIdentifiers.CGB_BOOTROM, "CGB.bin", (Setting<String> setting) -> {
            ImString tmp = new ImString(setting.getValue());
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
                            setting.setValue(cgb.getAbsolutePath());
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
        settings.put(SettingIdentifiers.DMG_PALETTE_0, new Setting<>( SettingIdentifiers.DMG_PALETTE_0, new Color(0xE0, 0xF8, 0xD0, 0xFF), (Setting<Color> setting) -> {
            float[] tmp = {setting.getValue().getRed()/255f, setting.getValue().getGreen()/255f, setting.getValue().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.setValue(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_1, new Setting<>(SettingIdentifiers.DMG_PALETTE_1, new Color(0x88, 0xC0, 0x70, 0xFF), (Setting<Color> setting) -> {
            float[] tmp = {setting.getValue().getRed()/255f, setting.getValue().getGreen()/255f, setting.getValue().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.setValue(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_2, new Setting<>( SettingIdentifiers.DMG_PALETTE_2, new Color(0x34, 0x58, 0x66, 0xFF), (Setting<Color> setting) -> {
            float[] tmp = {setting.getValue().getRed()/255f, setting.getValue().getGreen()/255f, setting.getValue().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.setValue(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_3, new Setting<>(SettingIdentifiers.DMG_PALETTE_3, new Color(0x08, 0x18, 0x20, 0xFF), (Setting<Color> setting) -> {
            float[] tmp = {setting.getValue().getRed()/255f, setting.getValue().getGreen()/255f, setting.getValue().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.setValue(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.GAMMA, new Setting<>(SettingIdentifiers.GAMMA, 2f, (Setting<Float> setting) -> {
            float[] tmp = {setting.getValue()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 1f, 3f)) {
                setting.setValue(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        //Sound
        settings.put(SettingIdentifiers.SQUARE_1_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_1_ENABLED, true, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SQUARE_2_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_2_ENABLED, true, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.WAVE_ENABLED, new Setting<>(SettingIdentifiers.WAVE_ENABLED, true, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.NOISE_ENABLED, new Setting<>(SettingIdentifiers.NOISE_ENABLED, true, (Setting<Boolean> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.setValue(tmp.get());
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.VOLUME, new Setting<>(SettingIdentifiers.VOLUME, 1f, (Setting<Float> setting) -> {
            float[] tmp = {setting.getValue()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 0f, 1f)) {
                setting.setValue(tmp[0]);
                gameboy.propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.KEYBOARD_CONTROL_MAP, new Setting<>(SettingIdentifiers.KEYBOARD_CONTROL_MAP, Button.getKeyboardMap(), (Setting<Map<Button, Integer>> setting) -> {
            for (Button button : Button.values()) {
                if (ImGui.button(button.name(), 80, 20)) {
                    while(true) {
                        if (ImGui.isKeyDown(GLFW.GLFW_KEY_ESCAPE))
                            break;
                        int keycode = Utils.getPressedKey();
                        if (keycode != -1 && !setting.getValue().containsValue(keycode)) {
                            setting.getValue().put(button, keycode);
                            break;
                        }

                    }
                }
                ImGui.sameLine(120);
                ImGui.text(Utils.getKeyName(setting.getValue().get(button)));

            }
        }));
    }

    public void loadFile() {
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(file));
            for (Setting<?> setting : settings.values())
                setting.setSerializedValue(prop.getProperty(setting.getIdentifier().toString()));
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when saving settings : " + e.getMessage());
        }
        applySettings();
    }

    public void saveFile() {
        try {
            Properties prop = new Properties();
            for (Setting<?> setting : settings.values())
                prop.put(setting.getIdentifier().toString(), setting.serializedValue());
            prop.store(new FileWriter(file), null);
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when saving settings : " + e.getMessage());
        }
    }

    public Setting<?> getSetting(SettingIdentifiers name) {
        return settings.get(name);
    }

    private void applySettings() {
        for (Setting<?> setting : settings.values())
            gameboy.propagateSetting(setting);
    }

    public void applyPalette(int[] colors) {
        gameboy.propagateSetting(((Setting<Color>)(settings.get(SettingIdentifiers.DMG_PALETTE_0))).setValue(new Color(colors[0])));
        gameboy.propagateSetting(((Setting<Color>)(settings.get(SettingIdentifiers.DMG_PALETTE_1))).setValue(new Color(colors[1])));
        gameboy.propagateSetting(((Setting<Color>)(settings.get(SettingIdentifiers.DMG_PALETTE_2))).setValue(new Color(colors[2])));
        gameboy.propagateSetting(((Setting<Color>)(settings.get(SettingIdentifiers.DMG_PALETTE_3))).setValue(new Color(colors[3])));
    }

    public void setWindow(long window) {
        this.window = window;
    }
}
