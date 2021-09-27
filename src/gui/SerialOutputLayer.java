package gui;

import core.GameBoy;
import imgui.ImGui;



public class SerialOutputLayer extends AbstractDebugLayer {

    public SerialOutputLayer(GameBoy gameboy) {
        super(gameboy);
    }

    public void render() {
        ImGui.begin("Serial Output");
        ImGui.setWindowSize(515, 192);
        ImGui.beginChild("Scrolling", 500, 130);

        ImGui.textColored(0, 255, 0, 255, Utils.getPrettifiedOutput(gameboy.getSerialOutput(), 69));
        ImGui.endChild();
        if (ImGui.button("Clear", 500, 18))
            gameboy.flushSerialOutput();
        ImGui.end();
    }
}