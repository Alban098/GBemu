package core.settings;

import audio.AudioEngine;
import audio.AudioOutput;
import console.Console;
import console.Type;
import core.GameBoy;
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

    private static SettingsContainer instance;

    private String file;

    private final Map<SettingIdentifiers, Setting<?>> settings;
    private GameBoy gameboy;

    public SettingsContainer() {
        this.settings = new HashMap<>();
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
        settings.put(SettingIdentifiers.AUDIO_MIXER, new Setting<>(SettingIdentifiers.AUDIO_MIXER, 0, (Setting<Integer> setting) -> {
            ImInt tmp = new ImInt(setting.getValue());
            String[] mixers = new String[AudioEngine.getInstance().getValidOutputs().size()];
            int i = 0;
            for (AudioOutput output : AudioEngine.getInstance().getValidOutputs()) {
                mixers[i] = output.mixer().getName();
                i++;
            }
            if (ImGui.combo(setting.getIdentifier().getDescription(), tmp, mixers)) {
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

    public static SettingsContainer getInstance() {
        if (instance == null)
            instance = new SettingsContainer();
        return instance;
    }

    public static void loadFile(String file) {
        getInstance().file = file;
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(file));
            for (Setting<?> setting : getInstance().settings.values())
                setting.setSerializedValue(prop.getProperty(setting.getIdentifier().toString()));
            getInstance().applySettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void hook(GameBoy gameboy) {
        getInstance().gameboy = gameboy;
    }

    public static void saveFile() {
        try {
            Properties prop = new Properties();
            for (Setting<?> setting : getInstance().settings.values())
                prop.put(setting.getIdentifier().toString(), setting.serializedValue());
            prop.store(new FileWriter(getInstance().file), null);
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
