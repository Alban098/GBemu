package gui;

import core.GameBoy;
import core.MMU;
import core.ppu.PPU;
import imgui.ImGui;
import imgui.ImGuiIO;
import openGL.Texture;

public class MemoryLayer {

    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Memory");
        for (int i = 0x000; i <= 0xFFF; i++)
            ImGui.text(String.format("%04X | ", i << 4) + gameBoy.getMemory().toString(i));
        ImGui.sameLine();

        ImGui.end();
    }
}