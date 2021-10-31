package gui;

import core.settings.SettingIdentifiers;
import core.settings.SettingsContainer;
import debug.Debugger;
import imgui.ImGui;
import imgui.flag.ImGuiComboFlags;
import imgui.type.ImInt;

public class SettingsLayer extends AbstractDebugLayer {

    private static final Palette[] palettes = {
            new Palette("Default", 0xE0F8D0, 0x88c070, 0x346856, 0x081820),
            new Palette("RedRose", 0xCC3D50, 0x991F27, 0x591616, 0x260F0D),
            new Palette("Aqua", 0x668FCC, 0x244AB3, 0x141F66, 0x141433),
            new Palette("Internal Yellow", 0xD0E040, 0xA0A830, 0x607028, 0x384828),
            new Palette("Deadbeat", 0xE2E8BD, 0xAF986F, 0x9C8277, 0x6B6066),
            new Palette("Gumball", 0xFFFFFF, 0x2CE8F5, 0xFF0044, 0x193C3E),
            new Palette("Kirokaze", 0xE2f3E4, 0x94E344, 0x46878F, 0x332C50),
            new Palette("AYY4", 0xF1F2DA, 0xFFCE96, 0xFF7777, 0x00303B),
            new Palette("Mist", 0xC4C0C2, 0x5AB9A8, 0x1E606E, 0x2D1B00),
            new Palette("Wish", 0x8BE5FF, 0x5608FCF, 0x7550E8, 0x622E4C),
            new Palette("Demichrome", 0xE9EFEC, 0xA0A08B, 0x555568, 0x211E20),
            new Palette("Gold", 0xCFAB51, 0x9D654C, 0x4D222C, 0x210B1B),
            new Palette("Grayscale", 0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000),
    };

    private static final String[] paletteCombo = new String[palettes.length];

    private final ImInt palette = new ImInt(0);

    public SettingsLayer(Debugger debugger) {
        super(debugger);

        for (int i = 0; i < palettes.length; i++)
            paletteCombo[i] = palettes[i].name;
    }

    public void render() {
        ImGui.begin("Settings");
        ImGui.setWindowSize(350, 280);
        if (ImGui.button("Save Settings"))
            SettingsContainer.saveFile();
        ImGui.sameLine(305);
        if (ImGui.button("Exit"))
            setVisible(false);
        ImGui.separator();
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("System")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Misc Options")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.SPEED).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.RTC).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.BOOTSTRAP).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Cheats")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.CHEAT_DATABASE).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Bootstrap Files")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.DMG_BOOTROM).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.CGB_BOOTROM).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Graphics")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Color Settings")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.GAMMA).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("DMG Palette")) {
                    ImGui.combo(" ", palette, paletteCombo);
                    ImGui.sameLine();
                    if (ImGui.button("Apply"))
                        SettingsContainer.getInstance().applyPalette(palettes[palette.get()].colors);
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.DMG_PALETTE_0).process();
                    ImGui.sameLine();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.DMG_PALETTE_1).process();
                    ImGui.sameLine();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.DMG_PALETTE_2).process();
                    ImGui.sameLine();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.DMG_PALETTE_3).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Sounds")) {
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Audio Output Settings")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.AUDIO_MIXER).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.VOLUME).process();
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Sound Channel Rendering")) {
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.SQUARE_1_ENABLED).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.SQUARE_2_ENABLED).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.WAVE_ENABLED).process();
                    SettingsContainer.getInstance().getSetting(SettingIdentifiers.NOISE_ENABLED).process();
                    ImGui.treePop();
                }
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();

        ImGui.end();
    }

    private static class Palette {

        private String name;
        private int[] colors;

        public Palette(String name, int color0, int color1, int color2, int color3) {
            this.name = name;
            colors = new int[]{color0, color1, color2, color3};
        }
    }
}