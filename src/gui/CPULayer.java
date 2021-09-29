package gui;

import core.Flags;
import core.GameBoy;
import core.GameBoyState;
import core.cpu.Instruction;
import core.memory.MMU;
import core.cpu.State;
import core.cpu.register.RegisterByte;
import core.cpu.register.RegisterWord;
import imgui.ImGui;

import java.util.Queue;

public class CPULayer extends AbstractDebugLayer {

    private final RegisterByte lcdc = new RegisterByte(0x00);
    private final RegisterByte stat = new RegisterByte(0x00);
    private final RegisterByte lcdy = new RegisterByte(0x00);
    private final RegisterByte divider = new RegisterByte(0x00);
    private final RegisterByte tima = new RegisterByte(0x00);
    private final RegisterByte irq_enable = new RegisterByte(0x00);
    private final RegisterByte irq_flags = new RegisterByte(0x00);

    public CPULayer(GameBoy gameboy) {
        super(gameboy);
    }

    public void render() {
        ImGui.begin("Debug");
        ImGui.setWindowSize(580, 450);

        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Controls")) {
            ImGui.text("Current State :");
            ImGui.sameLine();
            switch (gameboy.getState()) {
                case RUNNING -> {
                    ImGui.textColored(0, 255, 255, 255, "Running");
                    if (ImGui.button("Pause"))
                        gameboy.setState(GameBoyState.PAUSED);
                }
                case PAUSED -> {
                    ImGui.textColored(255, 0, 0, 255, "Paused");
                    if (ImGui.button("Run"))
                        gameboy.setState(GameBoyState.RUNNING);
                }
                case DEBUG -> ImGui.textColored(255, 255, 0, 255, "Debug Mode");
            }
            if (gameboy.getState() == GameBoyState.DEBUG) {
                if (ImGui.button("Exit Debug"))
                    gameboy.setState(GameBoyState.PAUSED);
            } else {
                if (ImGui.button("Enter Debug"))
                    gameboy.setState(GameBoyState.DEBUG);
            }
            ImGui.sameLine();
            if (ImGui.button("Reset"))
                gameboy.reset();
            ImGui.treePop();
        }

        State cpuState = gameboy.getDebugger().getCpuState();
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Flags")) {
            if (gameboy.getCpu().hasFlag(Flags.Z)) ImGui.textColored(0, 255, 0, 255, "Z");
            else ImGui.textColored(255, 0, 0, 255, "Z");
            ImGui.sameLine();
            if (gameboy.getCpu().hasFlag(Flags.N)) ImGui.textColored(0, 255, 0, 255, "N");
            else ImGui.textColored(255, 0, 0, 255, "N");
            ImGui.sameLine();
            if (gameboy.getCpu().hasFlag(Flags.H)) ImGui.textColored(0, 255, 0, 255, "H");
            else ImGui.textColored(255, 0, 0, 255, "H");
            ImGui.sameLine();
            if (gameboy.getCpu().hasFlag(Flags.C)) ImGui.textColored(0, 255, 0, 255, "C");
            else ImGui.textColored(255, 0, 0, 255, "C");
            ImGui.treePop();
        }
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Registers")) {
            lcdc.write(gameboy.getMemory().readByte(MMU.LCDC));
            stat.write(gameboy.getMemory().readByte(MMU.STAT));
            lcdy.write(gameboy.getMemory().readByte(MMU.LY));
            divider.write(gameboy.getMemory().readByte(MMU.DIV));
            tima.write(gameboy.getMemory().readByte(MMU.TIMA));
            irq_enable.write(gameboy.getMemory().readByte(MMU.IE));
            irq_flags.write(gameboy.getMemory().readByte(MMU.IF));

            printRegister(cpuState.getAf(), "AF", "A", "F");
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "LCDC:");
            ImGui.sameLine();
            ImGui.text(lcdc.toString());
            printRegister(cpuState.getBc(), "BC", "B", "C");
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "LCDS:");
            ImGui.sameLine();
            ImGui.text(stat.toString());
            printRegister(cpuState.getDe(), "DE", "D", "E");
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "LCDY:");
            ImGui.sameLine();
            ImGui.text(lcdy.toString());
            printRegister(cpuState.getHl(), "HL", "H", "L");
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "IRQF:");
            ImGui.sameLine();
            ImGui.text(irq_flags.toString());
            ImGui.textColored(255, 255, 0, 255, "SP:");
            ImGui.sameLine();
            ImGui.text(cpuState.getSp().toString());
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "IRQE:");
            ImGui.sameLine();
            ImGui.text(irq_enable.toString());
            ImGui.textColored(255, 255, 0, 255, "PC:");
            ImGui.sameLine();
            ImGui.text(cpuState.getPc().toString());
            ImGui.sameLine(140);
            ImGui.textColored(255, 255, 0, 255, "IME:");
            ImGui.sameLine();
            ImGui.text(String.valueOf(gameboy.getCpu().getIME()));
            ImGui.sameLine(280);
            ImGui.textColored(255, 255, 0, 255, "DIV:");
            ImGui.sameLine();
            ImGui.text(divider.toString());
            ImGui.sameLine(420);
            ImGui.textColored(255, 255, 0, 255, "TIMA:");
            ImGui.sameLine();
            ImGui.text(tima.toString());
            ImGui.treePop();
        }
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Code Execution")) {
            Queue<Instruction> instructions = gameboy.getDebugger().getInstructionQueue();
            ImGui.textColored(255, 255, 0, 255, cpuState.getInstruction().toString());
            for (Instruction instruction : instructions)
                ImGui.text(instruction.toString());
            ImGui.treePop();
        }

        ImGui.end();
    }

    private void printRegister(RegisterWord reg, String fullName, String highName, String lowName) {
        ImGui.textColored(255, 255, 0, 255, fullName + ":");
        ImGui.sameLine();
        ImGui.text(reg.toString());
        ImGui.sameLine(140);
        ImGui.textColored(255, 255, 0, 255, highName + ":");
        ImGui.sameLine();
        ImGui.text(reg.getHigh().toString());
        ImGui.sameLine(280);
        ImGui.textColored(255, 255, 0, 255, lowName + ":");
        ImGui.sameLine();
        ImGui.text(reg.getLow().toString());
    }
}