package gbemu.settings;

import audio.AudioEngine;
import audio.AudioOutput;
import console.Console;
import console.Type;
import gbemu.core.GameBoy;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
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
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "GameBoy ROM (.gb, .gbc, .bin)", "gb", ".gb", "gbc", ".gbc", "bin", ".bin");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        setting.setValue(chooser.getSelectedFile().getAbsolutePath());
                        if (chooser.getSelectedFile().length() != 0x100)
                            throw new Exception("Invalid DMG Size (must be 256 bytes");
                        gameboy.propagateSetting(setting);
                    } catch (Exception e) {
                        Console.getInstance().log(Type.ERROR, "Invalid file : " + e.getMessage());
                    }
                }
            }
        }));
        settings.put(SettingIdentifiers.CHEAT_DATABASE, new Setting<>(SettingIdentifiers.CHEAT_DATABASE, "gameshark.cht", (Setting<String> setting) -> {
            ImString tmp = new ImString(setting.getValue());
            ImGui.inputText("", tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load")) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "GameShark Database", "cht", ".cht");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        setting.setValue(chooser.getSelectedFile().getAbsolutePath());
                        gameboy.propagateSetting(setting);
                    } catch (Exception e) {
                        Console.getInstance().log(Type.ERROR, "Invalid file : " + e.getMessage());
                    }
                }
            }
        }));
        settings.put(SettingIdentifiers.CGB_BOOTROM, new Setting<>(SettingIdentifiers.CGB_BOOTROM, "CGB.bin", (Setting<String> setting) -> {
            ImString tmp = new ImString(setting.getValue());
            ImGui.inputText("", tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load CGB")) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "GameBoy ROM (.gb, .gbc, .bin)", "gb", ".gb", "gbc", ".gbc", "bin", ".bin");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        setting.setValue(chooser.getSelectedFile().getAbsolutePath());
                        if (chooser.getSelectedFile().length() != 0x900)
                            throw new Exception("Invalid CGB Size (must be 2304 bytes");
                        gameboy.propagateSetting(setting);
                    } catch (Exception e) {
                        Console.getInstance().log(Type.ERROR, "Invalid file : " + e.getMessage());
                    }
                }
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
    }

    public void loadFile() {
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(file));
            for (Setting<?> setting : settings.values())
                setting.setSerializedValue(prop.getProperty(setting.getIdentifier().toString()));
        } catch (IOException e) {
            e.printStackTrace();
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
}
