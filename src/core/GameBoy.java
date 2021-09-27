package core;

import core.apu.APU;
import core.cpu.LR35902;
import core.input.InputManager;
import core.input.State;
import core.input.Button;
import core.memory.MMU;
import core.ppu.PPU;
import debug.Logger;

public class GameBoy {

    public static final boolean DEBUG = true;
    public static final boolean ENABLE_BOOTSTRAP = true;

    public static final int[] BOOTSTRAP = {
            0x31, 0xFE, 0xFF, 0xAF, 0x21, 0xFF, 0x9F, 0x32, 0xCB, 0x7C, 0x20, 0xFB, 0x21, 0x26, 0xFF, 0x0E,
            0x11, 0x3E, 0x80, 0x32, 0xE2, 0x0C, 0x3E, 0xF3, 0xE2, 0x32, 0x3E, 0x77, 0x77, 0x3E, 0xFC, 0xE0,
            0x47, 0x11, 0x04, 0x01, 0x21, 0x10, 0x80, 0x1A, 0xCD, 0x95, 0x00, 0xCD, 0x96, 0x00, 0x13, 0x7B,
            0xFE, 0x34, 0x20, 0xF3, 0x11, 0xD8, 0x00, 0x06, 0x08, 0x1A, 0x13, 0x22, 0x23, 0x05, 0x20, 0xF9,
            0x3E, 0x19, 0xEA, 0x10, 0x99, 0x21, 0x2F, 0x99, 0x0E, 0x0C, 0x3D, 0x28, 0x08, 0x32, 0x0D, 0x20,
            0xF9, 0x2E, 0x0F, 0x18, 0xF3, 0x67, 0x3E, 0x64, 0x57, 0xE0, 0x42, 0x3E, 0x91, 0xE0, 0x40, 0x04,
            0x1E, 0x02, 0x0E, 0x0C, 0xF0, 0x44, 0xFE, 0x90, 0x20, 0xFA, 0x0D, 0x20, 0xF7, 0x1D, 0x20, 0xF2,
            0x0E, 0x13, 0x24, 0x7C, 0x1E, 0x83, 0xFE, 0x62, 0x28, 0x06, 0x1E, 0xC1, 0xFE, 0x64, 0x20, 0x06,
            0x7B, 0xE2, 0x0C, 0x3E, 0x87, 0xE2, 0xF0, 0x42, 0x90, 0xE0, 0x42, 0x15, 0x20, 0xD2, 0x05, 0x20,
            0x4F, 0x16, 0x20, 0x18, 0xCB, 0x4F, 0x06, 0x04, 0xC5, 0xCB, 0x11, 0x17, 0xC1, 0xCB, 0x11, 0x17,
            0x05, 0x20, 0xF5, 0x22, 0x23, 0x22, 0x23, 0xC9, 0xCE, 0xED, 0x66, 0x66, 0xCC, 0x0D, 0x00, 0x0B,
            0x03, 0x73, 0x00, 0x83, 0x00, 0x0C, 0x00, 0x0D, 0x00, 0x08, 0x11, 0x1F, 0x88, 0x89, 0x00, 0x0E,
            0xDC, 0xCC, 0x6E, 0xE6, 0xDD, 0xDD, 0xD9, 0x99, 0xBB, 0xBB, 0x67, 0x63, 0x6E, 0x0E, 0xEC, 0xCC,
            0xDD, 0xDC, 0x99, 0x9F, 0xBB, 0xB9, 0x33, 0x3E, 0x3C, 0x42, 0xB9, 0xA5, 0xB9, 0xA5, 0x42, 0x3C,
            0x21, 0x04, 0x01, 0x11, 0xA8, 0x00, 0x1A, 0x13, 0xBE, 0x20, 0xFE, 0x23, 0x7D, 0xFE, 0x34, 0x20,
            0xF5, 0x06, 0x19, 0x78, 0x86, 0x23, 0x05, 0x20, 0xFB, 0x86, 0x20, 0xFE, 0x3E, 0x01, 0xE0, 0x50
    };

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private final InputManager inputManager;

    private GameBoyState currentState = GameBoyState.PAUSED;

    public GameBoy() {
        memory = new MMU(this);
        cpu = new LR35902(memory, this);
        ppu = new PPU(memory);
        apu = new APU(memory);
        timer = new Timer(memory);
        inputManager = new InputManager(memory);
        if (!GameBoy.DEBUG)
            currentState = GameBoyState.RUNNING;
        reset();
    }

    public void insertCartridge(String file) throws Exception {
        memory.loadCart(file);
        cpu.init();
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
        memory.writeRaw(MMU.LCDC, 0x11);
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
        int mcycles = cpu.execute();
        memory.clock(mcycles);
        ppu.clock(mcycles);
        apu.clock(mcycles);
        timer.clock(mcycles);
        inputManager.clock();
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
        while(!ppu.isFrameComplete() && currentState == GameBoyState.RUNNING)
            executeInstruction();
    }

    public void forceFrame() {
        while(!ppu.isFrameComplete() && currentState == GameBoyState.DEBUG)
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

    public void addBreakpoint(int addr) {
        cpu.addBreakpoint(addr);
    }

    public void removeBreakpoint(int addr) {
        cpu.removeBreakpoint(addr);
    }

    public void addMemoryBreakpoint(int addr) {
        memory.addBreakpoint(addr);
    }

    public void setButtonState(Button button, State state) {
        inputManager.setButtonState(button, state);
    }
}
