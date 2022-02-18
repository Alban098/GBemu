package gui.std;

import gbemu.settings.Palette;
import gbemu.settings.SettingIdentifiers;
import gbemu.settings.SettingsContainer;
import imgui.ImGui;
import imgui.level.ImInt;

/**
 * This class represent the Setting window
 * allowing user to change the emulator's behaviour
 */
public class SettingsLayer extends Layer {

    private final String[] paletteCombo = new String[Palette.palettes.length];
    private final ImInt palette = new ImInt(0);
    private final SettingsContainer settingsContainer;

    /**
     * Create a new instance of the Layer
     * @param settingsContainer the container linked to the layer
     */
    public SettingsLayer(SettingsContainer settingsContainer) {
        super();
        this.settingsContainer = settingsContainer;
        for (int i = 0; i < Palette.palettes.length; i++)
            paletteCombo[i] = Palette.palettes[i].getName();
    }

    /**
     * Render the layer to the screen
     * and propagate user inputs to the emulator
     */
    public void render() {
        ImGui.begin("Settings");
        ImGui.setWindowSize(350, 310);
        if (ImGui.button("Save Settings"))
            settingsContainer.saveFile();
        ImGui.sameLine(305);
        if (ImGui.button("Exit"))
            setVisible(false);
        ImGui.separator();
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("System")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Misc Options")) {
                    settingsContainer.getSetting(SettingIdentifiers.SPEED).process();
                    settingsContainer.getSetting(SettingIdentifiers.RTC).process();
                    settingsContainer.getSetting(SettingIdentifiers.BOOTSTRAP).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Cheats")) {
                    settingsContainer.getSetting(SettingIdentifiers.CHEAT_DATABASE).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Bootstrap Files")) {
                    settingsContainer.getSetting(SettingIdentifiers.DMG_BOOTROM).process();
                    settingsContainer.getSetting(SettingIdentifiers.CGB_BOOTROM).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Graphics")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Color Settings")) {
                    settingsContainer.getSetting(SettingIdentifiers.GAMMA).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("DMG Palette")) {
                    ImGui.combo(" ", palette, paletteCombo);
                    ImGui.sameLine();
                    if (ImGui.button("Apply"))
                        settingsContainer.applyPalette(Palette.palettes[palette.get()].getColors());
                    settingsContainer.getSetting(SettingIdentifiers.DMG_PALETTE_0).process();
                    ImGui.sameLine();
                    settingsContainer.getSetting(SettingIdentifiers.DMG_PALETTE_1).process();
                    ImGui.sameLine();
                    settingsContainer.getSetting(SettingIdentifiers.DMG_PALETTE_2).process();
                    ImGui.sameLine();
                    settingsContainer.getSetting(SettingIdentifiers.DMG_PALETTE_3).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Sounds")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Audio Output Settings")) {
                    settingsContainer.getSetting(SettingIdentifiers.VOLUME).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Sound Channel Rendering")) {
                    settingsContainer.getSetting(SettingIdentifiers.SQUARE_1_ENABLED).process();
                    settingsContainer.getSetting(SettingIdentifiers.SQUARE_2_ENABLED).process();
                    settingsContainer.getSetting(SettingIdentifiers.WAVE_ENABLED).process();
                    settingsContainer.getSetting(SettingIdentifiers.NOISE_ENABLED).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Controls")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Keyboard")) {
                    settingsContainer.getSetting(SettingIdentifiers.KEYBOARD_CONTROL_MAP).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();

        ImGui.end();
    }
}