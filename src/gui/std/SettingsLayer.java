package gui.std;

import gbemu.settings.Palette;
import gbemu.settings.SettingIdentifiers;
import gbemu.settings.SettingsContainer;
import imgui.ImGui;
import imgui.type.ImInt;

/**
 * This class represent the Setting window
 * allowing user to change the emulator's behaviour
 */
public class SettingsLayer extends Layer {

    private final String[] palette_combo = new String[Palette.palettes.length];
    private final ImInt palette = new ImInt(0);
    private final SettingsContainer settings_container;

    /**
     * Create a new instance of the Layer
     * @param settings_container the container linked to the layer
     */
    public SettingsLayer(SettingsContainer settings_container) {
        super();
        this.settings_container = settings_container;
        for (int i = 0; i < Palette.palettes.length; i++)
            palette_combo[i] = Palette.palettes[i].getName();
    }

    /**
     * Render the layer to the screen
     * and propagate user inputs to the emulator
     */
    public void render() {
        ImGui.begin("Settings");
        ImGui.setWindowSize(350, 310);
        if (ImGui.button("Save Settings"))
            settings_container.saveFile();
        ImGui.sameLine(305);
        if (ImGui.button("Exit"))
            setVisible(false);
        ImGui.separator();
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("System")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Misc Options")) {
                    settings_container.getSetting(SettingIdentifiers.SPEED).process();
                    settings_container.getSetting(SettingIdentifiers.RTC).process();
                    settings_container.getSetting(SettingIdentifiers.BOOTSTRAP).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Cheats")) {
                    settings_container.getSetting(SettingIdentifiers.CHEAT_DATABASE).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Bootstrap Files")) {
                    settings_container.getSetting(SettingIdentifiers.DMG_BOOTROM).process();
                    settings_container.getSetting(SettingIdentifiers.CGB_BOOTROM).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Graphics")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Color Settings")) {
                    settings_container.getSetting(SettingIdentifiers.GAMMA).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("DMG Palette")) {
                    ImGui.combo(" ", palette, palette_combo);
                    ImGui.sameLine();
                    if (ImGui.button("Apply"))
                        settings_container.applyPalette(Palette.palettes[palette.get()].getColors());
                    settings_container.getSetting(SettingIdentifiers.DMG_PALETTE_0).process();
                    ImGui.sameLine();
                    settings_container.getSetting(SettingIdentifiers.DMG_PALETTE_1).process();
                    ImGui.sameLine();
                    settings_container.getSetting(SettingIdentifiers.DMG_PALETTE_2).process();
                    ImGui.sameLine();
                    settings_container.getSetting(SettingIdentifiers.DMG_PALETTE_3).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Sounds")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Audio Output Settings")) {
                    settings_container.getSetting(SettingIdentifiers.VOLUME).process();
                    settings_container.getSetting(SettingIdentifiers.PULSE_MODE).process();
                    settings_container.getSetting(SettingIdentifiers.PULSE_HARMONICS).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Sound Channel Rendering")) {
                    settings_container.getSetting(SettingIdentifiers.SQUARE_1_ENABLED).process();
                    settings_container.getSetting(SettingIdentifiers.SQUARE_2_ENABLED).process();
                    settings_container.getSetting(SettingIdentifiers.WAVE_ENABLED).process();
                    settings_container.getSetting(SettingIdentifiers.NOISE_ENABLED).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Controls")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Keyboard")) {
                    settings_container.getSetting(SettingIdentifiers.KEYBOARD_CONTROL_MAP).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
        ImGui.end();
    }
}