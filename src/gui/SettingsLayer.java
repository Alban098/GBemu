package gui;

import core.memory.MMU;
import core.ppu.PPU;
import core.ppu.helper.ColorShade;
import core.ppu.helper.Sprite;
import core.settings.SettingsContainer;
import debug.Debugger;
import debug.DebuggerMode;
import imgui.ImGui;
import openGL.Texture;

public class SettingsLayer extends AbstractDebugLayer {

    private final float[] gamma = new float[1];

    public SettingsLayer(Debugger debugger) {
        super(debugger);
    }

    public void render() {
        ImGui.begin("Settings");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Graphics")) {
                gamma[0] = (float) SettingsContainer.getInstance().getSetting("gamma").getValue();
                ImGui.sliderFloat("gamma", gamma, 1, 3);
                SettingsContainer.getInstance().setSetting("gamma", gamma[0]);
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
        ImGui.end();
    }
}