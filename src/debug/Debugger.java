package debug;

import core.GameBoy;
import core.GameBoyState;
import core.cpu.Instruction;
import core.cpu.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Debugger {

    private static final int DECOMPILE_SIZE = 0x08;

    private State cpuState;
    private final GameBoy gameboy;

    private boolean hooked = false;

    private final Map<Integer, BreakPoint> breakpoints;
    private final Queue<Instruction> instructionQueue;


    public Debugger(GameBoy gb) {
        breakpoints = new HashMap<>();
        instructionQueue = new ConcurrentLinkedQueue<>();
        this.gameboy = gb;
        for (int i = 0; i < DECOMPILE_SIZE; i++)
            instructionQueue.add(new Instruction(0, Instruction.Type.MISC,"NOP", 1, null, gameboy.getCpu()));
    }

    private void decompile() {
        int addr = cpuState.getPc().read();
        addr += cpuState.getInstruction().getLength();
        for (Instruction instr : instructionQueue) {
            if (addr >= 0x8000 && addr <= 0x9FFF || addr >= 0xFE00 && addr <= 0xFF7F || addr == 0xFFFF || addr >= 0x0104 && addr <= 0x014F) {
                instr.setAddr(addr);
                instr.setLength(0x10 - (addr & 0xF));
                for (int i = 0; i < instr.getLength(); i++)
                    instr.setParam(i, gameboy.getMemory().readByte(addr++));
                instr.setName("db   ");
                instr.setOpcode(0x00);
            } else {
                int opcode = gameboy.getMemory().readByte(addr++);
                if (opcode == 0xCB) {
                    instr.copyMeta(gameboy.getCpu().cb_opcodes.get(gameboy.getMemory().readByte(addr++)));
                    instr.setAddr(addr - 2);
                } else {
                    instr.copyMeta(gameboy.getCpu().opcodes.get(opcode));
                    instr.setAddr(addr - 1);
                }
                if (instr.getLength() == 2)
                    instr.setParams(gameboy.getMemory().readByte(addr++));

                if (instr.getLength() == 3)
                    instr.setParams(gameboy.getMemory().readByte(addr++), gameboy.getMemory().readByte(addr++));
            }
        }
    }

    public void addBreakpoint(int addr, BreakPoint.Type type) {
        breakpoints.put(addr, new BreakPoint(addr, type));
    }

    public void removeBreakpoint(int addr) {
        breakpoints.remove(addr);
    }

    public void clock() {
        decompile();
        //EXEC breakpoints
        if (breakpoints.containsKey(cpuState.getInstruction().getAddr()) && breakpoints.get(cpuState.getInstruction().getAddr()).type() == BreakPoint.Type.EXEC) {
            gameboy.setState(GameBoyState.DEBUG);
            Logger.log(Logger.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (breakpoint reached (EXEC))");
        }

        if (cpuState.getInstruction().getType() == Instruction.Type.R || cpuState.getInstruction().getType() == Instruction.Type.RW) {
            int addr = cpuState.getInstruction().getParamAddress();
            if (breakpoints.containsKey(addr) && breakpoints.get(addr).type() == BreakPoint.Type.READ) {
                gameboy.setState(GameBoyState.DEBUG);
                Logger.log(Logger.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (read from " + String.format("%04X", addr) + ")");
            }
        }

        if (cpuState.getInstruction().getType() == Instruction.Type.W || cpuState.getInstruction().getType() == Instruction.Type.RW) {
            int addr = cpuState.getInstruction().getParamAddress();
            if (breakpoints.containsKey(addr) && breakpoints.get(addr).type() == BreakPoint.Type.WRITE) {
                gameboy.setState(GameBoyState.DEBUG);
                Logger.log(Logger.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (write to " + String.format("%04X", addr) + ")");
            }
        }
    }

    public void linkCpu(State cpuState) {
        this.cpuState = cpuState;
    }

    public State getCpuState() {
        return cpuState;
    }

    public Queue<Instruction> getInstructionQueue() {
        return instructionQueue;
    }

    public boolean isHooked() {
        return hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
    }
}


