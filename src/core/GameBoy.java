package core;

import core.apu.APU;
import core.cpu.LR35902;
import core.input.InputManager;
import core.input.State;
import core.input.Button;
import core.memory.MMU;
import core.ppu.PPU;
import debug.BreakPoint;
import debug.Debugger;
import debug.Logger;

public class GameBoy {

    public static final boolean ENABLE_BOOTSTRAP = true;
    private boolean hasCartridge = false;
    public Mode mode = Mode.DMG;
    private long mcycles = 0;

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private final InputManager inputManager;
    private final Debugger debugger;

    private GameBoyState currentState;

    public GameBoy() {
        memory = new MMU(this);
        cpu = new LR35902(this);
        ppu = new PPU(this);
        apu = new APU(this);
        timer = new Timer(this);
        inputManager = new InputManager(this);
        debugger = new Debugger(this);
        currentState = GameBoyState.RUNNING;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public void hookDebugger(boolean hook) {
        if (hook)
            cpu.hookDebugger(debugger);
        else
            cpu.hookDebugger(null);
        debugger.setHooked(hook);
    }

    public void insertCartridge(String file) throws Exception {
        memory.loadCart(file);
        hasCartridge = true;
        reset();
    }

    public void reset() {
        memory.reset();
        cpu.reset();
        ppu.reset();
        apu.reset();
        memory.writeRaw(MMU.P1, 0xCF);
        memory.writeRaw(MMU.SB, 0x00);
        memory.writeRaw(MMU.SC, 0x7E);
        memory.writeRaw(MMU.DIV, 0xAB);
        memory.writeRaw(MMU.TIMA, 0x00);
        memory.writeRaw(MMU.TMA, 0x00);
        memory.writeRaw(MMU.TAC, 0xF8);
        memory.writeRaw(MMU.IF, 0xE1);
        memory.writeRaw(MMU.NR10, 0x80);
        memory.writeRaw(MMU.NR11, 0xBF);
        memory.writeRaw(MMU.NR12, 0xF3);
        memory.writeRaw(MMU.NR13, 0xFF);
        memory.writeRaw(MMU.NR14, 0xBF);
        memory.writeRaw(MMU.NR21, 0x3F);
        memory.writeRaw(MMU.NR22, 0x00);
        memory.writeRaw(MMU.NR23, 0xFF);
        memory.writeRaw(MMU.NR24, 0xBF);
        memory.writeRaw(MMU.NR30, 0x7F);
        memory.writeRaw(MMU.NR31, 0xFF);
        memory.writeRaw(MMU.NR32, 0x9F);
        memory.writeRaw(MMU.NR33, 0xFF);
        memory.writeRaw(MMU.NR34, 0xBF);
        memory.writeRaw(MMU.NR41, 0xFF);
        memory.writeRaw(MMU.NR42, 0x00);
        memory.writeRaw(MMU.NR43, 0x00);
        memory.writeRaw(MMU.NR44, 0xBF);
        memory.writeRaw(MMU.NR50, 0x77);
        memory.writeRaw(MMU.NR51, 0xF3);
        memory.writeRaw(MMU.NR52, 0xF1);
        memory.writeRaw(MMU.LCDC, 0x91);
        memory.writeRaw(MMU.STAT, 0x85);
        memory.writeRaw(MMU.SCX, 0x00);
        memory.writeRaw(MMU.SCY, 0x00);
        memory.writeRaw(MMU.LY, 0x00);
        memory.writeRaw(MMU.LYC, 0x00);
        memory.writeRaw(MMU.DMA, 0xFF);
        memory.writeRaw(MMU.BGP, 0xFC);
        memory.writeRaw(MMU.OBP0, 0xFF);
        memory.writeRaw(MMU.OBP1, 0xFF);
        memory.writeRaw(MMU.WX, 0x00);
        memory.writeRaw(MMU.WY, 0x00);
        memory.writeRaw(MMU.IE, 0x00);
        Logger.log(Logger.Type.INFO, "Emulation reset");
        cpu.init();
    }

    public MMU getMemory() {
        return memory;
    }

    public LR35902 getCpu() {
        return cpu;
    }

    public PPU getPpu() {
        return ppu;
    }

    public APU getApu() {
        return apu;
    }

    public void executeInstruction() {
        int opcode_mcycles = 100000000;
        while (opcode_mcycles > 0) {
            if (memory.clock()) {
                if (mode == Mode.CGB) {
                    if (memory.clock()) {
                        cpu.execute();
                        timer.clock();
                    }
                }
                opcode_mcycles = cpu.execute();
                timer.clock();
                ppu.clock();
                apu.clock();
                inputManager.clock();
            }

            mcycles++;
            if (mcycles >= LR35902.CPU_CYCLES_PER_SEC * 10) {
                memory.saveCartridge();
                mcycles -= LR35902.CPU_CYCLES_PER_SEC * 10;
            }
        }
    }

    public void executeInstructions(int nb_instr, boolean force) {
        for (int i = 0; i < nb_instr && (currentState == GameBoyState.RUNNING || force); i++)
            executeInstruction();
    }

    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }

    public void executeFrame() {
        while(ppu.isFrameIncomplete() && currentState == GameBoyState.RUNNING)
            executeInstruction();
    }

    public void forceFrame() {
        while(ppu.isFrameIncomplete() && currentState == GameBoyState.DEBUG)
            executeInstruction();
    }

    public GameBoyState getState() {
        return currentState;
    }

    public void setState(GameBoyState state) {
        this.currentState = state;
    }

    public float getNextSample() {
        return apu.getNextSample();
    }

    public void addBreakpoint(int addr, BreakPoint.Type type) {
        debugger.addBreakpoint(addr, type);
    }

    public void removeBreakpoint(int addr) {
        debugger.removeBreakpoint(addr);
    }

    public void setButtonState(Button button, State state) {
        inputManager.setButtonState(button, state);
    }

    public boolean hasCartridge() {
        return hasCartridge;
    }

    public boolean isDebuggerHooked() {
        return debugger.isHooked();
    }

    public enum Mode {
        DMG,
        CGB
    }
}
