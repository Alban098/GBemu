package gui.debug;

import gbemu.extension.debug.Debugger;
import gui.std.Layer;
import imgui.ImGui;
import utils.Utils;

/**
 * This class represent an abstract ImGui Layer coming with debug capabilities
 */
public abstract class DebugLayer extends Layer {

    protected final Debugger debugger;

    /**
     * Create a new instance of DebugLayer
     * @param debugger the debugger to link to
     */
    public DebugLayer(Debugger debugger) {
        super();
        this.debugger = debugger;
    }

    /**
     * Print a register to the screen as hex and binary
     * @param addr the address of the register in memory
     * @param name the name of the register to print
     * @param value the value of the register
     */
    protected void inlineRegister(int addr, String name, int value) {
        ImGui.textColored(0, 255, 255, 255, String.format("    $%04X", addr));
        ImGui.sameLine();
        ImGui.textColored(255, 0, 255, 255, name);
        ImGui.sameLine();
        ImGui.text(String.format("$%02X", value) + "(" + Utils.binaryString(value, 8) + ")");
    }

}
