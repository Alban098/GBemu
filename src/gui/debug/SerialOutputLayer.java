package gui.debug;

import gbemu.extension.debug.Debugger;
import imgui.ImGui;
import utils.Utils;


public class SerialOutputLayer extends DebugLayer {


    public SerialOutputLayer(Debugger debugger) {
        super(debugger);
    }

    public void render() {
        ImGui.begin("Serial Output");
        ImGui.setWindowSize(515, 192);
        ImGui.beginChild("Scrolling", 500, 130);
        synchronized (debugger) {
            ImGui.textColored(0, 255, 0, 255, Utils.getPrettifiedOutput(debugger.getSerialOutput(), 69));
            ImGui.endChild();
            if (ImGui.button("Clear", 500, 18))
                debugger.flushSerialOutput();
        }
        ImGui.end();
    }
}