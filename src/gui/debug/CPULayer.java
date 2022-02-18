package gui.debug;

import gbemu.core.Flags;
import gbemu.core.GameBoyState;
import gbemu.core.cpu.Instruction;
import gbemu.core.cpu.State;
import gbemu.extension.debug.BreakPoint;
import gbemu.extension.debug.Debugger;
import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import threading.GameBoyThread;

import java.util.Map;
import java.util.Queue;

/**
 * This class represent the DebugLayer in charge of displaying the current state of the CPU
 * registers, currently executing code and is in charge of handling breakpoints management
 */
public class CPULayer extends DebugLayer {

    private final GameBoyThread gameboyThread;
    private final ImInt breakType = new ImInt();
    private final ImString breakAddr = new ImString();

    /**
     * Create a new instance of CPULayer
     * @param debugger the debugger to link to
     */
    public CPULayer(Debugger debugger, GameBoyThread gameBoyThread) {
        super(debugger);
        this.gameboyThread = gameBoyThread;
    }

    /**
     * Render the layer to the screen
     * and propagate button presses if needed
     */
    public void render() {
        ImGui.begin("CPU Debugger");
        ImGui.setWindowSize(380, debugger.getGameboyState() == GameBoyState.DEBUG ? 760 : 490);
        ImGui.text("Current State :");
        ImGui.sameLine();
        switch (debugger.getGameboyState()) {
            case RUNNING -> {
                ImGui.textColored(0, 255, 255, 255, "Running");
                if (ImGui.button("Pause"))
                    synchronized (debugger) {
                        debugger.setGameboyState(GameBoyState.PAUSED);
                    }
            }
            case PAUSED -> {
                ImGui.textColored(255, 0, 0, 255, "Paused");
                if (ImGui.button("Run"))
                    synchronized (debugger) {
                        debugger.setGameboyState(GameBoyState.RUNNING);
                    }
            }
            case DEBUG -> {
                ImGui.textColored(255, 255, 0, 255, "Debug Mode");
                ImGui.newLine();
            }
        }
        ImGui.sameLine(80);
        if (debugger.getGameboyState() == GameBoyState.DEBUG) {
            if (ImGui.button("Debug"))
                synchronized (debugger) {
                    debugger.setGameboyState(GameBoyState.PAUSED);
                }
        } else {
            if (ImGui.button("Debug"))
                synchronized (debugger) {
                    debugger.setGameboyState(GameBoyState.DEBUG);
                }
        }
        ImGui.sameLine();
        if (ImGui.button("Reset")) {
            synchronized (debugger) {
                debugger.reset();
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Step Over") && debugger.getGameboyState() == GameBoyState.DEBUG)
            gameboyThread.requestInstructions(1);
        ImGui.sameLine();
        if (ImGui.button("Step Frame") && debugger.getGameboyState() == GameBoyState.DEBUG)
            gameboyThread.requestOneFrame();

        synchronized (debugger) {
            synchronized (debugger.getCpuState()) {
                State cpuState = debugger.getCpuState();
                ImGui.separator();
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Flags")) {
                    ImGui.newLine();
                    ImGui.sameLine(145);
                    ImGui.textColored(255, 0, 255, 255, "Z");
                    ImGui.sameLine();
                    ImGui.text("=");
                    ImGui.sameLine();
                    if (cpuState.hasFlag(Flags.Z)) ImGui.textColored(0, 255, 0, 255, "1");
                    else ImGui.textColored(255, 0, 0, 255, "0");
                    ImGui.sameLine(200);
                    ImGui.textColored(255, 0, 255, 255, "N");
                    ImGui.sameLine();
                    ImGui.text("=");
                    ImGui.sameLine();
                    if (cpuState.hasFlag(Flags.N)) ImGui.textColored(0, 255, 0, 255, "1");
                    else ImGui.textColored(255, 0, 0, 255, "0");

                    ImGui.newLine();
                    ImGui.sameLine(145);
                    ImGui.textColored(255, 0, 255, 255, "H");
                    ImGui.sameLine();
                    ImGui.text("=");
                    ImGui.sameLine();
                    if (cpuState.hasFlag(Flags.H)) ImGui.textColored(0, 255, 0, 255, "1");
                    else ImGui.textColored(255, 0, 0, 255, "0");
                    ImGui.sameLine(200);
                    ImGui.textColored(255, 0, 255, 255, "C");
                    ImGui.sameLine();
                    ImGui.text("=");
                    ImGui.sameLine();
                    if (cpuState.hasFlag(Flags.C)) ImGui.textColored(0, 255, 0, 255, "1");
                    else ImGui.textColored(255, 0, 0, 255, "0");

                    ImGui.newLine();
                    ImGui.sameLine(145);
                    ImGui.textColored(255, 0, 255, 255, "   IME");
                    ImGui.sameLine();
                    ImGui.text("=");
                    ImGui.sameLine();
                    if (cpuState.getIME()) ImGui.textColored(0, 255, 0, 255, "1");
                    else ImGui.textColored(255, 0, 0, 255, "0");
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Registers")) {

                    ImGui.textColored(0, 255, 255, 255, " A");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getAf().getHigh().toString());
                    ImGui.sameLine();
                    ImGui.textColored(0, 255, 255, 255, "  F");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getAf().getLow().toString());
                    ImGui.sameLine(210);
                    ImGui.textColored(0, 255, 255, 255, " B");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getBc().getHigh().toString());
                    ImGui.sameLine();
                    ImGui.textColored(0, 255, 255, 255, "  C");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getBc().getLow().toString());
                    ImGui.text(cpuState.getAf().binaryString());
                    ImGui.sameLine(210);
                    ImGui.text(cpuState.getBc().binaryString());
                    ImGui.separator();
                    ImGui.textColored(0, 255, 255, 255, " D");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getDe().getHigh().toString());
                    ImGui.sameLine();
                    ImGui.textColored(0, 255, 255, 255, "  E");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getDe().getLow().toString());
                    ImGui.sameLine(210);
                    ImGui.textColored(0, 255, 255, 255, " H");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getHl().getHigh().toString());
                    ImGui.sameLine();
                    ImGui.textColored(0, 255, 255, 255, "  L");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getHl().getLow().toString());
                    ImGui.text(cpuState.getDe().binaryString());
                    ImGui.sameLine(210);
                    ImGui.text(cpuState.getHl().binaryString());
                    ImGui.separator();
                    ImGui.textColored(255, 255, 0, 255, "    SP");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getSp().toString());
                    ImGui.sameLine(210);
                    ImGui.textColored(255, 255, 0, 255, "    PC");
                    ImGui.sameLine();
                    ImGui.text("= " + cpuState.getPc().toString());
                    ImGui.text(cpuState.getSp().binaryString());
                    ImGui.sameLine(210);
                    ImGui.text(cpuState.getPc().binaryString());
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Breakpoints")) {
                    ImGui.beginChild("Breakpoints", 340, 100);
                    for (Map.Entry<Integer, BreakPoint> entry :  debugger.getBreakpoints().entrySet()) {
                        ImGui.textColored(255, 255, 0, 255, String.format("$%04X", entry.getValue().address()));
                        ImGui.sameLine();
                        ImGui.text( ": " + entry.getValue().type().toString());
                        ImGui.sameLine(260);
                        if (ImGui.button("Delete"))
                            debugger.removeBreakpoint(entry.getValue().address());
                    }
                    ImGui.endChild();
                    ImGui.separator();
                    ImGui.pushItemWidth(70);
                    if (ImGui.inputText(": Address ", breakAddr))
                        breakAddr.set(breakAddr.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
                    ImGui.sameLine();
                    ImGui.pushItemWidth(70);
                    ImGui.combo(": Type", breakType, BreakPoint.Type.stringArray());
                    ImGui.sameLine();
                    if (ImGui.button("Add")) {
                        breakAddr.set(breakAddr.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
                        if (breakAddr.get().equals(""))
                            breakAddr.set("0");
                        int addr = Integer.decode("0x" + breakAddr.get());
                        debugger.addBreakpoint(addr, BreakPoint.Type.values()[breakType.get()]);
                    }
                    ImGui.treePop();
                }
                ImGui.separator();
                ImGui.separator();
                ImGui.setNextItemOpen(true);
                if (ImGui.treeNode("Code Execution")) {
                    if (debugger.getGameboyState() == GameBoyState.DEBUG) {
                        Queue<Instruction> instructions = debugger.getInstructionQueue();
                        for (Instruction instruction : instructions)
                            printInstruction(instruction, instruction.getAddr() == cpuState.getPc().read());
                    } else {
                        ImGui.textColored(255, 0, 0, 255, "To show code, enter Debug Mode !");
                    }
                    ImGui.treePop();
                }
            }
        }
        ImGui.end();
    }

    /**
     * Print an Instruction to the screen
     * @param instruction the instruction to print
     * @param overrideColor use alternate color or not
     */
    private void printInstruction(Instruction instruction, boolean overrideColor) {
        if (instruction.getAddr() == 0x00) ImGui.textColored(0, 255, 0, 255, "Reset 0x00 :");
        if (instruction.getAddr() == 0x08) ImGui.textColored(0, 255, 0, 255, "Reset 0x08 :");
        if (instruction.getAddr() == 0x10) ImGui.textColored(0, 255, 0, 255, "Reset 0x10 :");
        if (instruction.getAddr() == 0x18) ImGui.textColored(0, 255, 0, 255, "Reset 0x18 :");
        if (instruction.getAddr() == 0x20) ImGui.textColored(0, 255, 0, 255, "Reset 0x20 :");
        if (instruction.getAddr() == 0x28) ImGui.textColored(0, 255, 0, 255, "Reset 0x28 :");
        if (instruction.getAddr() == 0x30) ImGui.textColored(0, 255, 0, 255, "Reset 0x30 :");
        if (instruction.getAddr() == 0x38) ImGui.textColored(0, 255, 0, 255, "Reset 0x38 :");
        if (instruction.getAddr() == 0x40) ImGui.textColored(0, 255, 0, 255, "VBlank Interrupt :");
        if (instruction.getAddr() == 0x48) ImGui.textColored(0, 255, 0, 255, "LCDC Interrupt :");
        if (instruction.getAddr() == 0x50) ImGui.textColored(0, 255, 0, 255, "Timer Overflow Interrupt :");
        if (instruction.getAddr() == 0x58) ImGui.textColored(0, 255, 0, 255, "Serial Interrupt :");
        if (instruction.getAddr() == 0x60) ImGui.textColored(0, 255, 0, 255, "Joypad Interrupt :");
        if (instruction.getAddr() == 0x4000) ImGui.textColored(0, 255, 0, 255, debugger.getSector(0x4000) + " :");
        if (instruction.getAddr() == 0x8000) ImGui.textColored(0, 255, 0, 255, debugger.getSector(0x8000) + " :");
        if (instruction.getAddr() == 0xA000) ImGui.textColored(0, 255, 0, 255, debugger.getSector(0xA000) + " :");
        if (instruction.getAddr() == 0xC000) ImGui.textColored(0, 255, 0, 255, debugger.getSector(0xC000) + " :");
        if (instruction.getAddr() == 0xD000) ImGui.textColored(0, 255, 0, 255, debugger.getSector(0xD000) + " :");
        if (!overrideColor) {
            ImGui.textColored(0, 255, 255, 255, "  " + instruction.getAddrStr() + ":");
            ImGui.sameLine();
            ImGui.textColored(128, 128, 128, 255, instruction.getMemoryStr());
            ImGui.sameLine(150);
            ImGui.text(instruction.getDisassembled());
            ImGui.sameLine(275);
            ImGui.textColored(255, 0, 0, 255, instruction.getComment());
        } else {
            ImGui.textColored(255, 255, 0, 255, "  " + instruction.getAddrStr() + ":");
            ImGui.sameLine();
            ImGui.textColored(128, 128, 0, 255, instruction.getMemoryStr());
            ImGui.sameLine(150);
            ImGui.textColored(255, 255, 0, 255, instruction.getDisassembled());
            ImGui.sameLine(275);
            ImGui.textColored(255, 0, 0, 255, instruction.getComment());
        }
    }
}