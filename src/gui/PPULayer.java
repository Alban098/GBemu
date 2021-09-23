package gui;

import imgui.ImGui;
import openGL.Texture;

public class PPULayer {

    public void imgui(Texture[] tileTables, Texture[] tileMaps) {
        ImGui.begin("PPU");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Tile Data")) {
                ImGui.image(tileTables[0].getID(), 128 * 2, 64 * 2);
                ImGui.image(tileTables[1].getID(), 128 * 2, 64 * 2);
                ImGui.image(tileTables[2].getID(), 128 * 2, 64 * 2);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Tile Maps")) {
                ImGui.image(tileMaps[0].getID(), 256, 256);
                ImGui.sameLine();
                ImGui.image(tileMaps[1].getID(), 256, 256);
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
        ImGui.end();
    }
}