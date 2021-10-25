package debug;

import core.GameBoy;
import core.GameBoyState;
import core.apu.Sample;
import core.cpu.Instruction;
import core.cpu.State;
import core.memory.MMU;
import core.ppu.helper.IMMUListener;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Debugger implements IMMUListener {

    private static final int DECOMPILE_SIZE = 0x08;

    private final GameBoy gameboy;

    private boolean enabled = false;

    private core.cpu.State cpuState;
    private core.ppu.State ppuState;
    private Queue<Sample> sampleQueue;
    private final int[] memorySnapshot;

    private final Map<DebuggerMode, Boolean> hookedModes;

    private final Map<Integer, BreakPoint> breakpoints;
    private final Queue<Instruction> instructionQueue;


    public Debugger(GameBoy gb) {
        breakpoints = new HashMap<>();
        instructionQueue = new ConcurrentLinkedQueue<>();
        memorySnapshot = new int[0x10000];
        this.gameboy = gb;

        hookedModes = new HashMap<>();
        for (DebuggerMode mode : DebuggerMode.values())
            hookedModes.put(mode, false);
    }

    public void link(core.cpu.State state) {
        this.cpuState = state;
    }

    public void link(core.ppu.State state) {
        this.ppuState = state;
    }

    public void link(Queue<Sample> queue) {
        this.sampleQueue = queue;
    }

    public void link(MMU memory) {
        memory.addListener(this);
    }

    private void decompile() {
        int addr = cpuState.getPc().read();
        addr += cpuState.getInstruction().getLength();
        for (Instruction instr : instructionQueue) {
            if (addr >= 0x8000 && addr <= 0x9FFF || addr >= 0xFE00 && addr <= 0xFF7F || addr == 0xFFFF || addr >= 0x0104 && addr <= 0x014F) {
                instr.setAddr(addr);
                instr.setLength(0x10 - (addr & 0xF));
                for (int i = 0; i < instr.getLength(); i++)
                    instr.setParam(i, readMemorySnapshot(addr++));
                instr.setName("db   ");
                instr.setOpcode(0x00);
            } else {
                int opcode = readMemorySnapshot(addr++);
                if (opcode == 0xCB) {
                    instr.copyMeta(gameboy.getCpu().cb_opcodes.get(readMemorySnapshot(addr++)));
                    instr.setAddr(addr - 2);
                } else {
                    instr.copyMeta(gameboy.getCpu().opcodes.get(opcode));
                    instr.setAddr(addr - 1);
                }
                if (instr.getLength() == 2)
                    instr.setParams(readMemorySnapshot(addr++));

                if (instr.getLength() == 3)
                    instr.setParams(readMemorySnapshot(addr++), readMemorySnapshot(addr++));
            }
        }
    }

    public void addBreakpoint(int addr, BreakPoint.Type type) {
        breakpoints.put(addr, new BreakPoint(addr, type));
    }

    public void removeBreakpoint(int addr) {
        breakpoints.remove(addr);
    }

    public void breakpointCheck() {
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

    public void clock() {
        if (isHooked(DebuggerMode.CPU))
            decompile();
        if (isHooked(DebuggerMode.PPU)) {
            if (isHooked(DebuggerMode.OAMS))
                gameboy.getPpu().computeOAM();
            else if (isHooked(DebuggerMode.TILES))
                gameboy.getPpu().computeTileTables();
            else if (isHooked(DebuggerMode.TILEMAPS))
                gameboy.getPpu().computeTileMaps();
        }
    }

    public State getCpuState() {
        return cpuState;
    }

    public Queue<Instruction> getInstructionQueue() {
        return instructionQueue;
    }

    public boolean isHooked(DebuggerMode mode) {
        if (!enabled)
            return false;
        return hookedModes.get(mode);
    }

    public GameBoyState getGameboyState() {
        return gameboy.getState();
    }

    public int readMemorySnapshot(int addr) {
        return memorySnapshot[addr & 0xFFFF];
    }

    public void setHooked(DebuggerMode mode, boolean hooked) {
        hookedModes.put(mode, hooked);
    }

    @Override
    public void onWriteToMMU(int addr, int data) {
        memorySnapshot[addr] = gameboy.getMemory().readByte(addr, true);
    }

    public void setGameboyState(GameBoyState state) {
        gameboy.setState(state);
    }

    public Queue<Sample> getSampleQueue() {
        return sampleQueue;
    }

    public int readCGBPalette(boolean obj_pal, int addr) {
        return gameboy.getMemory().readCGBPalette(obj_pal, addr);
    }

    public String getSerialOutput() {
        return gameboy.getSerialOutput();
    }

    public void flushSerialOutput() {
        gameboy.flushSerialOutput();
    }

    public void reset() {
        gameboy.reset();
    }

    public void init() {
        instructionQueue.clear();
        for (int i = 0; i < DECOMPILE_SIZE; i++)
            instructionQueue.add(new Instruction(0, Instruction.Type.MISC,"NOP", 1, null, cpuState));
        for (int i = 0; i < memorySnapshot.length; i++)
            memorySnapshot[i] = gameboy.getMemory().readByte(i, true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized ByteBuffer[] getTileTableBuffers() {
        return ppuState.getTileTableBuffers();
    }

    public synchronized ByteBuffer[] getTileMapBuffers() {
        return ppuState.getTileMapBuffers();
    }

    public synchronized ByteBuffer getOAMBuffer() {
        return ppuState.getOAMBuffer();
    }
}


