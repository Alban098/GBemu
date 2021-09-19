package gui;

import core.GameBoy;
import core.cpu.Flags;
import core.cpu.State;
import imgui.ImGui;

public class CPULayer {

    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Debug");

        State cpuState = gameBoy.getCpu().getCpuState();
        ImGui.text("Flags");
        ImGui.separator();
        if (gameBoy.getCpu().hasFlag(Flags.ZERO)) ImGui.textColored(0, 255, 0, 255, "Z");
        else ImGui.textColored(255, 0, 0, 255, "Z");
        ImGui.sameLine();
        if (gameBoy.getCpu().hasFlag(Flags.SUBSTRACT)) ImGui.textColored(0, 255, 0, 255, "N");
        else ImGui.textColored(255, 0, 0, 255, "N");
        ImGui.sameLine();
        if (gameBoy.getCpu().hasFlag(Flags.HALF_CARRY)) ImGui.textColored(0, 255, 0, 255, "H");
        else ImGui.textColored(255, 0, 0, 255, "H");
        ImGui.sameLine();
        if (gameBoy.getCpu().hasFlag(Flags.CARRY)) ImGui.textColored(0, 255, 0, 255, "C");
        else ImGui.textColored(255, 0, 0, 255, "C");

        ImGui.newLine();
        ImGui.text("Next Instruction :");
        ImGui.text(cpuState.getInstruction().toString());

        ImGui.newLine();
        ImGui.text("Registers");
        ImGui.separator();
        ImGui.text("AF: " + cpuState.getAf().toString());
        ImGui.sameLine(150);
        ImGui.text("BC: " + cpuState.getBc().toString());
        ImGui.text("DE: " + cpuState.getDe().toString());
        ImGui.sameLine(150);
        ImGui.text("HL: " + cpuState.getHl().toString());
        ImGui.text("SP: " + cpuState.getSp().toString());
        ImGui.sameLine(150);
        ImGui.text("PC: " + cpuState.getPc().toString());

        ImGui.end();
    }
}