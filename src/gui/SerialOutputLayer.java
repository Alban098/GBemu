package gui;

import core.GameBoy;
import core.cpu.Flags;
import core.cpu.LR35902;
import core.cpu.State;
import core.cpu.register.RegisterWord;
import imgui.ImGui;

import java.util.Queue;

public class SerialOutputLayer {


    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Serial Output");
        ImGui.setWindowSize(515, 192);
        ImGui.beginChild("Scrolling", 500, 130);
        ImGui.textColored(0, 255, 0, 255, gameBoy.getSerialOutput());
        ImGui.endChild();
        if (ImGui.button("Clear", 500, 18))
            gameBoy.flushSerialOutput();
        ImGui.end();
    }

}