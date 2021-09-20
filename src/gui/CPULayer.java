package gui;

import core.GameBoy;
import core.cpu.Flags;
import core.cpu.LR35902;
import core.cpu.State;
import core.cpu.register.RegisterWord;
import imgui.ImGui;

import java.util.List;
import java.util.Queue;

public class CPULayer {

    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Debug");
        ImGui.setWindowSize(550, 480);

        State cpuState = gameBoy.getCpu().getCpuState();
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Flags")) {
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
            ImGui.treePop();
        }
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Registers")) {
            printRegister(cpuState.getAf(), "AF", "A", "F");
            printRegister(cpuState.getBc(), "BC", "B", "C");
            printRegister(cpuState.getDe(), "DE", "D", "E");
            printRegister(cpuState.getHl(), "HL", "H", "L");
            ImGui.textColored(255, 255, 0, 255, "SP:");
            ImGui.sameLine();
            ImGui.text(cpuState.getSp().toString());
            ImGui.textColored(255, 255, 0, 255, "PC:");
            ImGui.sameLine();
            ImGui.text(cpuState.getPc().toString());
            ImGui.treePop();
        }
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Code Execution")) {
            ImGui.beginChild("Scrolling", 512, 256);
            Queue<LR35902.Instruction> instructions = gameBoy.getCpu().getInstructionQueue();
            ImGui.textColored(255, 255, 0, 255, cpuState.getInstruction().toString());
            for (LR35902.Instruction instruction : instructions)
                ImGui.text(instruction.toString());
            ImGui.endChild();
            ImGui.treePop();
        }

        ImGui.end();
    }

    private void printRegister(RegisterWord reg, String fullName, String highName, String lowName) {
        ImGui.textColored(255, 255, 0, 255, fullName + ":");
        ImGui.sameLine();
        ImGui.text(reg.toString());
        ImGui.sameLine(130);
        ImGui.textColored(255, 255, 0, 255, highName + ":");
        ImGui.sameLine();
        ImGui.text(reg.getHigh().toString());
        ImGui.sameLine(260);
        ImGui.textColored(255, 255, 0, 255, lowName + ":");
        ImGui.sameLine();
        ImGui.text(reg.getLow().toString());
    }
}