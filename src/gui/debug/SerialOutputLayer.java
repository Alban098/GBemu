package gui.debug;

import gbemu.extension.debug.Debugger;
import imgui.ImGui;
import utils.Utils;

/**
 * This class represent the DebugLayer in charge of displaying the SerialOutput of the Game Boy
 */
public class SerialOutputLayer extends DebugLayer {

    /**
     * Create a new instance of SerialOutputLayer
     * @param debugger the debugger to link to
     */
    public SerialOutputLayer(Debugger debugger) {
        super(debugger);
    }

    /**
     * Render the layer to the screen
     */
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